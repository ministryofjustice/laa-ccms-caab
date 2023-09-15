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
import uk.gov.laa.ccms.caab.bean.ClientDetails;

@ExtendWith(SpringExtension.class)
class ClientEqualOpportunitiesMonitoringDetailsValidatorTest {

  @InjectMocks
  private ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  private ClientDetails clientDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientDetails = buildClientDetails();
    errors = new BeanPropertyBindingResult(clientDetails, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(validator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate() {
    validator.validate(clientDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_ethnicOrigin(String ethnicity) {
    clientDetails.setEthnicOrigin(ethnicity);
    validator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.ethnicOrigin",errors.getAllErrors().get(0).getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_disability(String disability) {
    clientDetails.setDisability(disability);
    validator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.disability",errors.getAllErrors().get(0).getCode());
  }

  private ClientDetails buildClientDetails(){
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setEthnicOrigin("TEST");
    clientDetails.setDisability("TEST");
    return clientDetails;
  }

}