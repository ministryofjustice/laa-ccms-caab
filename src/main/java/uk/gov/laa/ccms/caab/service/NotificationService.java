package uk.gov.laa.ccms.caab.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.S3ApiClientException;
import uk.gov.laa.ccms.caab.client.S3ApiFileNotFoundException;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
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
   * Retrieve the content for a notification attachment. First attempt retrieval from
   * S3, and if the document is not found attempt retrieval from EBS, then upload to S3 for future
   * retrieval.
   *
   * @param notificationAttachmentId  The identifier of the notification attachment.
   * @param loginId                   The login identifier for the user.
   * @param userType                  Type of the user (e.g., admin, user).
   * @return An Optional String containing the document content.
   */
  public Optional<String> getNotificationAttachment(String notificationAttachmentId,
      String loginId, String userType) {

    Optional<String> content = Optional.empty();
    try {
      content = s3ApiClient.downloadDocument(notificationAttachmentId);
      content.ifPresent(doc -> {
        if (!StringUtils.hasText(doc)) {
          log.warn("Notification attachment with ID '{}' retrieved from S3 has no content.",
              notificationAttachmentId);
        }
      });
    } catch (S3ApiFileNotFoundException ex) {
      log.warn("Notification attachment with ID '{}' missing in S3. Attempting to retrieve "
              + "from EBS instead.",
          notificationAttachmentId);
      content = getNotificationAttachementFromSoa(notificationAttachmentId, loginId, userType);
      content.ifPresent(doc -> {
        if (!StringUtils.hasText(doc)) {
          log.warn("Notification attachment with ID '{}' retrieved from EBS has no content.",
              notificationAttachmentId);
        }
        s3ApiClient.uploadDocument(notificationAttachmentId, doc);
      });

    } catch (S3ApiClientException ex) {
      log.warn("Unable to process content for notification attachment with ID '{}'.",
          notificationAttachmentId);
    }

    return content;
  }

  /**
   * Retrieve the content of a notification attachment from EBS.
   *
   * @param documentId The document identifier for the notification attachment.
   * @param loginId    The login identifier for the user.
   * @param userType   Type of the user (e.g., admin, user).
   * @return an Optional String containing the content of the notification attachment if available.
   */
  private Optional<String> getNotificationAttachementFromSoa(String documentId, String loginId,
      String userType) {
    Document document = soaApiClient.downloadDocument(documentId, loginId, userType)
        .block();

    if (document != null) {
      return Optional.ofNullable(document.getFileData());
    } else {
      return Optional.empty();
    }
  }

}
