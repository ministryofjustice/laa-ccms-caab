package uk.gov.laa.ccms.caab.client;

import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Client to handle interactions with AWS S3 buckets.
 */
@Service
@RequiredArgsConstructor
public class S3ApiClient {

  private final S3Template s3Template;
  private final S3ApiClientErrorHandler errorHandler;

  @Value("${laa.ccms.s3.buckets.document-bucket}")
  final String documentBucketName;

  /**
   * Retrieve the content of a document from S3.
   *
   * @param documentId The document identifier.
   * @return an Optional String containing the content of the document.
   */
  public Optional<String> downloadDocument(String documentId) {
    String content = null;
    try {
      content = s3Template.download(documentBucketName, documentId)
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (NoSuchKeyException e) {
      errorHandler.handleFileNotFoundError(e);
    } catch (IOException e) {
      errorHandler.handleS3ApiError(e);
    }
    return Optional.ofNullable(content);
  }

  /**
   * Upload a document to S3.
   *
   * @param documentId The document identifier for the document.
   * @param content    The content of the document.
   */
  public void uploadDocument(String documentId, String content) {
    InputStream contentInputStream = new ByteArrayInputStream(
        content.getBytes(StandardCharsets.UTF_8));
    s3Template.upload(documentBucketName, documentId, contentInputStream);
  }

}
