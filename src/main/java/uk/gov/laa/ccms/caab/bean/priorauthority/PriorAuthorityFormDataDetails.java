package uk.gov.laa.ccms.caab.bean.priorauthority;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents the details of a prior authority details form data.
 */
@Data
@NoArgsConstructor
public class PriorAuthorityFormDataDetails {

  private String summary;
  private String justification;

  private boolean valueRequired;
  private String amountRequested;

  private Map<String, PriorAuthorityFormDataDynamicOption> dynamicOptions = new HashMap<>();

}
