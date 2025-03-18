package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * Represents the criteria to search for a Case or Application.
 */
@Data
public class CaseSearchCriteria implements Serializable {

  /**
   * The LAA Application/Case Reference.
   */
  @Size(max = 35)
  private String caseReference;

  /**
   * The client reference.
   */
  private String clientReference;

  /**
   * The client surname.
   */
  @Size(max = 35)
  private String clientSurname;

  /**
   * The provider case reference.
   */
  @Size(max = 35)
  private String providerCaseReference;

  /**
   * The id of the related Fee Earner.
   */
  private Integer feeEarnerId;

  /**
   * The id of the related Office.
   */
  private Integer officeId;

  /**
   * The status value for the Case or Application.
   */
  private String status;
}

