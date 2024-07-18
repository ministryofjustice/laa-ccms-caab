package uk.gov.laa.ccms.caab.model.summary;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Summary display values for proceedings and costs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsAndCostsSummaryDisplay extends ApplicationSummaryStatusDisplay {

  /**
   * The requested cost limitation value.
   */
  private BigDecimal requestedCostLimitation;

  /**
   * The granted cost limitation value.
   */
  private BigDecimal grantedCostLimitation;

  /**
   * The list of proceedings for the application.
   */
  private List<ProceedingSummaryDisplay> proceedings;

}
