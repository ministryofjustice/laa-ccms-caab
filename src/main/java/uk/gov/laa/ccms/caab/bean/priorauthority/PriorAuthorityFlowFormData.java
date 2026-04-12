package uk.gov.laa.ccms.caab.bean.priorauthority;

import java.util.HashMap;
import lombok.Data;

/**
 * This class represents the form data of a prior authority flow. It includes the details of the
 * prior authority type form data and the action to be performed.
 */
@Data
public class PriorAuthorityFlowFormData {

  /** The details of the prior authority type form data. */
  private PriorAuthorityTypeFormData priorAuthorityTypeFormData;

  /** The details of the prior authority form data, including the dynamic form information. */
  private PriorAuthorityDetailsFormData priorAuthorityDetailsFormData;

  /** The action to be performed. */
  private String action;

  /** The database identifier of the prior authority. */
  private Integer priorAuthorityId;

  /**
   * Constructs a new PriorAuthorityFlowFormData with the specified action type. Initializes the
   * prior authority type form data details.
   *
   * @param action the action to be performed
   */
  public PriorAuthorityFlowFormData(final String action) {
    this.action = action;
    this.priorAuthorityTypeFormData = new PriorAuthorityTypeFormData();
    this.priorAuthorityDetailsFormData = new PriorAuthorityDetailsFormData();
  }

  /**
   * Resets the details form so that changing the prior authority type starts with a clean
   * state for the new type.
   *
   */
  public void resetForNewType() {
    this.priorAuthorityDetailsFormData = new PriorAuthorityDetailsFormData();

    if (this.priorAuthorityDetailsFormData.getDynamicOptions() != null) {
      this.priorAuthorityDetailsFormData.setDynamicOptions(new HashMap<>());
    }
  }
}
