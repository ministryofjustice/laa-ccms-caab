package uk.gov.laa.ccms.caab.constants;

import java.util.List;

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

  public static List<PriorAuthorityGroup> getGroupsForType(String typeId) {
    return switch (typeId != null ? typeId.toUpperCase() : "") {
      case "COUNSEL" -> List.of(COUNSEL_DETAILS, OTHER);
      case "EXPERT" -> List.of(EXPERT_DETAILS, TIME_SPENT, RATES, COSTS, REASONING, OTHER);
      case "OTHER" -> List.of(EXPENSE_DETAILS, OTHER);
      default -> List.of(OTHER);
    };
  }
}
