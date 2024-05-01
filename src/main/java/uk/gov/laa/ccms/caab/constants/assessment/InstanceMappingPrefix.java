package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Enumerates the prefix strings used for instance mapping in assessments.
 */
@Getter
public enum InstanceMappingPrefix {
  PROCEEDING("P_"),
  OPPONENT("OPPONENT_");

  private final String prefix;

  /**
   * Initializes the enum with the specified prefix string.
   *
   * @param prefix the string representing the prefix for instance mapping
   */
  InstanceMappingPrefix(final String prefix) {
    this.prefix = prefix;
  }
}
