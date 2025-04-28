package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssessmentAttributeTest {

  @Test
  void matterTypeAttribute() {
    assertEquals("MATTER_TYPE", AssessmentAttribute.MATTER_TYPE.name());
  }

  @Test
  void proceedingNameAttribute() {
    assertEquals("PROCEEDING_NAME", AssessmentAttribute.PROCEEDING_NAME.name());
  }

  @Test
  void clientInvolvementTypeAttribute() {
    assertEquals("CLIENT_INVOLVEMENT_TYPE", AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE.name());
  }

  @Test
  void requestedScopeAttribute() {
    assertEquals("REQUESTED_SCOPE", AssessmentAttribute.REQUESTED_SCOPE.name());
  }
}
