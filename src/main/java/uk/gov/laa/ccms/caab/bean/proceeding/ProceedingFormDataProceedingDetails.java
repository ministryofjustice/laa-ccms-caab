package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;

/**
 * This class represents the details of a proceeding form data.
 * It includes the proceeding type, its display value, the LAR scope, and whether the order type is
 * required.
 */
@Data
public class ProceedingFormDataProceedingDetails {

  /**
   * The type of the proceeding.
   */
  private String proceedingType;

  /**
   * The display value of the proceeding type.
   */
  private String proceedingTypeDisplayValue;

  /**
   * The description of the original proceeding type.
   */
  private String proceedingDescription;

  /**
   * The LAR scope of the proceeding.
   */
  private String larScope;

  /**
   * A flag indicating whether the order type is required.
   */
  private Boolean orderTypeRequired;
}
