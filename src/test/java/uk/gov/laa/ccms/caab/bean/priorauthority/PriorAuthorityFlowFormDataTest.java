package uk.gov.laa.ccms.caab.bean.priorauthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PriorAuthorityFlowFormDataTest {

  @Test
  void constructor() {
    final String expectedAction = "create";
    final PriorAuthorityFlowFormData formData = new PriorAuthorityFlowFormData(expectedAction);

    assertEquals(expectedAction, formData.getAction());
    assertNotNull(formData.getPriorAuthorityTypeFormData());
    assertNotNull(formData.getPriorAuthorityDetailsFormData());
  }
}
