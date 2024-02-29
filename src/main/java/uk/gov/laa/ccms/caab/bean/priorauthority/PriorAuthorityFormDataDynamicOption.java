package uk.gov.laa.ccms.caab.bean.priorauthority;

import lombok.Data;

/**
 * Represents a dynamic option in prior authority form data.
 */
@Data
public class PriorAuthorityFormDataDynamicOption {

  /**
   * The identifier of the field value, mapped from ReferenceDataItem's
   * value.id.
   */
  private String fieldValue;

  /**
   * The displayable string of the field value, mapped from ReferenceDataItem's
   * value.displayValue.
   */
  private String fieldValueDisplayValue;

  /**
   * The description of the field, mapped from ReferenceDataItem's
   * code.displayValue.
   */
  private String fieldDescription;

  /**
   * The type of the field, mapped from ReferenceDataItem's
   * type.
   */
  private String fieldType;

  /**
   * Indicates if the field is mandatory, mapped from ReferenceDataItem's
   * mandatory flag.
   */
  private boolean mandatory;
}
