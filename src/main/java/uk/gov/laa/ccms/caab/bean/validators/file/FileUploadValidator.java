package uk.gov.laa.ccms.caab.bean.validators.file;

import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/** Validate the evidence document details provided by evidence upload flow. */
@Getter
@RequiredArgsConstructor
public abstract class FileUploadValidator extends AbstractValidator {

  /** The error message for the required file field. */
  protected static final String FILE_REQUIRED_ERROR = "Please select a file to upload";

  protected static final String FILE_REQUIRED_ERROR_CODE = "validation.error.selectAFile";

  protected static final String INVALID_FILE_NAME_ERROR_CODE = "validation.error.invalidFileName";

  /** The error message for an invalid file extension. */
  protected static final String INVALID_EXTENSION_ERROR =
      "Invalid file extension. We can only accept %s files";

  /** The error message for an invalid mime type. */
  protected static final String INVALID_MIME_TYPE_ERROR =
      "Invalid mime type. We can only accept %s mime types";

  protected static final String INVALID_EXTENSION_ERROR_CODE = "validation.error.invalidExtension";

  protected static final String INVALID_MIME_TYPE_ERROR_CODE = "validation.error.invalidMimeType";

  protected static final String INVALID_MAGIC_BYTES_ERROR_CODE =
      "validation.error.invalidMagicBytes";

  protected static final String MULTIPLE_EXTENSION_ERROR_CODE =
      "validation.error.multipleExtension";

  /** The error message for an invalid file extension. */
  public static final String MAX_FILESIZE_ERROR =
      "File is too large. The file must be less than %s";

  protected static final String MAX_FILESIZE_ERROR_CODE = "validation.error.maxFileSize";

  /** The maximum length of the document description text area. */
  protected static final Integer DOCUMENT_DESCRIPTION_MAX_LENGTH = 255;

  /** The configurable list of valid file extensions. */
  private final List<String> validExtensions;

  /** The configurable maximum allowed file size. */
  private final String maxFileSize;

  /** The configurable list of mime types. */
  private final List<String> validMimeTypes;

  private static final Tika tika = new Tika();

  /**
   * Validate generic file upload details such as file data, extension, type, description and size.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateFile(FileUploadFormData fileUploadFormData, Errors errors) {

    if (fileUploadFormData.getFile() == null || fileUploadFormData.getFile().isEmpty()) {
      errors.rejectValue("file", FILE_REQUIRED_ERROR_CODE, FILE_REQUIRED_ERROR);
    } else {
      fileUploadFormData.setFileExtension(getFileExtension(fileUploadFormData.getFile()));

      if (!hasValidFileName(fileUploadFormData)) {
        errors.rejectValue("file", INVALID_FILE_NAME_ERROR_CODE);
      }

      if (!hasSingleExtension(fileUploadFormData)) {
        errors.rejectValue("file", MULTIPLE_EXTENSION_ERROR_CODE);
      }

      if (!isValidExtension(fileUploadFormData.getFileExtension())) {
        errors.rejectValue(
            "file",
            INVALID_EXTENSION_ERROR_CODE,
            new String[] {getCommaDelimitedString(validExtensions)},
            INVALID_EXTENSION_ERROR.formatted(getCommaDelimitedString(validExtensions)));
      } else {
        validateFileSize(fileUploadFormData, errors);

        if (!isValidMimeType(fileUploadFormData.getFile().getContentType())) {
          errors.rejectValue(
              "file",
              INVALID_MIME_TYPE_ERROR_CODE,
              new String[] {getCommaDelimitedString(validMimeTypes)},
              INVALID_MIME_TYPE_ERROR.formatted(getCommaDelimitedString(validMimeTypes)));
        } else {
          if (!isValidMagicBytes(fileUploadFormData)) {
            errors.rejectValue("file", INVALID_MAGIC_BYTES_ERROR_CODE);
          }
        }
      }
    }
  }

  /**
   * Validate the document description text input.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateDocumentDescription(FileUploadFormData fileUploadFormData, Errors errors) {
    validateFieldMaxLength(
        "documentDescription",
        fileUploadFormData.getDocumentDescription(),
        DOCUMENT_DESCRIPTION_MAX_LENGTH,
        "description",
        errors);
  }

  /**
   * Validate the selected document type input.
   *
   * @param fileUploadFormData the file upload form data object.
   * @param errors the Errors object to store validation errors.
   */
  public void validateDocumentType(FileUploadFormData fileUploadFormData, Errors errors) {
    validateRequiredField(
        "documentType", fileUploadFormData.getDocumentType(), "Document type", errors);
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
    errors.rejectValue(
        "file",
        MAX_FILESIZE_ERROR_CODE,
        new String[] {maxFileSize},
        MAX_FILESIZE_ERROR.formatted(maxFileSize));
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

  /**
   * Check whether mime type is valid based on the allowed mime types configured by the application.
   *
   * @param mimeType content type of the file.
   * @return true if the mime type is valid, false otherwise.
   */
  protected boolean isValidMimeType(final String mimeType) {
    return validMimeTypes.stream().anyMatch(type -> type.equalsIgnoreCase(mimeType));
  }

  /**
   * Check whether a file is valid by ensuring it does not have multiple extensions.
   *
   * @param fileUploadFormData the file upload form data object.
   * @return true if the file only has a single extension, false otherwise.
   */
  protected boolean hasSingleExtension(FileUploadFormData fileUploadFormData) {
    String filename = StringUtils.cleanPath(fileUploadFormData.getFile().getOriginalFilename());

    int lastDot = filename.lastIndexOf('.');
    int firstDot = filename.indexOf('.');

    return (firstDot > 0) && (firstDot == lastDot) && lastDot < (filename.length() - 1);
  }

  /**
   * Check whether a file has a valid filename using a regex expression.
   *
   * @param fileUploadFormData the file upload form data object.
   * @return true if the filename is valid, false otherwise.
   */
  protected boolean hasValidFileName(FileUploadFormData fileUploadFormData) {
    String filename = StringUtils.cleanPath(fileUploadFormData.getFile().getOriginalFilename());

    return filename.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$");
  }

  /**
   * Checks whether the file content matches the file type.
   *
   * @param fileUploadFormData the file upload form data object.
   * @return true if the file content matches the file type, false otherwise.
   */
  protected boolean isValidMagicBytes(FileUploadFormData fileUploadFormData) {
    try (InputStream inputStream = fileUploadFormData.getFile().getInputStream()) {
      String detectedMime = tika.detect(inputStream);

      return validMimeTypes.stream().anyMatch(type -> type.equalsIgnoreCase(detectedMime));
    } catch (IOException e) {
      throw new CaabApplicationException("Failed to read file data", e);
    }
  }
}
