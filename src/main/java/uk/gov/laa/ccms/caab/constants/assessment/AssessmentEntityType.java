package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Enumerates the types of entities related to an assessment.
 */
@Getter
public enum AssessmentEntityType {
  PROCEEDING("PROCEEDING"),
  OPPONENT("OPPONENT_OTHER_PARTIES"),
  GLOBAL("global");

  private final String type;

  /**
   * Initializes the enum with the specified type string.
   *
   * @param type the string representation of the entity type
   */
  AssessmentEntityType(final String type) {
    this.type = type;
  }

}
