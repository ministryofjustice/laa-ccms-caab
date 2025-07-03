package uk.gov.laa.ccms.caab.bean.validators.evidence;

import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;

/** Validate the evidence document details provided by evidence upload flow. */
@Component
@Getter
public class EvidenceUploadValidator extends FileUploadValidator {

  public EvidenceUploadValidator(
      @Value("${laa.ccms.caab.upload.valid-extensions}") List<String> validExtensions,
      @Value("${spring.servlet.multipart.max-file-size}") String maxFileSize) {
    super(validExtensions, maxFileSize);
  }

  /** The error message for the required evidenceTypes field. */
  protected static final String EVIDENCE_TYPES_REQUIRED_ERROR =
      "Please select at least one evidence type";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link EvidenceUploadFormData}, {@code
   *     false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return EvidenceUploadFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the evidence document details in the {@link EvidenceUploadFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final EvidenceUploadFormData evidenceUploadFormData = (EvidenceUploadFormData) target;

    validateFile(evidenceUploadFormData, errors);
    validateDocumentType(evidenceUploadFormData, errors);
    validateDocumentDescription(evidenceUploadFormData, errors);

    if (evidenceUploadFormData.getEvidenceTypes() == null
        || evidenceUploadFormData.getEvidenceTypes().isEmpty()) {
      errors.rejectValue("evidenceTypes", "required.evidenceTypes", EVIDENCE_TYPES_REQUIRED_ERROR);
    }
  }
}
