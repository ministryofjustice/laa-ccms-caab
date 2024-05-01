package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssessmentAttributeTest {

  @Test
  void testMatterTypeAttribute() {
    assertEquals("MATTER_TYPE", AssessmentAttribute.MATTER_TYPE.getAttribute());
  }

  @Test
  void testProceedingNameAttribute() {
    assertEquals("PROCEEDING_NAME", AssessmentAttribute.PROCEEDING_NAME.getAttribute());
  }

  @Test
  void testClientInvolvementTypeAttribute() {
    assertEquals("CLIENT_INVOLVEMENT_TYPE", AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE.getAttribute());
  }

  @Test
  void testRequestedScopeAttribute() {
    assertEquals("REQUESTED_SCOPE", AssessmentAttribute.REQUESTED_SCOPE.getAttribute());
  }
}
