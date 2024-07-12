package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
public class S3ApiClientTest {

  @Mock
  private S3Template s3Template;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private S3ApiClientErrorHandler s3ApiClientErrorHandler;

  @InjectMocks
  S3ApiClient s3ApiClient;

  private final String documentId = "documentId";
  private final String documentContent = "documentContent";

  @Test
  void downloadDocument_successful_returnsDocumentContent() throws IOException {
    S3Resource s3Resource = mock(S3Resource.class);
    when(s3Resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(documentContent);

    when(s3Template.download(any(), eq(documentId))).thenReturn(s3Resource);

    Optional<String> actual = s3ApiClient.downloadDocument(documentId);

    assertTrue(actual.isPresent());
    assertEquals(documentContent, actual.get());
  }

  @Test
  void downloadDocument_keyNotFound_wrapsSpecificException() {

    when(s3Template.download(any(), eq(documentId))).thenThrow(NoSuchKeyException.class);

    assertThrows(S3ApiFileNotFoundException.class, () -> s3ApiClient.downloadDocument(documentId),
        "Expected S3ApiFileNotFoundException to be thrown, but wasn't.");

  }

  @Test
  void downloadDocument_ioError_wrapsException() throws IOException {

    S3Resource s3Resource = mock(S3Resource.class);
    when(s3Resource.getContentAsString(any())).thenThrow(IOException.class);

    when(s3Template.download(any(), eq(documentId))).thenReturn(s3Resource);

    assertThrows(S3ApiClientException.class, () -> s3ApiClient.downloadDocument(documentId),
        "Expected S3ApiClientException to be thrown, but wasn't.");

  }

  @Test
  void uploadDocument_successful_callsS3() {

    s3ApiClient.uploadDocument(documentId, documentContent);

    verify(s3Template).upload(any(), eq(documentId), any());

  }

}
