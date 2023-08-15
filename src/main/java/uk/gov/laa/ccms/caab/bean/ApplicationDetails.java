package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the details of an application.
 */
@Data
public class ApplicationDetails {

  /**
   * The caseReferenceNumber selected for a Copy Case.
   */
  private String copyCaseReferenceNumber;

  /**
   * The ID of the office related to this application.
   */
  private Integer officeId;

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
   * The day when delegated function was used.
   */
  private String delegatedFunctionUsedDay;

  /**
   * The month when delegated function was used.
   */
  private String delegatedFunctionUsedMonth;

  /**
   * The year when delegated function was used.
   */
  private String delegatedFunctionUsedYear;

  /**
   * The option for privacy notice agreement.
   */
  private boolean agreementAccepted = false;

  /**
   * The boolean to control the routing after the privacy notice agreement.
   */
  private boolean applicationCreated = false;

}

