package uk.gov.laa.ccms.caab.service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
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
  private final S3Template s3Template;

  @Value("${laa.ccms.s3.buckets.document-bucket.url-duration}")
  private final Long urlDuration;

  @Value("${laa.ccms.s3.buckets.document-bucket.name}")
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
   * Retrieve the signed S3 URL of a document.
   *
   * @param documentId The document identifier.
   * @return an Optional String containing the signed S3 URL of the notification attachment.
   */
  public Optional<String> getDocumentUrl(String documentId) {
    return s3Template.listObjects(bucketName, documentId + ".").stream()
        .findFirst()
        .map(S3Resource::getFilename)
        .map(filename -> s3Template
            .createSignedGetURL(bucketName, filename, Duration.ofMinutes(urlDuration)))
        .map(URL::toString);
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

    if (getDocumentUrl(attachmentId).isPresent()) {
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
        uploadDocumentToS3(attachment);
        log.debug("Document with ID '{}' uploaded to S3.",
            attachmentId);
      } else {
        throw new CaabApplicationException(String.format("Unable to retrieve document with ID "
                + "'%s' from EBS.", attachmentId));
      }
    }
  }

  /**
   * Upload a document to S3.
   *
   * @param document   The document to upload.
   */
  private void uploadDocumentToS3(Document document) {
    InputStream contentInputStream = new ByteArrayInputStream(
        Base64.getDecoder().decode(document.getFileData()));
    String filename = document.getDocumentId() + '.' + document.getFileExtension();
    s3Template.upload(bucketName, filename, contentInputStream);
  }

}
