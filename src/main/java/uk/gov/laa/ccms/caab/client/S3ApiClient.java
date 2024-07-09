package uk.gov.laa.ccms.caab.client;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.laa.ccms.caab.config.S3DocumentBucketProperties;

/**
 * Client to handle interactions with AWS S3 buckets.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({S3DocumentBucketProperties.class})
public class S3ApiClient {

  private final S3Template s3Template;
  private final S3ApiClientErrorHandler errorHandler;
  private final S3DocumentBucketProperties documentBucketProperties;


  /**
   * Retrieve the content of a document from S3.
   *
   * @param documentId The document identifier.
   * @return an Optional String containing the content of the document.
   */
  public Optional<String> downloadDocument(String documentId) {
    String content = null;
    try {
      content = s3Template.download(documentBucketProperties.getName(), documentId)
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (NoSuchKeyException e) {
      errorHandler.handleFileNotFoundError(e);
    } catch (IOException e) {
      errorHandler.handleS3ApiError(e);
    }
    return Optional.ofNullable(content);
  }

  /**
   * Generate a signed S3 URL for a document.
   *
   * @param documentId The document identifier.
   * @return an Optional String containing the signed S3 URL of the document.
   */
  public Optional<String> getDocumentUrl(String documentId) {
    return s3Template.listObjects(documentBucketProperties.getName(), documentId + ".").stream()
        .findFirst()
        .map(S3Resource::getFilename)
        .map(filename -> s3Template
            .createSignedGetURL(documentBucketProperties.getName(), filename,
                Duration.ofMinutes(documentBucketProperties.getUrlDuration())))
        .map(URL::toString);
  }

  /**
   * Upload a document to S3.
   *
   * @param documentId The document identifier for the document.
   * @param content    Base64 encoded String containing the content of the document.
   */
  public void uploadDocument(String documentId, String content) {
    InputStream contentInputStream = new ByteArrayInputStream(
        Base64.getDecoder().decode(content));
    s3Template.upload(documentBucketProperties.getName(), documentId, contentInputStream);
  }

}
