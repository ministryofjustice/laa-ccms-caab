package uk.gov.laa.ccms.caab.model.summary;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the display values for a Prior Authority.
 */
@Data
@Builder
public class PriorAuthoritySummaryDisplay {

  /**
   * The description of the prior authority.
   */
  private String description;

  /**
   * The type of prior authority.
   */
  private String type;

  /**
   * The amount requested value.
   */
  private BigDecimal amountRequested;

  /**
   * The status of the prior authority.
   */
  private String status;

}
