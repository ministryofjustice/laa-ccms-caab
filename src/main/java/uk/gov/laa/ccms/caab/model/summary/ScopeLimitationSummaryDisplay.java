package uk.gov.laa.ccms.caab.model.summary;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the Summary display values for a Scope Limitation.
 */
@Data
@Builder
public class ScopeLimitationSummaryDisplay {

  /**
   * The scope limitation name.
   */
  private String scopeLimitation;

  /**
   * The wording for the scope limitation.
   */
  private String wording;


}
