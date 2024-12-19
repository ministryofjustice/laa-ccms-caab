package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.util.FileUtil;
import uk.gov.laa.ccms.data.model.NotificationSummary;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.soa.gateway.model.CoverSheet;
import uk.gov.laa.ccms.soa.gateway.model.Document;

/**
 * Service class to handle Notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final EbsApiClient ebsApiClient;
  private final SoaApiClient soaApiClient;
  private final CaabApiClient caabApiClient;
  private final S3ApiClient s3ApiClient;

  private final NotificationAttachmentMapper notificationAttachmentMapper;

  private static final String COVER_SHEET_FILE_EXTENSION = "pdf";

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId  The login identifier for the user.
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(String loginId) {
    return ebsApiClient.getUserNotificationSummary(loginId);
  }

  /**
   * Search and retrieve notifications based on search criteria.
   *
   * @param searchCriteria The criteria on which to search.
   * @param page           The page number for pagination.
   * @param size           The size or number of records per page.
   * @return A Mono wrapping the notifications list based on search criteria.
   */
  public Mono<Notifications> getNotifications(NotificationSearchCriteria searchCriteria,
      final Integer page, final Integer size) {
    return ebsApiClient.getNotifications(searchCriteria, page, size);
  }

  /**
   * If the document with the provided ID does not exist in S3, fetch it from EBS and upload it.
   *
   * @param attachmentId The ID of the notification attachment to retrieve.
   * @param loginId      The login identifier for the user.
   * @param userType     Type of the user (e.g., admin, user).
   */
  public void retrieveNotificationAttachment(String attachmentId,
      String loginId, String userType) {

    retrieveDocument(attachmentId, loginId, userType, false, false);
  }

  /**
   * If the document with the provided ID does not exist in S3, fetch it from TDS and upload it.
   *
   * @param attachmentId The ID of the notification attachment to retrieve.
   * @param loginId      The login identifier for the user.
   * @param userType     Type of the user (e.g., admin, user).
   */
  public void retrieveDraftNotificationAttachment(String attachmentId,
      String loginId, String userType) {

    retrieveDocument(attachmentId, loginId, userType, true, false);
  }

  /**
   * If the cover sheet with the provided ID does not exist in S3, fetch it from EBS and upload it.
   *
   * @param attachmentId The ID of the notification attachment to retrieve a cover sheet for.
   * @param loginId      The login identifier for the user.
   * @param userType     Type of the user (e.g., admin, user).
   */
  public void retrieveCoverSheet(String attachmentId,
      String loginId, String userType) {

    retrieveDocument(attachmentId, loginId, userType, false, true);
  }

  /**
   * If the cover sheet with the provided ID does not exist in S3, fetch it from the appropriate
   * data store and upload it.
   *
   * @param attachmentId      The ID of the notification attachment to retrieve a cover sheet for.
   * @param loginId           The login identifier for the user.
   * @param userType          Type of the user (e.g., admin, user).
   * @param isDraftDocument   Whether the document is a draft document.
   * @param isCoverSheet      Whether the document is a cover sheet.
   */
  private void retrieveDocument(String attachmentId,
      String loginId, String userType, boolean isDraftDocument, boolean isCoverSheet) {

    final boolean existsInS3;

    if (isDraftDocument) {
      existsInS3 = s3ApiClient.getDraftDocumentUrl(attachmentId).isPresent();
    } else {
      existsInS3 = s3ApiClient.getDocumentUrl(attachmentId).isPresent();
    }

    if (existsInS3) {
      log.debug("Document with ID '{}' found in S3.",
          attachmentId);
    } else {
      log.debug("Document with ID '{}' missing in S3. Attempting to retrieve "
              + "from database instead.",
          attachmentId);

      if (isDraftDocument) {
        NotificationAttachmentDetail draftAttachment =
            caabApiClient.getNotificationAttachment(Integer.parseInt(attachmentId)).block();
        if (draftAttachment != null) {
          uploadToS3(draftAttachment, attachmentId);
        }
      } else if (isCoverSheet) {
        CoverSheet coverSheet = soaApiClient.downloadCoverSheet(attachmentId,
            loginId, userType).block();
        if (coverSheet != null) {
          uploadToS3(coverSheet, attachmentId);
        }
      } else {
        Document attachment = soaApiClient.downloadDocument(attachmentId,
            loginId, userType).block();
        if (attachment != null) {
          uploadToS3(attachment, attachmentId);
        }
      }

      log.debug("Document with ID '{}' uploaded to S3.",
          attachmentId);
    }
  }

  private void uploadToS3(NotificationAttachmentDetail draftAttachment, String attachmentId) {
    s3ApiClient.uploadDraftDocument(attachmentId, draftAttachment.getFileData(),
        FileUtil.getFileExtension(draftAttachment.getFileName()));

  }

  private void uploadToS3(Document attachment, String attachmentId) {
    s3ApiClient.uploadDocument(attachmentId, attachment.getFileData(),
        attachment.getFileExtension());
  }

  private void uploadToS3(CoverSheet coverSheet, String attachmentId) {
    s3ApiClient.uploadDocument(attachmentId, coverSheet.getFileData(),
        COVER_SHEET_FILE_EXTENSION);
  }

  /**
   * Store a notification attachment in the TDS and S3, prior to submission to EBS.
   *
   * @param notificationAttachment - the notification attachment detail.
   * @param loginId                - The login identifier for the user.
   */
  public void addDraftNotificationAttachment(
      NotificationAttachmentDetail notificationAttachment,
      String loginId) {
    String attachmentId = caabApiClient.createNotificationAttachment(notificationAttachment,
        loginId).block();
    if (notificationAttachment.getSendBy().equals(ELECTRONIC.getCode())) {
      String extension = FileUtil.getFileExtension(notificationAttachment.getFileName());
      s3ApiClient.uploadDraftDocument(attachmentId, notificationAttachment.getFileData(),
          extension);
    }
  }

  /**
   * Store a notification attachment in the TDS and S3, prior to submission to EBS.
   *
   * @param notificationAttachment - the notification attachment detail.
   * @param loginId                - The login identifier for the user.
   */
  public void updateDraftNotificationAttachment(
      NotificationAttachmentDetail notificationAttachment,
      String loginId) {
    caabApiClient.updateNotificationAttachment(notificationAttachment,
        loginId).block();
    if (notificationAttachment.getSendBy().equals(ELECTRONIC.getCode())) {
      String extension = FileUtil.getFileExtension(notificationAttachment.getFileName());
      s3ApiClient.uploadDraftDocument(String.valueOf(notificationAttachment.getId()),
          notificationAttachment.getFileData(),
          extension);
    }
  }

  /**
   * Submit drafted notification attachments to EBS, and clear down draft document stores.
   *
   * @param notificationId The ID of the notification to submit attachments for.
   * @param loginId        The login identifier for the user.
   * @param userType       Type of the user (e.g., admin, user).
   */
  public void submitNotificationAttachments(String notificationId, String loginId,
      String userType, Integer providerId) {

    // Get all notification attachments from TDS

    Set<String> notificationAttachmentIds = getNotificationAttachmentIds(notificationId,
        providerId);

    List<NotificationAttachmentDetail> notificationAttachmentDetails =
        Flux.fromStream(notificationAttachmentIds.stream()
                .map(Integer::parseInt)
                .map(caabApiClient::getNotificationAttachment)).flatMap(mono -> mono).collectList()
            .block();

    if (notificationAttachmentDetails != null && !notificationAttachmentDetails.isEmpty()) {

      // Upload documents to EBS

      Flux.fromStream(notificationAttachmentDetails.stream()
              .map(notificationAttachmentMapper::toDocument)
              .map(document -> soaApiClient.uploadDocument(document, notificationId,
                  null, loginId,
                  userType))).flatMap(mono -> mono).collectList()
          .block();

      // Delete draft documents from TDS and S3
      removeDraftNotificationAttachments(notificationId, providerId, notificationAttachmentIds,
          loginId);
    }
  }

  /**
   * Get a single NotificationAttachmentDetail from TDS by its id.
   *
   * @param notificationAttachmentId - the id of the notification attachment.
   * @return NotificationAttachmentDetail for the supplied id.
   */
  public Mono<NotificationAttachmentDetail> getDraftNotificationAttachment(
      final Integer notificationAttachmentId) {
    return caabApiClient.getNotificationAttachment(notificationAttachmentId);
  }

  /**
   * Get a List of uploaded notification attachments by notification id.
   *
   * @param notificationId - the notification id.
   * @return NotificationAttachmentDetails containing the list of NotificationAttachmentDetail.
   */
  public Mono<NotificationAttachmentDetails> getDraftNotificationAttachments(
      final String notificationId, final Integer providerId) {
    return caabApiClient.getNotificationAttachments(
        notificationId,
        providerId,
        null,
        null);
  }

  /**
   * Remove a notification attachment from the TDS and S3.
   *
   * @param notificationId - the id of the notification attachment to remove.
   * @param loginId        - the user removing a notification attachment.
   */
  public void removeDraftNotificationAttachment(
      final String notificationId,
      final Integer notificationAttachmentId,
      final String loginId,
      final Integer providerId) {

    // First ensure that the notification attachment exists and is related to the notification.
    // (We don't want to retrieve the notification attachment by its id, as that will include the
    // (possibly) 8MB of file data.)
    final BaseNotificationAttachmentDetail notificationAttachment =
        getDraftNotificationAttachments(
            notificationId, providerId)
            .map(NotificationAttachmentDetails::getContent)
            .mapNotNull(baseNotificationAttachmentDetails ->
                baseNotificationAttachmentDetails.stream()
                    .filter(baseNotificationAttachmentDetail -> baseNotificationAttachmentDetail
                        .getId().equals(notificationAttachmentId))
                    .findFirst()
                    .orElse(null))
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Invalid notification attachment id: %s", notificationAttachmentId)));

    caabApiClient.deleteNotificationAttachment(notificationAttachmentId, loginId).block();
    if (notificationAttachment.getSendBy().equals(ELECTRONIC.getCode())) {
      final String fileExtension = FileUtil.getFileExtension(notificationAttachment.getFileName());
      s3ApiClient.removeDraftDocument(FileUtil.getFilename(String.valueOf(notificationAttachmentId),
          fileExtension));
    }
  }

  /**
   * Remove all draft notification attachments for the given notification.
   *
   * @param notificationId the ID of the notification.
   * @param loginId        the user login ID.
   */
  public void removeDraftNotificationAttachments(String notificationId, String loginId,
      Integer providerId) {
    // Get all notification attachment IDs linked to the given notification
    Set<String> notificationAttachmentIds = getNotificationAttachmentIds(notificationId,
        providerId);

    removeDraftNotificationAttachments(notificationId, providerId, notificationAttachmentIds,
        loginId);
  }

  /**
   * Remove all draft notification attachments for the given notification. The caller must ensure
   * the provided attachment IDs are linked to the notification.
   *
   * @param notificationId            the ID of the notification.
   * @param notificationAttachmentIds the IDs of the notification attachments.
   * @param loginId                   the user login ID.
   */
  private void removeDraftNotificationAttachments(String notificationId,
      Integer providerId, Set<String> notificationAttachmentIds, String loginId) {

    if (!CollectionUtils.isEmpty(notificationAttachmentIds)) {
      caabApiClient.deleteNotificationAttachments(notificationId, providerId, null,
              null, loginId)
          .block();
      s3ApiClient.removeDraftDocuments(notificationAttachmentIds);
    }
  }

  /**
   * For each {@link BaseNotificationAttachmentDetail} provided, generate a signed URL to access the
   * file in S3.
   *
   * @param attachments The list of draft documents for which to generate access URLs.
   * @return a map of document ID / URL pairs.
   */
  public Map<String, String> getDraftDocumentLinks(
      List<BaseNotificationAttachmentDetail> attachments) {
    List<String> documentIds = attachments.stream()
        .map(BaseNotificationAttachmentDetail::getId)
        .map(String::valueOf)
        .toList();
    return getDocumentLinks(documentIds, true);
  }

  /**
   * For each {@link Document} provided, generate a signed URL to access the file in S3.
   *
   * @param documents The list of uploaded documents for which to generate access URLs.
   * @return a map of document ID / URL pairs.
   */
  public Map<String, String> getDocumentLinks(List<Document> documents) {
    List<String> documentIds = documents.stream()
        .map(Document::getDocumentId)
        .toList();
    return getDocumentLinks(documentIds, false);
  }

  /**
   * For each {@link Document} provided, generate a signed URL to access the file in S3.
   *
   * @param documentIds The IDs of the documents for which to generate access URLs.
   * @return a map of document ID / URL pairs.
   */
  private Map<String, String> getDocumentLinks(List<String> documentIds, boolean isDraft) {
    Map<String, String> documentLinks = new HashMap<>();
    if (!documentIds.isEmpty()) {
      for (String documentId : documentIds) {
        String documentUrl;
        if (isDraft) {
          documentUrl = s3ApiClient.getDraftDocumentUrl(documentId).orElse(null);
        } else {
          documentUrl = s3ApiClient.getDocumentUrl(documentId).orElse(null);
        }
        documentLinks.put(documentId, documentUrl);
      }
    }
    return documentLinks;
  }

  /**
   * Get all IDs of notification attachments linked to the given notification.
   *
   * @param notificationId the ID of the notification.
   * @return Set of notification attachment IDs linked to the notification.
   */
  private Set<String> getNotificationAttachmentIds(String notificationId, Integer providerId) {
    return getDraftNotificationAttachments(notificationId, providerId)
        .map(NotificationAttachmentDetails::getContent)
        .mapNotNull(baseNotificationAttachmentDetails ->
            baseNotificationAttachmentDetails.stream()
                .map(BaseNotificationAttachmentDetail::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet()))
        .block();
  }


}
