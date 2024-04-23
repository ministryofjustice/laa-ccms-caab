package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssessmentStatusTest {

  @Test
  void testCompleteStatus() {
    assertEquals("COMPLETE", AssessmentStatus.COMPLETE.getStatus());
  }

  @Test
  void testErrorStatus() {
    assertEquals("ERROR", AssessmentStatus.ERROR.getStatus());
  }

  @Test
  void testNotStartedStatus() {
    assertEquals("NOT_STARTED", AssessmentStatus.NOT_STARTED.getStatus());
  }

  @Test
  void testRequiredStatus() {
    assertEquals("REQUIRED", AssessmentStatus.REQUIRED.getStatus());
  }

  @Test
  void testUnchangedStatus() {
    assertEquals("UNCHANGED", AssessmentStatus.UNCHANGED.getStatus());
  }
}
