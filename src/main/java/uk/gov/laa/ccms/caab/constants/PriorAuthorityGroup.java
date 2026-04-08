package uk.gov.laa.ccms.caab.constants;

import java.util.List;

/** Enumeration of the possible groups for a Prior Authority. */
public enum PriorAuthorityGroup {
  COUNSEL_DETAILS("Counsel details"),
  EXPERT_DETAILS("Expert details"),
  TIME_SPENT("Time spent"),
  RATES("Rates"),
  COSTS("Costs"),
  REASONING("Reasoning"),
  EXPENSE_DETAILS("Expense details"),
  OTHER("Other");

  private final String label;

  PriorAuthorityGroup(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /**
   * Method that returns a list of groups within a given Prior Authority depending on its type.
   *
   * @param typeId the type of Prior Authority (counsel, expert, or other expense)
   * @return returns a list of groups for a given type of Prior Authority
   */
  public static List<PriorAuthorityGroup> getGroupsForType(String typeId) {
    return switch (typeId != null ? typeId.toUpperCase() : "") {
      case "COUNSEL" -> List.of(COUNSEL_DETAILS, OTHER);
      case "EXPERT" -> List.of(EXPERT_DETAILS, TIME_SPENT, RATES, COSTS, REASONING, OTHER);
      case "OTHER" -> List.of(EXPENSE_DETAILS, OTHER);
      default -> List.of(OTHER);
    };
  }
}
