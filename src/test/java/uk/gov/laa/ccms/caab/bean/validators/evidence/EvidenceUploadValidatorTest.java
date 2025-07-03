package uk.gov.laa.ccms.caab.bean.validators.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.constants.CcmsModule;

@ExtendWith(SpringExtension.class)
class EvidenceUploadValidatorTest {

  private final EvidenceUploadValidator validator =
      new EvidenceUploadValidator(List.of("pdf", "doc"), "20B");

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
  public void validate_fileSize() {
    evidenceUploadFormData = buildEvidenceUploadFormData();
    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "theFile",
            "originalName.pdf",
            "contentType",
            "file content which is over twenty bytes in length".getBytes()));

    validator.validate(evidenceUploadFormData, errors);
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("file"));
    assertEquals("validation.error.maxFileSize", errors.getFieldError("file").getCode());
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
            "theFile", "originalName.pdf", "contentType", "the file data".getBytes()));
    formData.setProviderId(789);
    formData.setRegisteredDocumentId("regId");
    return formData;
  }
}
