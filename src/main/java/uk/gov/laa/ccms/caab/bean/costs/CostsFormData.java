package uk.gov.laa.ccms.caab.bean.costs;

import lombok.Data;

/**
 * Represents form data for costs, including requested cost limitation.
 */
@Data
public class CostsFormData {

  /**
   * The requested cost limitation amount.
   */
  private String requestedCostLimitation;
}
