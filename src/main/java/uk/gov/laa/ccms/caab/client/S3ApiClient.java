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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import uk.gov.laa.ccms.caab.config.S3DocumentBucketProperties;

/**
 * Client to handle interactions with AWS S3 buckets.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({S3DocumentBucketProperties.class})
public class S3ApiClient {

  // For Simple Interactions
  private final S3Template s3Template;

  // For Complex Interactions
  private final S3Client s3Client;

  private final S3ApiClientErrorHandler errorHandler;
  private final S3DocumentBucketProperties documentBucketProperties;

  private final String draftPrefix = "draft/";

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
    } catch (SdkException | IOException e) {
      errorHandler.handleS3ApiError(e);
    }
    return Optional.ofNullable(content);
  }

  public void removeDraftDocuments(Set<String> documentIds) {
    removeDocuments(documentIds.stream()
        .map(this::getDraftId)
        .collect(Collectors.toSet()));
  }

  public void removeDocuments(Set<String> documentIds) {
    DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest
        .builder()
        .bucket(documentBucketProperties.getName())
        .delete(
            Delete.builder().objects(
              documentIds.stream()
                  .map(ObjectIdentifier.builder()::key)
                  .map(SdkBuilder::build)
                  .toList())
                .build()
        ).build();
    try {
      s3Client.deleteObjects(deleteObjectsRequest);
    } catch (SdkException e) {
      errorHandler.handleS3ApiError(e);
    }
  }

  public void removeDraftDocument(String documentId) {
    removeDocument(getDraftId(documentId));
  }

  public void removeDocument(String documentId) {
    try {
      s3Template.deleteObject(documentBucketProperties.getName(), documentId);
    } catch (NoSuchKeyException e) {
      errorHandler.handleFileNotFoundError(e);
    } catch (SdkException e) {
      errorHandler.handleS3ApiError(e);
    }
  }

  public Optional<String> getDraftDocumentUrl(String documentId) {
    return getDocumentUrl(getDraftId(documentId));
  }

  /**
   * Generate a signed S3 URL for a document.
   *
   * @param documentId The document identifier.
   * @return an Optional String containing the signed S3 URL of the document.
   */
  public Optional<String> getDocumentUrl(String documentId) {
    return s3Template.listObjects(documentBucketProperties.getName(), documentId).stream()
        .findFirst()
        .map(S3Resource::getFilename)
        .map(filename -> s3Template
            .createSignedGetURL(documentBucketProperties.getName(), filename,
                Duration.ofMinutes(documentBucketProperties.getUrlDuration())))
        .map(URL::toString);
  }

  public void uploadDraftDocument(String documentId, String fileData, String extension) {
    uploadDocument(documentId, fileData, extension,true);
  }

  public void uploadDocument(String documentId, String fileData, String extension) {
    uploadDocument(documentId, fileData, extension, false);
  }

  /**
   * Upload a document to S3.
   *
   * @param documentId The id of the document to upload.
   * @param fileData The content of the document to upload.
   * @param extension The extension of the document to upload.
   */
  public void uploadDocument(String documentId, String fileData, String extension,
      boolean isDraft) {
    InputStream contentInputStream = new ByteArrayInputStream(
        Base64.getDecoder().decode(fileData));
    String filename = getFilename(documentId, extension);
    if (isDraft) {
      filename = getDraftId(filename);
    }
    s3Template.upload(documentBucketProperties.getName(), filename, contentInputStream);
  }

  private String getFilename(String name, String extension) {
    return name + ((extension == null || extension.equals(name)) ? "" : "." + extension);
  }

  private String getDraftId(String id) {
    return draftPrefix + id;
  }

}
