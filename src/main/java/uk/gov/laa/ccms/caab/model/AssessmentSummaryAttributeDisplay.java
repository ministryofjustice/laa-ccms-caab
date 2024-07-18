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
  private String name;
  private String displayName;
  private String value;
}
