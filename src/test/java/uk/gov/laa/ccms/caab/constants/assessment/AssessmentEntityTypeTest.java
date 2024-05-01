package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssessmentEntityTypeTest {

  @Test
  void testProceedingType() {
    assertEquals("PROCEEDING", AssessmentEntityType.PROCEEDING.getType());
  }

  @Test
  void testOpponentOtherPartiesType() {
    assertEquals("OPPONENT_OTHER_PARTIES", AssessmentEntityType.OPPONENT.getType());
  }
}
