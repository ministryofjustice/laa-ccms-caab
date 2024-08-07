package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;

@SpringBootTest
public class S3ClientIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private S3ApiClient s3ApiClient;

  @Autowired
  private S3Template s3Template;

  @Value("${laa.ccms.s3.buckets.document-bucket.name}")
  private String bucketName;


  @AfterEach
  void cleanup() {
    List<S3Resource> testFiles = s3Template.listObjects(bucketName, "integration-test-file");
    List<S3Resource> testDraftFiles = s3Template.listObjects(bucketName, "draft/integration-test-file");
    testFiles.forEach(testFile -> s3Template.deleteObject(bucketName, testFile.getFilename()));
    testDraftFiles.forEach(testFile -> s3Template.deleteObject(bucketName, testFile.getFilename()));
  }

  @Test
  public void testDownloadDocument_success() {
    String documentName = "integration-test-file-1";
    s3Template.upload(bucketName, documentName, new ByteArrayInputStream("content".getBytes()));

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "integration-test"
        + "-file");
    assertEquals(1, beforeFiles.size());

    Optional<String> document = s3ApiClient.downloadDocument(documentName);

    assertTrue(document.isPresent());
    assertEquals("content", document.get());
  }

  @Test
  public void testRemoveDocuments_success() {
    String documentName = "integration-test-file-1";
    String documentName2 = "integration-test-file-2";
    String documentName3 = "integration-test-file-3";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());
    s3Template.upload(bucketName, documentName2, InputStream.nullInputStream());
    s3Template.upload(bucketName, documentName3, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "integration-test-file");
    assertEquals(3, beforeFiles.size());

    s3ApiClient.removeDocuments(Set.of(documentName, documentName2));

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "integration-test-file");
    assertEquals(1, afterFiles.size());
    assertNotNull(afterFiles.getFirst());
    assertEquals(afterFiles.getFirst().getFilename(), documentName3);
  }

  @Test
  public void testRemoveDraftDocuments_success() {
    String documentName = "draft/integration-test-file-1";
    String documentName2 = "draft/integration-test-file-2";
    String documentName3 = "draft/integration-test-file-3";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());
    s3Template.upload(bucketName, documentName2, InputStream.nullInputStream());
    s3Template.upload(bucketName, documentName3, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "draft/integration-test"
        + "-file");
    assertEquals(3, beforeFiles.size());

    s3ApiClient.removeDraftDocuments(Set.of("integration-test-file-1", "integration-test-file-2"));

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "draft/integration-test-file");
    assertEquals(1, afterFiles.size());
    assertNotNull(afterFiles.getFirst());
    assertEquals(afterFiles.getFirst().getFilename(), documentName3);
  }

  @Test
  public void testRemoveDocument_success() {
    String documentName = "integration-test-file-1";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "integration-test"
        + "-file");
    assertEquals(1, beforeFiles.size());

    s3ApiClient.removeDocument("integration-test-file-1");

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "integration-test-file");
    assertTrue(afterFiles.isEmpty());
  }

  @Test
  public void testRemoveDraftDocument_success() {
    String documentName = "draft/integration-test-file-1";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "draft/integration-test"
        + "-file");
    assertEquals(1, beforeFiles.size());

    s3ApiClient.removeDraftDocument("integration-test-file-1");

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "draft/integration-test-file");
    assertTrue(afterFiles.isEmpty());
  }

  @Test
  public void testGetDocumentUrl_success() {
    String documentName = "integration-test-file-1";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "integration-test"
        + "-file");
    assertEquals(1, beforeFiles.size());

    Optional<String> documentUrl = s3ApiClient.getDocumentUrl("integration-test-file-1");

    assertTrue(documentUrl.isPresent());
    assertTrue(documentUrl.get().contains("X-Amz-Algorithm"));
    assertTrue(documentUrl.get().contains("X-Amz-Date"));
    assertTrue(documentUrl.get().contains("X-Amz-SignedHeaders"));
    assertTrue(documentUrl.get().contains("X-Amz-Credential"));
    assertTrue(documentUrl.get().contains("X-Amz-Expires"));
  }

  @Test
  public void testGetDraftDocumentUrl_success() {
    String documentName = "draft/integration-test-file-1";
    s3Template.upload(bucketName, documentName, InputStream.nullInputStream());

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "draft/integration-test"
        + "-file");
    assertEquals(1, beforeFiles.size());

    Optional<String> documentUrl = s3ApiClient.getDraftDocumentUrl("integration-test-file-1");

    assertTrue(documentUrl.isPresent());
    assertTrue(documentUrl.get().contains("X-Amz-Algorithm"));
    assertTrue(documentUrl.get().contains("X-Amz-Date"));
    assertTrue(documentUrl.get().contains("X-Amz-SignedHeaders"));
    assertTrue(documentUrl.get().contains("X-Amz-Credential"));
    assertTrue(documentUrl.get().contains("X-Amz-Expires"));
  }

  @Test
  public void testUploadDocument_success() throws IOException {
    String documentName = "integration-test-file-1";
    String documentContent = "ZG9jdW1lbnRDb250ZW50"; // "documentContent" Base64 encoded
    String documentExtension = "pdf";

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "integration-test-file");
    assertTrue(beforeFiles.isEmpty());

    s3ApiClient.uploadDocument(documentName, documentContent, documentExtension);

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "integration-test-file");
    assertEquals(1, afterFiles.size());

    assertEquals("integration-test-file-1.pdf", afterFiles.getFirst().getFilename());
    assertEquals(documentContent,
        Base64.getEncoder().encodeToString(afterFiles.getFirst().getContentAsByteArray()));
  }

  @Test
  public void testUploadDraftDocument_success() throws IOException {
    String documentName = "integration-test-file-1";
    String documentContent = "ZG9jdW1lbnRDb250ZW50"; // "documentContent" Base64 encoded
    String documentExtension = "pdf";

    List<S3Resource> beforeFiles = s3Template.listObjects(bucketName, "draft/integration-test"
        + "-file");
    assertTrue(beforeFiles.isEmpty());

    s3ApiClient.uploadDraftDocument(documentName, documentContent, documentExtension);

    List<S3Resource> afterFiles = s3Template.listObjects(bucketName, "draft/integration-test-file");
    assertEquals(1, afterFiles.size());

    assertEquals("draft/integration-test-file-1.pdf", afterFiles.getFirst().getFilename());
    assertEquals(documentContent,
        Base64.getEncoder().encodeToString(afterFiles.getFirst().getContentAsByteArray()));
  }

}
