package uk.gov.laa.ccms.caab.bean.scopelimitation;

import lombok.Data;

/**
 * This class represents the flow form data of a scope limitation. It includes the action to be
 * performed, the ID and index of the scope limitation, and details about the scope limitation.
 */
@Data
public class ScopeLimitationFlowFormData {

  /** The details about the scope limitation. */
  private ScopeLimitationFormDataDetails scopeLimitationDetails;

  /** The action to be performed. */
  private String action;

  /** The database identifier of the scope limitation. */
  private Integer scopeLimitationId;

  /** The index of the scope limitation stored in the session only. */
  private Integer scopeLimitationIndex;

  /**
   * Constructs a new ScopeLimitationFlowFormData with the specified action type. Initializes the
   * scope limitation details.
   *
   * @param action the action to be performed
   */
  public ScopeLimitationFlowFormData(final String action) {
    this.action = action;
    this.scopeLimitationDetails = new ScopeLimitationFormDataDetails();
  }
}
