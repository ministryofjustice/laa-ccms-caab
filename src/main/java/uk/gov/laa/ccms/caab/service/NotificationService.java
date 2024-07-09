package uk.gov.laa.ccms.caab.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
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

  private final S3ApiClient s3ApiClient;

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
   * @param attachmentId  The ID of the notification attachment to retrieve.
   * @param loginId       The login identifier for the user.
   * @param userType      Type of the user (e.g., admin, user).
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
        s3ApiClient.uploadDocument(attachmentId, attachment.getFileData());
        log.debug("Document with ID '{}' uploaded to S3.",
            attachmentId);
      } else {
        throw new CaabApplicationException(String.format("Unable to retrieve document with ID "
                + "'%s' from EBS.", attachmentId));
      }
    }
  }

  /**
   * For each {@link Document} provided, generate a signed URL to access the file in S3.
   *
   * @param documents   The documents for which to generate access URLs.
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
