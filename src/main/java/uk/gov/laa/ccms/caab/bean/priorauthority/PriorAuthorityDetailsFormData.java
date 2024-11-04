package uk.gov.laa.ccms.caab.bean.priorauthority;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;

/**
 * This class represents the details of a prior authority details form data.
 */
@Data
@NoArgsConstructor
public class PriorAuthorityDetailsFormData  {

  /**
   * The summary of the prior authority .
   */
  private String summary;

  /**
   * The justification for the prior authority.
   */
  private String justification;

  /**
   * A flag indicating whether a value is required for the prior authority.
   */
  private boolean valueRequired;

  /**
   * The amount requested in the prior authority.
   */
  private String amountRequested;

  /**
   * A map of dynamic options for the prior authority.
   * The key is the option name and the value is the PriorAuthorityFormDataDynamicOption object.
   */
  private Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();

}
