package uk.gov.laa.ccms.caab.bean.evidence;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;
import uk.gov.laa.ccms.caab.constants.CcmsModule;

/**
 * Represents an evidence upload form.
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class EvidenceUploadFormData extends FileUploadFormData implements Serializable {

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
  private String applicationOrOutcomeId;

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
  private String documentSender;

  /**
   * The area of the site performing the upload.
   */
  private CcmsModule ccmsModule;

}
