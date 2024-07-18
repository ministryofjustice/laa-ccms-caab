package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the Summary display values for a proceeding.
 */
@Data
@Builder
public class ProceedingSummaryDisplay {

  /**
   * The type of proceeding.
   */
  private String proceedingType;

  /**
   * The matter type for the proceeding.
   */
  private String matterType;

  /**
   * The proceeding's level of service.
   */
  private String levelOfService;

  /**
   * The client involvement level.
   */
  private String clientInvolvement;

  /**
   * The status of the proceeding.
   */
  private String status;

  /**
   * The list of scope limitations.
   */
  private List<ScopeLimitationSummaryDisplay> scopeLimitations;
  

}
