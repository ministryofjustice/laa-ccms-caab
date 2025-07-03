package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import uk.gov.laa.ccms.caab.config.S3DocumentBucketProperties;

@ExtendWith(MockitoExtension.class)
public class S3ApiClientTest {

  @Mock private S3Template s3Template;

  @Mock private S3Client s3Client;

  @Mock private S3DocumentBucketProperties s3DocumentBucketProperties;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private S3ApiClientErrorHandler s3ApiClientErrorHandler;

  @InjectMocks S3ApiClient s3ApiClient;

  private static final String DOCUMENT_ID = "documentId";
  private static final String DOCUMENT_CONTENT = "documentContent";
  private static final String DRAFT_PREFIX = "draft/";

  @Test
  void downloadDocument_successful_returnsDocumentContent() throws IOException {
    S3Resource s3Resource = mock(S3Resource.class);
    when(s3Resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(DOCUMENT_CONTENT);

    when(s3Template.download(any(), eq(DOCUMENT_ID))).thenReturn(s3Resource);

    Optional<String> actual = s3ApiClient.downloadDocument(DOCUMENT_ID);

    assertTrue(actual.isPresent());
    assertEquals(DOCUMENT_CONTENT, actual.get());
  }

  @Test
  void downloadDocument_keyNotFound_wrapsSpecificException() {

    when(s3Template.download(any(), eq(DOCUMENT_ID))).thenThrow(NoSuchKeyException.class);

    assertThrows(
        S3ApiFileNotFoundException.class,
        () -> s3ApiClient.downloadDocument(DOCUMENT_ID),
        "Expected S3ApiFileNotFoundException to be thrown, but wasn't.");
  }

  @Test
  void downloadDocument_ioError_wrapsException() throws IOException {

    S3Resource s3Resource = mock(S3Resource.class);
    when(s3Resource.getContentAsString(any())).thenThrow(IOException.class);

    when(s3Template.download(any(), eq(DOCUMENT_ID))).thenReturn(s3Resource);

    assertThrows(
        S3ApiClientException.class,
        () -> s3ApiClient.downloadDocument(DOCUMENT_ID),
        "Expected S3ApiClientException to be thrown, but wasn't.");
  }

  @Test
  void getDocumentUrl_successful_callsS3() throws MalformedURLException {

    S3Resource resource = mock(S3Resource.class);
    String filename = DOCUMENT_ID + ".txt";

    when(resource.getFilename()).thenReturn(filename);

    URL url = mock(URL.class);

    when(url.toString()).thenReturn("test-url");

    when(s3Template.listObjects(any(), eq(DOCUMENT_ID))).thenReturn(List.of(resource));
    when(s3Template.createSignedGetURL(any(), eq(filename), any())).thenReturn(url);

    String actual = s3ApiClient.getDocumentUrl(DOCUMENT_ID).get();

    assertNotNull(actual);
    assertEquals("test-url", actual);
    verify(s3Template).listObjects(any(), eq(DOCUMENT_ID));
    verify(s3Template).createSignedGetURL(any(), eq(filename), any());
  }

  @Test
  void uploadDocument_successful_callsS3() {

    s3ApiClient.uploadDocument(DOCUMENT_ID, DOCUMENT_CONTENT, "xls");

    verify(s3Template).upload(any(), eq(DOCUMENT_ID + ".xls"), any());
  }

  @Test
  void removeDraftDocuments_success() {
    Set<String> documentIds = Set.of("1", "2");

    final ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor =
        ArgumentCaptor.forClass(DeleteObjectsRequest.class);

    when(s3Client.deleteObjects(deleteRequestCaptor.capture()))
        .thenReturn(DeleteObjectsResponse.builder().build());

    s3ApiClient.removeDraftDocuments(documentIds);

    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

    final DeleteObjectsRequest deleteRequest = deleteRequestCaptor.getValue();

    Set<String> deletedKeys =
        deleteRequest.delete().objects().stream()
            .map(ObjectIdentifier::key)
            .collect(Collectors.toSet());

    assertEquals(2, deleteRequest.delete().objects().size());
    assertEquals(Set.of(DRAFT_PREFIX + "1", DRAFT_PREFIX + "2"), deletedKeys);
  }

  @Test
  void removeDraftDocuments_wrapsException() {
    Set<String> documentIds = Set.of("1", "2");

    when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenThrow(SdkException.class);

    assertThrows(S3ApiClientException.class, () -> s3ApiClient.removeDraftDocuments(documentIds));
  }

  @Test
  void removeDocuments() {
    Set<String> documentIds = Set.of("1", "2");

    final ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor =
        ArgumentCaptor.forClass(DeleteObjectsRequest.class);

    when(s3Client.deleteObjects(deleteRequestCaptor.capture()))
        .thenReturn(DeleteObjectsResponse.builder().build());

    s3ApiClient.removeDocuments(documentIds);

    verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

    final DeleteObjectsRequest deleteRequest = deleteRequestCaptor.getValue();

    Set<String> deletedKeys =
        deleteRequest.delete().objects().stream()
            .map(ObjectIdentifier::key)
            .collect(Collectors.toSet());

    assertEquals(2, deleteRequest.delete().objects().size());
    assertEquals(Set.of("1", "2"), deletedKeys);
  }

  @Test
  void removeDraftDocument() {
    s3ApiClient.removeDraftDocument("1");

    verify(s3Template).deleteObject(any(), eq(DRAFT_PREFIX + "1"));
  }

  @Test
  void removeDraftDocument_wrapsFileNotFound() {
    doThrow(NoSuchKeyException.class).when(s3Template).deleteObject(any(), any());

    assertThrows(S3ApiFileNotFoundException.class, () -> s3ApiClient.removeDraftDocument("1"));
  }

  @Test
  void removeDraftDocument_wrapsException() {
    doThrow(SdkException.class).when(s3Template).deleteObject(any(), any());

    assertThrows(S3ApiClientException.class, () -> s3ApiClient.removeDraftDocument("1"));
  }

  @Test
  void removeDocument() {
    s3ApiClient.removeDocument("1");

    verify(s3Template).deleteObject(any(), eq("1"));
  }

  @Test
  void getDraftDocumentUrl() {
    S3Resource s3Resource = mock(S3Resource.class);
    when(s3Template.listObjects(any(), eq(DRAFT_PREFIX + "1"))).thenReturn(List.of(s3Resource));
    when(s3Resource.getFilename()).thenReturn(DRAFT_PREFIX + "1.pdf");

    URL url = mock(URL.class);
    when(url.toString()).thenReturn("test-url");
    when(s3Template.createSignedGetURL(any(), eq(DRAFT_PREFIX + "1.pdf"), any())).thenReturn(url);

    String actual = s3ApiClient.getDraftDocumentUrl("1").get();

    assertNotNull(actual);
    assertEquals("test-url", actual);

    verify(s3Template).listObjects(any(), eq(DRAFT_PREFIX + "1"));
    verify(s3Template).createSignedGetURL(any(), eq(DRAFT_PREFIX + "1.pdf"), any());
  }

  @Test
  void uploadDraftDocument() {
    s3ApiClient.uploadDraftDocument("1", "fileData", "pdf");

    verify(s3Template).upload(any(), eq(DRAFT_PREFIX + "1.pdf"), any());
  }
}
