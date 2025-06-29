package uk.gov.laa.ccms.caab.bean.priorauthority;

import lombok.Data;

/**
 * This class represents the details of a prior authority type form data. It includes the prior
 * authority type and its display value.
 */
@Data
public class PriorAuthorityTypeFormData {

  /** The type of the prior authority. */
  private String priorAuthorityType;

  /** The display value of the prior authority type. */
  private String priorAuthorityTypeDisplayValue;
}
