package uk.gov.laa.ccms.caab.bean.billing;

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
}
