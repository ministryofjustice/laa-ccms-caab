package uk.gov.laa.ccms.caab.bean.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * Represents a generic file upload form.
 */
@Data
@Slf4j
public abstract class FileUploadFormData {

  /**
   * The multipart file data to upload.
   */
  private MultipartFile file;

  /**
   * The file extension.
   */
  private String fileExtension;

  /**
   * The type of document.
   */
  private String documentType;

  /**
   * The display value for the type of document.
   */
  private String documentTypeDisplayValue;

  /**
   * A description of the document.
   */
  private String documentDescription;

}
