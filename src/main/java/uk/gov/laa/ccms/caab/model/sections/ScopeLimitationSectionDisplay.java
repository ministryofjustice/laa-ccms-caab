package uk.gov.laa.ccms.caab.model.sections;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the Section display values for a Scope Limitation.
 */
@Data
@Builder
public class ScopeLimitationSectionDisplay {

  /**
   * The scope limitation name.
   */
  private String scopeLimitation;

  /**
   * The wording for the scope limitation.
   */
  private String wording;


}
