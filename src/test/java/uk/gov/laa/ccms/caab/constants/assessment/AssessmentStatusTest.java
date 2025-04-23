package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssessmentStatusTest {

  @Test
  void completeStatus() {
    assertEquals("COMPLETE", AssessmentStatus.COMPLETE.getStatus());
  }

  @Test
  void errorStatus() {
    assertEquals("ERROR", AssessmentStatus.ERROR.getStatus());
  }

  @Test
  void notStartedStatus() {
    assertEquals("NOT_STARTED", AssessmentStatus.NOT_STARTED.getStatus());
  }

  @Test
  void requiredStatus() {
    assertEquals("REQUIRED", AssessmentStatus.REQUIRED.getStatus());
  }

  @Test
  void unchangedStatus() {
    assertEquals("UNCHANGED", AssessmentStatus.UNCHANGED.getStatus());
  }
}
