package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
import lombok.Getter;

/** Represents the different statuses an assessment can have within the system. */
@Getter
public enum AssessmentStatus {
  COMPLETE("COMPLETE"),
  ERROR("ERROR"),
  NOT_STARTED("NOT_STARTED"),
  REQUIRED("REQUIRED"),
  UNCHANGED("UNCHANGED"),
  INCOMPLETE("INCOMPLETE");

  private final String status;

  /**
   * Initializes the enum with the specified status string.
   *
   * @param status the string representation of the assessment status
   */
  AssessmentStatus(final String status) {
    this.status = status;
  }

  /**
   * Finds and returns an AssessmentStatus enum by its status string.
   *
   * @param status the status string to match.
   * @return the matched AssessmentStatus enum, or null if no match is found.
   */
  public static AssessmentStatus findByStatus(final String status) {
    return Arrays.stream(AssessmentStatus.values())
        .filter(assessmentStatus -> assessmentStatus.getStatus().equalsIgnoreCase(status))
        .findFirst()
        .orElse(null);
  }
}
