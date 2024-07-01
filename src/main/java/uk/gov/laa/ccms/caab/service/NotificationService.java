package uk.gov.laa.ccms.caab.service;

import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
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
  private final S3Template s3Template;

  @Value("${laa.ccms.s3.buckets.document-bucket}")
  String bucketName;

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
   * Retrieve the content for all documents attached to a notification. First attempt retrieval from
   * S3, and if the document is not found attempt retrieval from EBS, then upload to S3 for future
   * retrieval.
   *
   * @param notificationAttachmentId  The notification to get attachments for.
   * @param loginId                   The login identifier for the user.
   * @param userType                  Type of the user (e.g., admin, user).
   * @return An Optional String containing the document content.
   */
  public Optional<String> getNotificationAttachment(String notificationAttachmentId,
      String loginId, String userType) {

    String content = null;
    try {
      content = getNotificationAttachmentFromS3(notificationAttachmentId);
      if (!StringUtils.hasText(content)) {
        log.warn("Notification attachment with ID '{}' retrieved from S3 has no content.",
            notificationAttachmentId);
      }
    } catch (NoSuchKeyException ex) {
      log.warn("Notification attachment with ID '{}' missing in S3. Attempting to retrieve "
              + "from EBS instead.",
          notificationAttachmentId);
      content = getNotificationAttachementFromSoa(notificationAttachmentId, loginId, userType);
      if (!StringUtils.hasText(content)) {
        log.warn("Notification attachment with ID '{}' retrieved from EBS has no content.",
            notificationAttachmentId);
      }
      uploadNotificationAttachmentToS3(notificationAttachmentId, content);
    } catch (IOException ex) {
      log.warn("Unable to process content for notification attachment with ID '{}'.",
          notificationAttachmentId);
    }

    return Optional.ofNullable(content);
  }

  /**
   * Retrieve the content of a notification attachment from S3.
   *
   * @param documentId The document identifier for the notification attachment.
   * @return a String containing the content of the notification attachment.
   * @throws IOException when the file content cannot be processed.
   */
  private String getNotificationAttachmentFromS3(String documentId) throws IOException {
    return s3Template.download(bucketName, documentId).getContentAsString(StandardCharsets.UTF_8);
  }

  /**
   * Retrieve the content of a notification attachment from EBS.
   *
   * @param documentId The document identifier for the notification attachment.
   * @param loginId    The login identifier for the user.
   * @param userType   Type of the user (e.g., admin, user).
   * @return a String containing the content of the notification attachment.
   */
  private String getNotificationAttachementFromSoa(String documentId, String loginId,
      String userType) {
    return soaApiClient.downloadDocument(documentId, loginId, userType)
        .block()
        .getFileData();
  }

  /**
   * Upload a notification attachment to S3.
   *
   * @param documentId The document identifier for the notification attachment.
   * @param content    The content of the notification attachment.
   */
  private void uploadNotificationAttachmentToS3(String documentId, String content) {
    InputStream contentInputStream = new ByteArrayInputStream(
        content.getBytes(StandardCharsets.UTF_8));
    s3Template.upload(bucketName, documentId, contentInputStream);
  }


}
