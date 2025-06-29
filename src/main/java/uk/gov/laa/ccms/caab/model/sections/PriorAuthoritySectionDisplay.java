package uk.gov.laa.ccms.caab.model.sections;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/** Represents the Section display values for a Prior Authority. */
@Data
@Builder
public class PriorAuthoritySectionDisplay {

  /** The description of the prior authority. */
  private String description;

  /** The type of prior authority. */
  private String type;

  /** The amount requested value. */
  private BigDecimal amountRequested;

  /** The status of the prior authority. */
  private String status;
}
