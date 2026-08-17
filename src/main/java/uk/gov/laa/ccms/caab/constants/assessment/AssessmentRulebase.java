package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/** Enumerates the different assessment rulebases. */
@Getter
public enum AssessmentRulebase {
  MEANS(
      1L,
      "MEANS",
      "MEANS",
      "meansAssessment",
      "MeansAssessment",
      "global",
      "CLIENT_PROV_LA",
      false),
  MERITS(
      2L,
      "MERITS",
      "MERITS",
      "meritsAssessment",
      "MeritsAssessment",
      "global",
      "ASSESS_COMPLETE",
      false),
  BILLING(
      3L,
      "BILLING",
      "BILLING",
      "billingAssessment",
      "BillingAssessment",
      "global",
      "BILLING_IS_COMPLETE",
      true),
  // There is no POA rulebase deployed to OWD: the POA journey runs the shared BillingAssessment
  // deployment, which switches behaviour on POA_OR_BILL_FLAG. Old PUI does the same - its
  // poaAssessment definition carries assessmentType "BILLING". Only the stored assessment name
  // ("poaAssessment") differs, which is what the connector keys its TDS data off.
  POA(
      4L,
      "POA",
      "BILLING",
      "poaAssessment",
      "BillingAssessment",
      "global",
      "BILLING_IS_COMPLETE",
      true);

  private final long id;
  private final String type;
  private final String ebsAssessmentType;
  private final String name;
  private final String deploymentName;
  private final String rootNameEntity;
  private final String goalAttributeName;
  private final boolean isFinancialAssessment;

  /**
   * Initializes the enum with the specified name string.
   *
   * @param name the string representation of the assessment name
   * @param ebsAssessmentType the type EBS holds the stored assessment data under. POA shares
   *     BILLING, as it shares the rulebase - old PUI's poaAssessment definition carries
   *     assessmentType "BILLING".
   * @param deploymentName the name of the rulebase deployed to OWD, which is not always derivable
   *     from the assessment name
   */
  AssessmentRulebase(
      final Long id,
      final String type,
      final String ebsAssessmentType,
      final String name,
      final String deploymentName,
      final String rootNameEntity,
      final String goalAttributeName,
      final boolean isFinancialAssessment) {
    this.id = id;
    this.type = type;
    this.ebsAssessmentType = ebsAssessmentType;
    this.name = name;
    this.deploymentName = deploymentName;
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
   * Retrieves an AssessmentRulebase enum by its assessment name, e.g. "poaAssessment".
   *
   * @param name the name to match against AssessmentRulebase enums.
   * @return the matched AssessmentRulebase enum, or null if no match is found.
   */
  public static AssessmentRulebase findByName(final String name) {
    return Arrays.stream(AssessmentRulebase.values())
        .filter(assessmentRulebase -> assessmentRulebase.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves an AssessmentRulebase enum by its id.
   *
   * @param id the id to match against AssessmentRulebase enums.
   * @return the matched AssessmentRulebase enum, or null if no match is found.
   */
  public static AssessmentRulebase findById(final Long id) {
    return Arrays.stream(AssessmentRulebase.values())
        .filter(assessmentRulebase -> assessmentRulebase.getId() == id)
        .findFirst()
        .orElse(null);
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
