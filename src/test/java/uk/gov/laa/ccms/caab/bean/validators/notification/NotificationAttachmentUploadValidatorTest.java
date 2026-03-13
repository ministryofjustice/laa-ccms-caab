package uk.gov.laa.ccms.caab.bean.validators.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.constants.SendBy;

@ExtendWith(MockitoExtension.class)
public class NotificationAttachmentUploadValidatorTest {

  private final NotificationAttachmentUploadValidator validator =
      new NotificationAttachmentUploadValidator(
          List.of("pdf", "doc"),
          "20B",
          List.of(
              "application/pdf",
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
              "text/plain"));

  private NotificationAttachmentUploadFormData notificationAttachmentUploadFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    notificationAttachmentUploadFormData = new NotificationAttachmentUploadFormData();
    errors =
        new BeanPropertyBindingResult(
            notificationAttachmentUploadFormData, "attachmentUploadFormData");
  }

  @Test
  public void supports_ReturnsTrueForCorrectClass() {
    assertTrue(validator.supports(NotificationAttachmentUploadFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_electronic_noErrors() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setSendBy(SendBy.ELECTRONIC);

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_postal_noErrors() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setSendBy(SendBy.POSTAL);

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_fileMandatory() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(null);

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
  }

  @Test
  public void validate_fileExtension() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.ppp", "contentType", "the file data".getBytes()));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
  }

  @Test
  public void validate_mimeType() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.pdf", "contentType", "the file data".getBytes()));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidMimeType", errors.getFieldError("file").getCode());
  }

  @Test
  public void validate_fileSize() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "originalName.pdf",
            "application/pdf",
            "file content which is over twenty bytes in length".getBytes()));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.maxFileSize", errors.getFieldError("file").getCode());
  }

  @Test
  public void validate_fileName() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile(
            "invalid name.pdf", "invalid name.pdf", "application/pdf", "the file data".getBytes()));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidFileName", errors.getFieldError("file").getCode());
  }

  @Test
  @DisplayName("validate - Adds error for invalid magic bytes")
  void validate_InvalidMagicBytes_HasErrors() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[3000000]));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
  }

  @Test
  public void validate_documentTypeMandatory() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setDocumentType(null);

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("documentType"));
  }

  @Test
  public void validate_descriptionMaxLength() {
    notificationAttachmentUploadFormData = buildNotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setDocumentDescription("a".repeat(256));

    validator.validate(notificationAttachmentUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("documentDescription"));
  }

  private NotificationAttachmentUploadFormData buildNotificationAttachmentUploadFormData() {
    NotificationAttachmentUploadFormData formData = new NotificationAttachmentUploadFormData();
    formData.setDocumentDescription("doc desc");
    formData.setDocumentType("docType");
    formData.setDocumentTypeDisplayValue("doc type");
    formData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.pdf", "application/pdf", "the file data".getBytes()));
    formData.setProviderId(789);
    formData.setDocumentId(123);
    formData.setSendBy(SendBy.ELECTRONIC);
    formData.setNumber(1L);
    formData.setNotificationReference("123");
    return formData;
  }
}
