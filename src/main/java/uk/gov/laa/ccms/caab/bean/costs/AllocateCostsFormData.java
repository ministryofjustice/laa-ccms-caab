package uk.gov.laa.ccms.caab.bean.costs;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;

/** Represents form data for allocate costs. */
@Data
public class AllocateCostsFormData {

  private BigDecimal grantedCostLimitation;

  private BigDecimal requestedCostLimitation;

  private String providerName;

  private List<CostEntryDetail> costEntries;

  private BigDecimal totalRemaining;

  private BigDecimal currentProviderBilledAmount;
}
