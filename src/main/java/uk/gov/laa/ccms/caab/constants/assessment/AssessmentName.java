package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
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

  /**
   * Retrieves an AssessmentName enum by its name.
   *
   * @param name the name string to match against AssessmentName enums.
   * @return the matched AssessmentName enum, or null if no match is found.
   */
  public static AssessmentName findByName(final String name) {
    return Arrays.stream(AssessmentName.values())
        .filter(assessmentName -> assessmentName.getName().equals(name))
        .findFirst()
        .orElse(null);
  }
}
