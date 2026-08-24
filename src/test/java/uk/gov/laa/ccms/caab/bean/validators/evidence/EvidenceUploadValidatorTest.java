package uk.gov.laa.ccms.caab.bean.validators.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.constants.CcmsModule;

@ExtendWith(MockitoExtension.class)
class EvidenceUploadValidatorTest {

  private final EvidenceUploadValidator validator =
      new EvidenceUploadValidator(
          List.of("pdf", "doc"),
          "20B",
          List.of("application/pdf", "application/msword", "text/plain"));

  private EvidenceUploadFormData evidenceUploadFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    evidenceUploadFormData = new EvidenceUploadFormData();
    errors = new BeanPropertyBindingResult(evidenceUploadFormData, EVIDENCE_UPLOAD_FORM_DATA);
  }

  @Test
  public void supports_ReturnsTrueForCorrectClass() {
    assertTrue(validator.supports(EvidenceUploadFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_noErrors() {
    evidenceUploadFormData = buildEvidenceUploadFormData();

    validator.validate(evidenceUploadFormData, errors);
    assertFalse(errors.hasErrors());
    assertEquals("originalName.pdf", evidenceUploadFormData.getSanitisedFileName());
  }

  @Test
  void validate_sanitizesFilename() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "My interesting%filename!.pdf",
            "application/pdf",
            "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertFalse(errors.hasErrors());
    assertEquals("My_interesting_filename_.pdf", evidenceUploadFormData.getSanitisedFileName());
    assertEquals("pdf", evidenceUploadFormData.getFileExtension());
  }

  @Test
  void validate_collapsesRepeatedSpacesInFilename() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "Test             Upload--  -- copyDoublespaces.rtf",
            "text/plain",
            "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
    assertEquals(
        "Test_Upload--_--_copyDoublespaces.rtf", evidenceUploadFormData.getSanitisedFileName());
    assertEquals("rtf", evidenceUploadFormData.getFileExtension());
  }

  @Test
  void validate_sanitizesSpecialAndInternationalCharactersInFilename() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "TestUpload™‹#. €@£ {}__[';]_' copy.rtf",
            "text/plain",
            "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
    assertEquals("TestUploadTM______copy.rtf", evidenceUploadFormData.getSanitisedFileName());
    assertEquals("rtf", evidenceUploadFormData.getFileExtension());
  }

  @Test
  public void validate_fileMandatory() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(null);

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
  }

  @Test
  public void validate_fileExtension() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.ppp", "contentType", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
  }

  @Test
  @DisplayName("validate - Filename without dot maps to invalid extension error")
  void validate_FileWithoutDot_HasInvalidExtensionError() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile", "originalName", "application/pdf", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
  }

  @Test
  public void validate_mimeType() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.pdf", "contentType", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals(
        "validation.error.invalidMimeType",
        Objects.requireNonNull(errors.getFieldError("file")).getCode());
  }

  @Test
  public void validate_fileSize() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "originalName.pdf",
            "application/pdf",
            "file content which is over twenty bytes in length".getBytes()));

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.maxFileSize", errors.getFieldError("file").getCode());
  }

  @Test
  @DisplayName("validate - Adds error for invalid magic bytes")
  void validate_InvalidMagicBytes_HasErrors() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[3]));

    validator.validate(evidenceUploadFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidMagicBytes", errors.getFieldError("file").getCode());
  }

  @Test
  public void validate_documentTypeMandatory() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setDocumentType(null);

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("documentType"));
  }

  @Test
  public void validate_evidenceTypeMandatory() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setEvidenceTypes(Collections.emptyList());

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("evidenceTypes"));
  }

  @Test
  public void validate_descriptionMaxLength() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setDocumentDescription("a".repeat(256));

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("documentDescription"));
  }

  @Test
  @DisplayName("validate - Rejects double extension")
  void validate_DoubleExtension_HasErrors() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "file", "document.pdf.exe", "application/pdf", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.invalidExtension", errors.getFieldError("file").getCode());
  }

  @Test
  @DisplayName("validate - Sanitises and accepts filename with null-byte escape")
  void validate_NullByteEscapeInFilename_Sanitized() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "file", "malicious%00.pdf", "application/pdf", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertFalse(errors.hasErrors());
    assertEquals("malicious_00.pdf", evidenceUploadFormData.getSanitisedFileName());
  }

  @Test
  @DisplayName("validate - Rejects filename exceeding 255 characters")
  void validate_FilenameExceeds255Chars_HasErrors() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    final String longName = "a".repeat(252) + ".pdf";
    evidenceUploadFormData.setFile(
        new MockMultipartFile("file", longName, "application/pdf", "the file data".getBytes()));

    validator.validate(evidenceUploadFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.filenameTooLong", errors.getFieldError("file").getCode());
  }

  private EvidenceUploadFormData buildEvidenceUploadFormData() {
    EvidenceUploadFormData formData = new EvidenceUploadFormData();
    formData.setApplicationOrOutcomeId("123");
    formData.setCaseReferenceNumber("caseRef");
    formData.setCcmsModule(CcmsModule.APPLICATION);
    formData.setDocumentDescription("doc desc");
    formData.setDocumentSender("doc sender");
    formData.setDocumentType("docType");
    formData.setDocumentTypeDisplayValue("doc type");
    formData.setEvidenceTypes(List.of("type 1", "type 2"));
    formData.setFile(
        new MockMultipartFile(
            "theFile", "originalName.pdf", "application/pdf", "the file data".getBytes()));
    formData.setProviderId(789);
    formData.setRegisteredDocumentId("regId");
    return formData;
  }
}
