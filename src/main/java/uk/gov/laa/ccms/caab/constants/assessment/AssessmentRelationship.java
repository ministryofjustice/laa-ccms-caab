package uk.gov.laa.ccms.caab.constants.assessment;

import java.util.Arrays;
import lombok.Getter;

/**
 * Defines the relationship associated with an assessment entity.
 */
@Getter
public enum AssessmentRelationship {
  OPPONENT("opponentotherparties"),
  PROCEEDING("proceeding");

  private final String relationship;

  /**
   * Initializes the enum with the specified relationship string.
   *
   * @param relationship the string representation of the assessment entities relationship
   */
  AssessmentRelationship(final String relationship) {
    this.relationship = relationship;
  }

}

