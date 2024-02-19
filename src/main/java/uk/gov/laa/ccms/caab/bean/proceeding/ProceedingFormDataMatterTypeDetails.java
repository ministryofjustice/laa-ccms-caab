package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Data;

/**
 * This class represents the matter type details of a proceeding form data.
 * It includes the matter type and its display value.
 */
@Data
public class ProceedingFormDataMatterTypeDetails {

  /**
   * The type of the matter.
   */
  private String matterType;

  /**
   * The display value of the matter type.
   */
  private String matterTypeDisplayValue;
}
