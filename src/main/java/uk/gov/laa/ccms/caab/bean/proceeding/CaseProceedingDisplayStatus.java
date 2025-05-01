package uk.gov.laa.ccms.caab.bean.proceeding;

import lombok.Getter;

/**
 * The proceeding status values used for display purposes.
 */
@Getter
public enum CaseProceedingDisplayStatus {
  OUTCOME("Outcome"),
  SUBMITTED("Submitted"),
  ADDED("Added"),
  UNCHANGED("Unchanged"),
  UPDATED("Updated"),;

  private final String status;

  /**
   * Initializes the enum with the specified status  string.
   *
   * @param status the string representation of the proceeding status
   */
  CaseProceedingDisplayStatus(final String status) {
    this.status = status;
  }
}
