package uk.gov.laa.ccms.caab.bean.costs;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Represents form data for costs, including requested cost limitation.
 */
@Data
public class CostsFormData {

  /**
   * The granted cost limitation amount.
   */
  private final BigDecimal grantedCostLimitation;
  /**
   * The requested cost limitation amount.
   */
  private String requestedCostLimitation;
}
