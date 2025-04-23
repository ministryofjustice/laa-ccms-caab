package uk.gov.laa.ccms.caab.bean.scopelimitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopeLimitationFlowFormDataTest {

  private ScopeLimitationFlowFormData scopeLimitationFlowFormData;

  @BeforeEach
  void setUp() {
    scopeLimitationFlowFormData = new ScopeLimitationFlowFormData("action");
  }

  @Test
  void scopeLimitationDetails() {
    assertNotNull(scopeLimitationFlowFormData.getScopeLimitationDetails());
    final ScopeLimitationFormDataDetails newDetails = new ScopeLimitationFormDataDetails();
    scopeLimitationFlowFormData.setScopeLimitationDetails(newDetails);
    assertEquals(newDetails, scopeLimitationFlowFormData.getScopeLimitationDetails());
  }

}