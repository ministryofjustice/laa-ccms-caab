package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;

/**
 * This class represents the flow form data of a proceeding.
 * It includes the action, whether it's amended, whether scope limitations are being edited,
 * the ID of the existing proceeding if any, whether it's a lead proceeding,
 * and details about the matter type, proceeding, and further details.
 */
@Data
public class ProceedingFlowFormData {

  /**
   * The action to be performed.
   */
  private String action;

  /**
   * A flag indicating whether the proceeding is amended.
   */
  private boolean amended;

  /**
   * A flag indicating whether scope limitations are being edited.
   */
  private boolean editingScopeLimitations;

  /**
   * The ID of the existing proceeding, if any.
   */
  private Integer existingProceedingId;

  /**
   * A flag indicating whether this is a lead proceeding.
   */
  private boolean leadProceeding;

  /**
   * The details about the matter type.
   */
  private ProceedingFormDataMatterTypeDetails matterTypeDetails;

  /**
   * The details about the proceeding.
   */
  private ProceedingFormDataProceedingDetails proceedingDetails;

  /**
   * The further details about the proceeding.
   */
  private ProceedingFormDataFurtherDetails furtherDetails;

  /**
   * Constructs a new ProceedingFlowFormData with the specified action type.
   * Initializes the matter type details, proceeding details, and further details.
   *
   * @param action the action to be performed
   */
  public ProceedingFlowFormData(final String action) {
    this.action = action;
    this.matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
    this.proceedingDetails = new ProceedingFormDataProceedingDetails();
    this.furtherDetails = new ProceedingFormDataFurtherDetails();
  }
}
