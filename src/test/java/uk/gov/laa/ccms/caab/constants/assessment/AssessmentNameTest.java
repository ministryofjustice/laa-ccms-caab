package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssessmentNameTest {

  @Test
  void meansAssessmentName() {
    assertEquals("meansAssessment", AssessmentName.MEANS.getName());
  }

  @Test
  void meritsAssessmentName() {
    assertEquals("meritsAssessment", AssessmentName.MERITS.getName());
  }
}
