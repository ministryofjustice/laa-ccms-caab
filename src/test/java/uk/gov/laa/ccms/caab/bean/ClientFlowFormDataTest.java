package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFlowFormDataTest {

  @Test
  void eachNewRegistrationGetsItsOwnServerGeneratedIdentity() {
    ClientFlowFormData first = new ClientFlowFormData("create");
    ClientFlowFormData second = new ClientFlowFormData("create");

    assertEquals(first.getRegistrationId(), UUID.fromString(first.getRegistrationId()).toString());
    assertNotEquals(first.getRegistrationId(), second.getRegistrationId());
    assertEquals(first, second);
  }

  @Test
  void editingFormDataKeepsTheRegistrationIdentity() {
    ClientFlowFormData flow = new ClientFlowFormData("create");
    String registrationId = flow.getRegistrationId();

    flow.setBasicDetails(new ClientFormDataBasicDetails());
    flow.setAddressDetails(new ClientFormDataAddressDetails());
    flow.setAction("edit");

    assertEquals(registrationId, flow.getRegistrationId());
  }

  @Test
  void registrationIdentityCannotBeSetThroughABeanSetter() {
    assertFalse(
        Arrays.stream(ClientFlowFormData.class.getMethods())
            .anyMatch(method -> method.getName().equals("setRegistrationId")));
  }
}
