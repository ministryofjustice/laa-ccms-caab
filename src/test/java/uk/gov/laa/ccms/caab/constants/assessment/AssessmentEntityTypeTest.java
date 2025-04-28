package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssessmentEntityTypeTest {

  @Test
  void proceedingType() {
    assertEquals("PROCEEDING", AssessmentEntityType.PROCEEDING.getType());
  }

  @Test
  void opponentOtherPartiesType() {
    assertEquals("OPPONENT_OTHER_PARTIES", AssessmentEntityType.OPPONENT.getType());
  }
}
