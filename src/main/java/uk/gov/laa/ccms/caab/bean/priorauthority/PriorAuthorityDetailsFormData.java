package uk.gov.laa.ccms.caab.bean.priorauthority;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.JUSTIFICATION_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;

/**
 * This class represents the details of a prior authority details form data.
 */
@Data
@NoArgsConstructor
public class PriorAuthorityDetailsFormData  {

  /**
   * The summary of the prior authority .
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String summary;

  /**
   * The justification for the prior authority.
   */
  @Size(max = JUSTIFICATION_CHARACTER_SIZE)
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
