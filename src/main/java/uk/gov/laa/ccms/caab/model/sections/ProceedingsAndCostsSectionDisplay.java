package uk.gov.laa.ccms.caab.model.sections;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Represents the Section display values for proceedings and costs. */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsAndCostsSectionDisplay extends ApplicationSectionStatusDisplay {

  /** The requested cost limitation value. */
  private BigDecimal requestedCostLimitation;

  /** The granted cost limitation value. */
  private BigDecimal grantedCostLimitation;

  /** The default cost limitation value. */
  private BigDecimal defaultCostLimitation;

  /** The list of proceedings for the application. */
  private List<ProceedingSectionDisplay> proceedings;
}
