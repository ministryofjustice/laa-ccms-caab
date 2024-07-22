package uk.gov.laa.ccms.caab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

/**
 * Represents the display details of an assessment summary attribute.
 */
@Data
@AllArgsConstructor
public class AssessmentSummaryAttributeDisplay {

  /**
   * The name of the assessment summary attribute.
   */
  private String name;

  /**
   * The display name of the assessment summary attribute.
   */
  private String displayName;

  /**
   * The value of the assessment summary attribute.
   */
  private String value;
}
