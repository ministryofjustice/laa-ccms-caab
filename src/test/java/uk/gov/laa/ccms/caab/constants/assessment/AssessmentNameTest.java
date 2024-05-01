package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssessmentNameTest {

  @Test
  void testMeansAssessmentName() {
    assertEquals("meansAssessment", AssessmentName.MEANS.getName());
  }

  @Test
  void testMeritsAssessmentName() {
    assertEquals("meritsAssessment", AssessmentName.MERITS.getName());
  }
}
