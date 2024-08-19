package uk.gov.laa.ccms.caab.bean.validators.file;

import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Validate the evidence document details provided by evidence upload flow.
 */
@Getter
@RequiredArgsConstructor
public abstract class FileUploadValidator extends AbstractValidator {

  /**
   * The error message for the required file field.
   */
  protected static final String FILE_REQUIRED_ERROR = "Please select a file to upload";

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
  private final List<String> validExtensions;

  /**
   * The configurable maximum allowed file size.
   */
  private final String maxFileSize;

  /**
   * Validate generic file upload details such as file data, extension, type, description and
   * size.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateFile(FileUploadFormData fileUploadFormData, Errors errors) {

    if (fileUploadFormData.getFile() == null
        || fileUploadFormData.getFile().isEmpty()) {
      errors.rejectValue("file", "required.file", FILE_REQUIRED_ERROR);
    } else {
      fileUploadFormData.setFileExtension(getFileExtension(fileUploadFormData.getFile()));

      if (!isValidExtension(fileUploadFormData.getFileExtension())) {
        errors.rejectValue("file", "invalid.extension",
            String.format(INVALID_EXTENSION_ERROR, getCommaDelimitedString(validExtensions)));
      } else {
        validateFileSize(fileUploadFormData, errors);
      }
    }

    validateDocumentType(fileUploadFormData, errors);
    validateDocumentDescription(fileUploadFormData, errors);
  }

  /**
   * Validate the document description text input.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateDocumentDescription(FileUploadFormData fileUploadFormData, Errors errors) {
    validateFieldMaxLength("documentDescription",
        fileUploadFormData.getDocumentDescription(),
        DOCUMENT_DESCRIPTION_MAX_LENGTH, "description", errors);
  }

  /**
   * Validate the selected document type input.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateDocumentType(FileUploadFormData fileUploadFormData, Errors errors) {
    validateRequiredField("documentType", fileUploadFormData.getDocumentType(),
        "Document type", errors);
  }

  /**
   * Validate the size of a file based on the max size configured by the application.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  private void validateFileSize(FileUploadFormData fileUploadFormData, Errors errors) {
    try {
      // Check the file size is within limits
      final int fileSize = fileUploadFormData.getFile().getBytes().length;
      final long maxSize = DataSize.parse(maxFileSize).toBytes();

      if (fileSize > maxSize) {
        rejectFileSize(errors);
      }
    } catch (IOException ioe) {
      throw new CaabApplicationException("Failed to read file data", ioe);
    }
  }

  /**
   * Reject the uploaded file due to exceeded file size.
   *
   * @param errors the Errors object to store validation errors.
   */
  public void rejectFileSize(Errors errors) {
    errors.rejectValue("file", "max.filesize.exceeded",
        String.format(MAX_FILESIZE_ERROR, maxFileSize));
  }

  /**
   * Check whether a file extension is valid based on the allowed extensions configured by the
   * application.
   *
   * @param fileExtension the file extension.
   * @return true if the file extension is valid, false otherwise.
   */
  protected boolean isValidExtension(final String fileExtension) {
    return validExtensions.stream().anyMatch(ext -> ext.equalsIgnoreCase(fileExtension));
  }

}
