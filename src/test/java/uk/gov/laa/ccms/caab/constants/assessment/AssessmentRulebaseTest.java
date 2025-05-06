package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssessmentRulebaseTest {

  @Test
  void testFindByType() {
    assertEquals(AssessmentRulebase.MEANS, AssessmentRulebase.findByType("MEANS"));
    assertEquals(AssessmentRulebase.MERITS, AssessmentRulebase.findByType("MERITS"));
    assertEquals(AssessmentRulebase.BILLING, AssessmentRulebase.findByType("BILLING"));
    assertEquals(AssessmentRulebase.POA, AssessmentRulebase.findByType("POA"));
  }

  @Test
  void testGetPrePopAssessmentName() {
    assertEquals("meansAssessment_PREPOP", AssessmentRulebase.MEANS.getPrePopAssessmentName());
    assertEquals("meritsAssessment_PREPOP", AssessmentRulebase.MERITS.getPrePopAssessmentName());
    assertEquals("billingAssessment_PREPOP", AssessmentRulebase.BILLING.getPrePopAssessmentName());
    assertEquals("poaAssessment_PREPOP", AssessmentRulebase.POA.getPrePopAssessmentName());
  }

  @Test
  void testGetPrePopAssessmentNameById() {
    assertEquals("meansAssessment_PREPOP", AssessmentRulebase.getPrePopAssessmentName(1L));
    assertEquals("meritsAssessment_PREPOP", AssessmentRulebase.getPrePopAssessmentName(2L));
    assertEquals("billingAssessment_PREPOP", AssessmentRulebase.getPrePopAssessmentName(3L));
    assertEquals("poaAssessment_PREPOP", AssessmentRulebase.getPrePopAssessmentName(4L));
  }

  @Test
  void testFindById() {
    assertEquals(AssessmentRulebase.MEANS, AssessmentRulebase.findById(1L));
    assertEquals(AssessmentRulebase.MERITS, AssessmentRulebase.findById(2L));
    assertEquals(AssessmentRulebase.BILLING, AssessmentRulebase.findById(3L));
    assertEquals(AssessmentRulebase.POA, AssessmentRulebase.findById(4L));
    assertNull(AssessmentRulebase.findById(999L));
  }

  @Test
  void testGetDeploymentName() {
    assertEquals("MeansAssessment", AssessmentRulebase.MEANS.getDeploymentName());
    assertEquals("MeritsAssessment", AssessmentRulebase.MERITS.getDeploymentName());
    assertEquals("BillingAssessment", AssessmentRulebase.BILLING.getDeploymentName());
    assertEquals("PoaAssessment", AssessmentRulebase.POA.getDeploymentName());
  }

}
