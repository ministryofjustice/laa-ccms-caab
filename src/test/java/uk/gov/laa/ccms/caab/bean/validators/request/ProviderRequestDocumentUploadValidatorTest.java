package uk.gov.laa.ccms.caab.bean.validators.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;

@ExtendWith(MockitoExtension.class)
class ProviderRequestDocumentUploadValidatorTest {

  private ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

  private EvidenceUploadFormData evidenceUploadFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    providerRequestDocumentUploadValidator =
        new ProviderRequestDocumentUploadValidator(
            Arrays.asList("pdf", "jpg", "png"),
            "5MB",
            Arrays.asList("application/pdf", "image/jpg", "image/png"));
    evidenceUploadFormData = new EvidenceUploadFormData();
    errors = new BeanPropertyBindingResult(evidenceUploadFormData, "evidenceUploadFormData");
  }

  @Test
  @DisplayName("supports - Returns true for EvidenceUploadFormData class")
  public void supports_ReturnsTrueForEvidenceUploadFormDataClass() {
    assertTrue(providerRequestDocumentUploadValidator.supports(EvidenceUploadFormData.class));
  }

  @Test
  @DisplayName("supports - Returns false for other classes")
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(providerRequestDocumentUploadValidator.supports(Object.class));
  }

  @Test
  @DisplayName("validate - Adds error when file size exceeds limit")
  public void validate_FileSizeExceedsLimit_HasErrors() {
    final MockMultipartFile oversizedFile =
        new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[6000000]);
    evidenceUploadFormData.setFile(oversizedFile);
    evidenceUploadFormData.setFileExtension("pdf");

    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals(
        FileUploadValidator.MAX_FILESIZE_ERROR.formatted("5MB"),
        errors.getFieldError("file").getDefaultMessage());
  }

  @Test
  @DisplayName("validate - Adds error when document description exceeds max length")
  public void validate_DescriptionExceedsMaxLength_HasErrors() {
    final String longDescription = "A".repeat(300); // Exceeds max length of 255
    evidenceUploadFormData.setDocumentDescription(longDescription);

    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("documentDescription"));
  }

  @Test
  @DisplayName("validate - Adds error when filename is invalid")
  public void validate_InvalidFilename_HasErrors() {
    final MockMultipartFile invalidNamedFile =
        new MockMultipartFile(
            "invalid name.pdf", "valid.pdf", "application/pdf", new byte[6000000]);
    evidenceUploadFormData.setFile(invalidNamedFile);

    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
  }

  @Test
  @DisplayName("validate - Adds error for invalid magic bytes")
  void validate_InvalidMagicBytes_HasErrors() {
    final MockMultipartFile invalidFile =
        new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[3000000]);
    evidenceUploadFormData.setFile(invalidFile);

    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
  }
}
