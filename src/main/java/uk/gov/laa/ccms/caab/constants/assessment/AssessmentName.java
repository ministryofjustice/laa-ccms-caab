package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Enumerates the names of different types of assessments.
 */
@Getter
public enum AssessmentName {
  MEANS("meansAssessment"),
  MERITS("meritsAssessment");

  private final String name;

  /**
   * Initializes the enum with the specified name string.
   *
   * @param name the string representation of the assessment name
   */
  AssessmentName(final String name) {
    this.name = name;
  }
}
