package uk.gov.laa.ccms.caab.model.assessment;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Represents the display details of an assessment summary attribute. */
@Data
@AllArgsConstructor
public class AssessmentSummaryAttributeDisplay {

  /** The name of the assessment summary attribute. */
  private String name;

  /** The display name of the assessment summary attribute. */
  private String displayName;

  /** The value of the assessment summary attribute. */
  private String value;
}
