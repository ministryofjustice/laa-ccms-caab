package uk.gov.laa.ccms.caab.bean.validators.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;

import java.util.Arrays;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;

@ExtendWith(SpringExtension.class)
class ProviderRequestDocumentUploadValidatorTest {

  private ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

  private EvidenceUploadFormData evidenceUploadFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    providerRequestDocumentUploadValidator = new ProviderRequestDocumentUploadValidator(
        Arrays.asList("pdf", "jpg", "png"),
        "5MB"
    );
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
    final MockMultipartFile oversizedFile = new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[6000000]);
    evidenceUploadFormData.setFile(oversizedFile);
    evidenceUploadFormData.setFileExtension("pdf");

    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals(String.format(FileUploadValidator.MAX_FILESIZE_ERROR, "5MB"),
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

}
