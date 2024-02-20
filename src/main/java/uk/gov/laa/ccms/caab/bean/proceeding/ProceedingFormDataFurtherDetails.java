package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;

/**
 * This class represents the further details of a proceeding form data.
 * It includes the client involvement type and its display value, the level of service and its
 * display value, and the type of order and its display value.
 */
@Data
public class ProceedingFormDataFurtherDetails {

  /**
   * The type of client involvement.
   */
  private String clientInvolvementType;

  /**
   * The display value of the client involvement type.
   */
  private String clientInvolvementTypeDisplayValue;

  /**
   * The level of service.
   */
  private String levelOfService;

  /**
   * The display value of the level of service.
   */
  private String levelOfServiceDisplayValue;

  /**
   * The type of order.
   */
  private String typeOfOrder;

  /**
   * The display value of the type of order.
   */
  private String typeOfOrderDisplayValue;
}
