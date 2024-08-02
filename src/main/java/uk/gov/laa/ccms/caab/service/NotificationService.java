package uk.gov.laa.ccms.caab.service;

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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.util.FileUtil;
import uk.gov.laa.ccms.soa.gateway.model.BaseDocument;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

/**
 * Service class to handle Notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final SoaApiClient soaApiClient;
  private final CaabApiClient caabApiClient;
  private final S3ApiClient s3ApiClient;

  private final NotificationAttachmentMapper notificationAttachmentMapper;

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId  The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType) {
    return soaApiClient.getNotificationsSummary(loginId, userType);
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
    return soaApiClient.getNotifications(searchCriteria, page, size);
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

    if (s3ApiClient.getDocumentUrl(attachmentId).isPresent()) {
      log.debug("Document with ID '{}' found in S3.",
          attachmentId);
    } else {
      log.debug("Document with ID '{}' missing in S3. Attempting to retrieve "
              + "from EBS instead.",
          attachmentId);
      Document attachment = soaApiClient.downloadDocument(attachmentId,
          loginId, userType).block();
      if (attachment != null) {
        log.debug("Document with ID '{}' retrieved from EBS. Uploading to S3.",
            attachmentId);
        s3ApiClient.uploadDocument(attachmentId, attachment.getFileData(),
            attachment.getFileExtension());
        log.debug("Document with ID '{}' uploaded to S3.",
            attachmentId);
      } else {
        throw new CaabApplicationException(String.format("Unable to retrieve document with ID "
            + "'%s' from EBS.", attachmentId));
      }
    }
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
    String extension = FileUtil.getFileExtension(notificationAttachment.getFileName());
    s3ApiClient.uploadDraftDocument(attachmentId, notificationAttachment.getFileData(),
        extension);
  }

  /**
   * Submit drafted notification attachments to EBS, and clear down draft document stores.
   *
   * @param notificationId The ID of the notification to submit attachments for.
   * @param loginId        The login identifier for the user.
   * @param userType       Type of the user (e.g., admin, user).
   */
  public void submitNotificationAttachments(String notificationId, String loginId,
      String userType) {

    // Get all notification attachments from TDS

    Set<String> notificationAttachmentIds = getNotificationAttachmentIds(notificationId);

    List<NotificationAttachmentDetail> notificationAttachmentDetails =
        Flux.fromStream(notificationAttachmentIds.stream()
                .map(Integer::parseInt)
                .map(caabApiClient::getNotificationAttachment)).flatMap(mono -> mono).collectList()
            .block();

    // Register documents in EBS

    Set<Tuple2<NotificationAttachmentDetail, String>> registeredAttachments =
        notificationAttachmentDetails.stream()
            .map(notificationAttachment -> {
              BaseDocument baseDocument =
                  notificationAttachmentMapper.toBaseDocument(notificationAttachment);
              return Tuples.of(notificationAttachment,
                  soaApiClient.registerDocument(baseDocument, loginId, userType).block()
                      .getReferenceNumber());
            }).collect(Collectors.toSet());

    // Upload document contents to EBS

    Flux.fromStream(registeredAttachments.stream()
        .map(registeredAttachment -> {
          Document document = notificationAttachmentMapper.toDocument(registeredAttachment.getT1());
          document.setDocumentId(registeredAttachment.getT2());
          return soaApiClient.uploadDocument(document, loginId, userType);
        })).flatMap(mono -> mono).collectList().block();

    // Delete draft documents from TDS and S3

    removeDraftNotificationAttachments(notificationId, notificationAttachmentIds, loginId);
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
      final String notificationId) {
    return caabApiClient.getNotificationAttachments(
        notificationId,
        null,
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
      final String loginId) {

    // First ensure that the notification attachment exists and is related to the notification.
    // (We don't want to retrieve the notification attachment by its id, as that will include the
    // (possibly) 8MB of file data.)
    getDraftNotificationAttachments(
        notificationId)
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
    s3ApiClient.removeDraftDocument(String.valueOf(notificationAttachmentId));
  }

  /**
   * Remove all draft notification attachments for the given notification.
   *
   * @param notificationId the ID of the notification.
   * @param loginId        the user login ID.
   */
  public void removeDraftNotificationAttachments(String notificationId, String loginId) {
    // Get all notification attachment IDs linked to the given notification
    Set<String> notificationAttachmentIds = getNotificationAttachmentIds(notificationId);

    removeDraftNotificationAttachments(notificationId, notificationAttachmentIds, loginId);
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
      Set<String> notificationAttachmentIds, String loginId) {

    if (!CollectionUtils.isEmpty(notificationAttachmentIds)) {
      caabApiClient.deleteNotificationAttachments(notificationId, null, null, null, loginId)
          .block();
      s3ApiClient.removeDraftDocuments(notificationAttachmentIds);
    }
  }

  /**
   * Get all IDs of notification attachments linked to the given notification.
   *
   * @param notificationId the ID of the notification.
   * @return Set of notification attachment IDs linked to the notification.
   */
  private Set<String> getNotificationAttachmentIds(String notificationId) {
    return getDraftNotificationAttachments(notificationId)
        .map(NotificationAttachmentDetails::getContent)
        .mapNotNull(baseNotificationAttachmentDetails ->
            baseNotificationAttachmentDetails.stream()
                .map(BaseNotificationAttachmentDetail::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet()))
        .block();
  }

  /**
   * For each {@link Document} provided, generate a signed URL to access the file in S3.
   *
   * @param documents The documents for which to generate access URLs.
   * @return a map of document ID / URL pairs.
   */
  public Map<String, String> getDocumentLinks(List<Document> documents) {
    Map<String, String> documentLinks = new HashMap<>();
    if (!documents.isEmpty()) {
      for (Document document : documents) {
        documentLinks.put(document.getDocumentId(),
            s3ApiClient.getDocumentUrl(document.getDocumentId()).orElse(null));
      }
    }
    return documentLinks;
  }

}
