package uk.gov.laa.ccms.caab.bean.evidence;

import java.util.List;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Represents an evidence upload form.
 */
@Data
public class EvidenceUploadFormData {

  /**
   * The multipart file data to upload.
   */
  private MultipartFile file;

  /**
   * The type of evidence document.
   */
  private String documentType;

  /**
   * A description of the document.
   */
  private String documentDescription;

  /**
   * The evidence types covered by the uploaded file.
   */
  private List<String> evidenceTypes;


}
