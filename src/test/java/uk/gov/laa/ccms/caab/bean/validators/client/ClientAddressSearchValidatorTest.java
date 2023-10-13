package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;

@ExtendWith(SpringExtension.class)
class ClientAddressSearchValidatorTest {

  @InjectMocks
  private ClientAddressSearchValidator clientAddressSearchValidator;

  private ClientDetails clientDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientDetails =
        new ClientDetails(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(clientDetails, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientAddressSearchValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressSearchValidator.supports(Object.class));
  }

  @Test
  public void validate_errors() {
    clientAddressSearchValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.uprn",errors.getAllErrors().get(0).getCode());
  }

  @Test
  public void validate() {
    clientDetails.setUprn("12345");
    clientAddressSearchValidator.validate(clientDetails, errors);
    assertFalse(errors.hasErrors());
  }

}