package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/** Enumerates the names of different types of assessments. */
@Getter
public enum AssessmentName {
  MEANS("meansAssessment", "means"),
  MEANS_PREPOP("meansAssessment_PREPOP", "means"),
  MERITS("meritsAssessment", "merits"),
  MERITS_PREPOP("meritsAssessment_PREPOP", "merits");

  private final String name;
  private final String category;

  /**
   * Initializes the enum with the specified name string.
   *
   * @param name the string representation of the assessment name
   */
  AssessmentName(final String name, final String category) {
    this.name = name;
    this.category = category;
  }

  /**
   * Retrieves a list of strings containing the assessment name by category.
   *
   * @param category the category to match against AssessmentName enums.
   * @return a list of matched assessment names.
   */
  public static List<String> findAssessmentNamesByCategory(final String category) {
    return Arrays.stream(AssessmentName.values())
        .filter(assessmentName -> assessmentName.getCategory().equalsIgnoreCase(category))
        .map(AssessmentName::getName)
        .toList();
  }
}
