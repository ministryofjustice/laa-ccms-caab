package uk.gov.laa.ccms.caab.model.summary;

import lombok.Data;

/** Represents the scope limitation submission summary display. */
@Data
public class ScopeLimitationSubmissionSummaryDisplay {

  /** The scope limitation. */
  private String scopeLimitation;

  /** The wording of the scope limitation. */
  private String scopeLimitationWording;
}
