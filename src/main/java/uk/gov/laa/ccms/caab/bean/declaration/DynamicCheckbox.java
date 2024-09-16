package uk.gov.laa.ccms.caab.bean.declaration;

import lombok.Data;

/**
 * Represents a dynamic option for declaration form data.
 * This is typically used for rendering selectable options such as checkboxes.
 */
@Data
public class DynamicCheckbox {

  /**
   * The identifier of the declaration field.
   */
  private boolean checked;

  /**
   * The displayable checkbox label.
   */
  private String fieldValueDisplayValue;

}
