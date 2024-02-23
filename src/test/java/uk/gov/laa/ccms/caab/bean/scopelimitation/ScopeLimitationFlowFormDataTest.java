package uk.gov.laa.ccms.caab.bean.scopelimitation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopeLimitationFlowFormDataTest {

  private ScopeLimitationFlowFormData scopeLimitationFlowFormData;

  @BeforeEach
  public void setUp() {
    scopeLimitationFlowFormData = new ScopeLimitationFlowFormData("action");
  }

  @Test
  public void testScopeLimitationDetails() {
    assertNotNull(scopeLimitationFlowFormData.getScopeLimitationDetails());
    final ScopeLimitationFormDataDetails newDetails = new ScopeLimitationFormDataDetails();
    scopeLimitationFlowFormData.setScopeLimitationDetails(newDetails);
    assertEquals(newDetails, scopeLimitationFlowFormData.getScopeLimitationDetails());
  }

}