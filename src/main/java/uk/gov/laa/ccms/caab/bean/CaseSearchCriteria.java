package uk.gov.laa.ccms.caab.bean;

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
  private String caseReference;

  /**
   * The client reference.
   */
  private String clientReference;

  /**
   * The client surname.
   */
  private String clientSurname;

  /**
   * The provider case reference.
   */
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

