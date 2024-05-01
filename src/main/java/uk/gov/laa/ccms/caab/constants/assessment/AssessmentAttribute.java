package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Defines the attributes associated with an assessment.
 */
@Getter
public enum AssessmentAttribute {
  MATTER_TYPE("MATTER_TYPE"),
  PROCEEDING_NAME("PROCEEDING_NAME"),
  CLIENT_INVOLVEMENT_TYPE("CLIENT_INVOLVEMENT_TYPE"),
  REQUESTED_SCOPE("REQUESTED_SCOPE");

  private final String attribute;

  /**
   * Initializes the enum with the specified attribute string.
   *
   * @param attribute the string representation of the assessment attribute
   */
  AssessmentAttribute(final String attribute) {
    this.attribute = attribute;
  }

}

