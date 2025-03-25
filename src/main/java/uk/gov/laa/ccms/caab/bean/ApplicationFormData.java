package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the details of an application.
 */
@Data
public class ApplicationFormData {

  /**
   * The caseReferenceNumber selected for a Copy Case.
   */
  private String copyCaseReferenceNumber;

  /**
   * The case client first name for a Copy Case.
   */
  private String copyCaseClientFirstName;

  /**
   * The ID of the office related to this application.
   */
  private Integer officeId;

  /**
   * The name of the office related to this application, used for the edit provider details, part
   * of application summary sections.
   */
  private String officeName;

  /**
   * The ID of the category of law related to this application.
   */
  private String categoryOfLawId;

  /**
   * Flag indicating whether exceptional funding has been requested for this application.
   */
  private boolean exceptionalFunding;

  /**
   * The category of the application type.
   */
  private String applicationTypeCategory;

  /**
   * The option for delegated functions.
   */
  private boolean delegatedFunctions = false;

  /**
   * The date when delegated function was used.
   */
  private String delegatedFunctionUsedDate;
  /**
   * The option for privacy notice agreement.
   */
  private boolean agreementAccepted = false;

  /**
   * The boolean to control the routing after the privacy notice agreement.
   */
  private boolean applicationCreated = false;

  /**
   * The string to control contractual devolved powers flag, used for edit application type, not
   * on creation of an application.
   */
  private String devolvedPowersContractFlag;

  /**
   * The string containing the fee earners id, used for the edit provider details, part of
   * application summary sections.
   */
  private Integer feeEarnerId;

  /**
   * The string containing the supervisor id, used for the edit provider details, part of
   * application summary sections.
   */
  private Integer supervisorId;

  /**
   * The string containing the provider case reference, used for the edit provider details, part of
   * application summary sections.
   */
  private String providerCaseReference;

  /**
   * The string containing the contact name id, used for the edit provider details, part of
   * application summary sections.
   */
  private String contactNameId;

}

