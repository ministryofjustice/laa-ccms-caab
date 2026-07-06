package uk.gov.laa.ccms.caab.bean.billing;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Holds one column of statement of account figures (e.g. the current provider, the aggregated prior
 * solicitors, or the aggregated counsel) as displayed on the Case Statement of Account screen.
 */
@Data
public class SoaFigureColumn {

  private BigDecimal certificateCostLimitation;
  private BigDecimal undertaking;
  private BigDecimal billsAuthorised;
  private BigDecimal billsSubmittedButNotAuthorised;
  private BigDecimal poaRecouped;
  private BigDecimal poaAuthorised;
  private BigDecimal poaSubmittedButNotAuthorised;
  private BigDecimal costLimitationRemaining;
}
