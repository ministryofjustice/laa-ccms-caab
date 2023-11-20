package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;

@ExtendWith(SpringExtension.class)
class ClientEqualOpportunitiesMonitoringDetailsValidatorTest {

  @InjectMocks
  private ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  private ClientFormDataMonitoringDetails monitoringDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    monitoringDetails = buildClientDetails();
    errors = new BeanPropertyBindingResult(monitoringDetails, "monitoringDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(validator.supports(ClientFormDataMonitoringDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate() {
    validator.validate(monitoringDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_ethnicOrigin(String ethnicity) {
    monitoringDetails.setEthnicOrigin(ethnicity);
    validator.validate(monitoringDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.ethnicOrigin",errors.getAllErrors().get(0).getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_disability(String disability) {
    monitoringDetails.setDisability(disability);
    validator.validate(monitoringDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.disability",errors.getAllErrors().get(0).getCode());
  }

  private ClientFormDataMonitoringDetails buildClientDetails(){
    ClientFormDataMonitoringDetails monitoringDetails1 = new ClientFormDataMonitoringDetails();
    monitoringDetails1.setEthnicOrigin("TEST");
    monitoringDetails1.setDisability("TEST");
    return monitoringDetails1;
  }

}