package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/**
 * Enumerates the different assessment rulebases.
 */
@Getter
public enum AssessmentRulebase {
  MEANS(1L, "MEANS", "meansAssessment", "global", "CLIENT_PROV_LA", false),
  MERITS(2L, "MERITS", "meritsAssessment", "global", "ASSESS_COMPLETE", false),
  BILLING(3L, "BILLING", "billingAssessment", "global", "BILLING_IS_COMPLETE", true),
  POA(4L, "POA", "poaAssessment", "global", "BILLING_IS_COMPLETE", true);

  private final long id;
  private final String type;
  private final String name;
  private final String rootNameEntity;
  private final String goalAttributeName;
  private final boolean isFinancialAssessment;

  /**
   * Initializes the enum with the specified name string.
   *
   * @param name the string representation of the assessment name
   */
  AssessmentRulebase(
      final Long id,
      final String type,
      final String name,
      final String rootNameEntity,
      final String goalAttributeName,
      final boolean isFinancialAssessment) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.rootNameEntity = rootNameEntity;
    this.goalAttributeName = goalAttributeName;
    this.isFinancialAssessment = isFinancialAssessment;
  }

  /**
   * Retrieves an AssessmentRulebase enum by its type.
   *
   * @param type the string to match against AssessmentRulebase enums.
   * @return the matched AssessmentRulebase enum, or null if no match is found.
   */
  public static AssessmentRulebase findByType(final String type) {
    return Arrays.stream(AssessmentRulebase.values())
        .filter(assessmentRulebase -> assessmentRulebase.getType().equalsIgnoreCase(type))
        .findFirst()
        .orElse(null);
  }

  /**
   * Capitalises the first letter of the assessment name.
   *
   * @return the Assessment deployment name
   */
  public String getDeploymentName() {
    return this.name.substring(0, 1).toUpperCase() + this.name.substring(1);
  }

  /**
   * Appends "_PREPOP" to the current assessment name.
   *
   * @return the pre-populated assessment name
   */
  public String getPrePopAssessmentName() {
    return this.name + "_PREPOP";
  }

  /**
   * Retrieves the pre-populated assessment name for the given ID.
   *
   * @param id the ID of the assessment rule base
   * @return the pre-populated assessment name, or {@code null} if not found
   */
  public static String getPrePopAssessmentName(final Long id) {
    return Arrays.stream(AssessmentRulebase.values())
        .filter(assessmentRulebase -> assessmentRulebase.getId() == id)
        .findFirst()
        .map(AssessmentRulebase::getPrePopAssessmentName)
        .orElse(null);
  }

  /**
   * Retrieves a list of non-financial assessment rulebases.
   *
   * @return List of AssessmentRulebase where isFinancialAssessment() is false.
   */
  public static List<AssessmentRulebase> getNonFinancialRulebases() {
    return Arrays.stream(AssessmentRulebase.values())
        .filter(assessmentRulebase -> !assessmentRulebase.isFinancialAssessment())
        .toList();
  }
}
