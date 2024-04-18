package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Represents the different statuses an assessment can have within the system.
 */
@Getter
public enum AssessmentStatus {
  COMPLETE("COMPLETE"),
  ERROR("ERROR"),
  NOT_STARTED("NOT_STARTED"),
  REQUIRED("REQUIRED"),
  UNCHANGED("UNCHANGED");

  private final String status;

  /**
   * Initializes the enum with the specified status string.
   *
   * @param status the string representation of the assessment status
   */
  AssessmentStatus(final String status) {
    this.status = status;
  }

}

