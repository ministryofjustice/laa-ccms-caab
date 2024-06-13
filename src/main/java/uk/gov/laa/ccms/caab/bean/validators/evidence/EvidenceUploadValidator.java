package uk.gov.laa.ccms.caab.bean.validators.evidence;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Validate the evidence document details provided by evidence upload flow.
 */
@Component
@RequiredArgsConstructor
public class EvidenceUploadValidator extends AbstractValidator {

  /**
   * The error message for the required file field.
   */
  protected static final String FILE_REQUIRED_ERROR = "Please select a file to upload";

  /**
   * The error message for the required evidenceTypes field.
   */
  protected static final String EVIDENCE_TYPES_REQUIRED_ERROR =
      "Please select at least one evidence type";

  /**
   * The error message for an invalid file extension.
   */
  protected static final String INVALID_EXTENSION_ERROR =
      "Invalid file extension.  We can only accept %s files.";

  /**
   * The error message for an invalid file extension.
   */
  public static final String MAX_FILESIZE_ERROR =
      "File is too large. The file must be less than %s";

  /**
   * The maximum length of the document description text area.
   */
  protected static final Integer DOCUMENT_DESCRIPTION_MAX_LENGTH = 255;

  /**
   * The configurable list of valid file extensions.
   */
  @Value("${upload.valid-extensions}")
  private final List<String> validExtensions;

  @Value("${spring.servlet.multipart.max-file-size}")
  private final String maxFileSize;

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link EvidenceUploadFormData},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return EvidenceUploadFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the evidence document details in the
   * {@link EvidenceUploadFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final EvidenceUploadFormData evidenceUploadFormData =
        (EvidenceUploadFormData) target;

    if (evidenceUploadFormData.getFile() == null
        || evidenceUploadFormData.getFile().isEmpty()) {
      errors.rejectValue("file", "required.file", FILE_REQUIRED_ERROR);
    } else {
      evidenceUploadFormData.setFileExtension(getFileExtension(evidenceUploadFormData.getFile()));

      // Check the file extension and file length.
      validateFile(evidenceUploadFormData, errors);
    }

    validateRequiredField("documentType", evidenceUploadFormData.getDocumentType(),
        "Document type", errors);

    if (evidenceUploadFormData.getEvidenceTypes() == null
        || evidenceUploadFormData.getEvidenceTypes().isEmpty()) {
      errors.rejectValue("evidenceTypes",
          "required.evidenceTypes", EVIDENCE_TYPES_REQUIRED_ERROR);
    }

    validateFieldMaxLength("documentDescription",
        evidenceUploadFormData.getDocumentDescription(),
        DOCUMENT_DESCRIPTION_MAX_LENGTH, "description", errors);
  }

  private void validateFile(EvidenceUploadFormData formData, Errors errors) {
    if (!isValidExtension(formData.getFileExtension())) {
      errors.rejectValue("file", "invalid.extension",
          String.format(INVALID_EXTENSION_ERROR, getExtensionDisplayString()));
    } else {
      try {
        // Check the file size is within limits
        final int fileSize = formData.getFile().getBytes().length;
        final long maxSize = DataSize.parse(maxFileSize).toBytes();

        if (fileSize > maxSize) {
          rejectFileSize(errors);
        }
      } catch (IOException ioe) {
        throw new CaabApplicationException("Failed to read file data", ioe);
      }
    }
  }

  public void rejectFileSize(Errors errors) {
    errors.rejectValue("file", "max.filesize.exceeded",
        String.format(MAX_FILESIZE_ERROR, maxFileSize));
  }

  protected boolean isValidExtension(final String fileExtension) {
    return validExtensions.stream().anyMatch(ext -> ext.toUpperCase().equals(fileExtension));
  }

  protected String getFileExtension(MultipartFile file) {
    return Optional.ofNullable(file.getOriginalFilename())
        .map(s -> s.substring(s.lastIndexOf(".") + 1).toUpperCase())
        .orElse("");
  }

  protected String getExtensionDisplayString() {
    return validExtensions.stream().collect(Collectors.collectingAndThen(Collectors.toList(),
        EvidenceUploadValidator::joiningLastDelimiter));
  }

  /**
   * Method to handle concatenating a list of Strings into a comma-separated string,
   * with an alternative delimiter of ' or ' before the final entry.
   *
   * @param list - the list of strings.
   * @return Delimited String.
   */
  protected static String joiningLastDelimiter(List<String> list) {
    final String delimiter = ", ";
    final String lastDelimiter = " or ";

    int last = list.size() - 1;
    if (last < 1) {
      return String.join(delimiter, list);
    }

    return String.join(
        lastDelimiter,
        String.join(delimiter, list.subList(0, last)),
        list.get(last));
  }
}
