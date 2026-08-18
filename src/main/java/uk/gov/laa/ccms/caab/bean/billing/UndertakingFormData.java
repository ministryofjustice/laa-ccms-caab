package uk.gov.laa.ccms.caab.bean.billing;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

/** View model for the undertaking screen. */
@Data
@Accessors(chain = true)
public class UndertakingFormData {

  /** The undertaking amount shown on the page. */
  private String undertakingAmount;

  /** Whether the undertaking terms were accepted. */
  private boolean acceptedTerms;

  /** Minimum undertaking amount allowed for the case. */
  private BigDecimal undertakingMinimumAmount;

  /** Maximum undertaking amount used for quick amendment submission. */
  private BigDecimal undertakingMaximumAmount;
}
