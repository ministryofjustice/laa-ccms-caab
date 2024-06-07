package uk.gov.laa.ccms.caab.bean.evidence;

import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.constants.CcmsModule;

/**
 * Represents an evidence upload form.
 */
@Data
@Slf4j
public class EvidenceUploadFormData {

  /**
   * The multipart file data to upload.
   */
  private MultipartFile file;

  /**
   * The file extension.
   */
  private String fileExtension;

  /**
   * The type of evidence document.
   */
  private String documentType;

  /**
   * The display value for the type of evidence document.
   */
  private String documentTypeDisplayValue;

  /**
   * A description of the document.
   */
  private String documentDescription;

  /**
   * The evidence types covered by the uploaded file.
   */
  private List<String> evidenceTypes;

  /**
   * The ebs registered id for this document.
   */
  private String registeredDocumentId;

  /**
   * The application or outcome that this upload relates to.
   */
  private Integer applicationOrOutcomeId;

  /**
   * The related case reference number.
   */
  private String caseReferenceNumber;

  /**
   * The related provider id.
   */
  private Integer providerId;

  /**
   * The user id performing the upload.
   */
  private Integer documentSender;

  /**
   * The area of the site performing the upload.
   */
  private CcmsModule ccmsModule;

}
