package uk.gov.laa.ccms.caab.bean.costs;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;

/** Represents form data for costs, including requested cost limitation. */
@Data
public class CostsFormData {

  /** The granted cost limitation amount. */
  private final BigDecimal grantedCostLimitation;

  /** The requested cost limitation amount. */
  private String requestedCostLimitation;

  /** The provider name. */
  private String providerName;

  /** List of cost entries. */
  private List<CostEntryDetail> costEntries;
}
