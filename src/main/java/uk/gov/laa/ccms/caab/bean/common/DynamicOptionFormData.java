package uk.gov.laa.ccms.caab.bean.common;

import lombok.Data;

/**
 * Represents a dynamic option in form data, used for prior authorities and provider requests.
 */
@Data
public class DynamicOptionFormData {

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
