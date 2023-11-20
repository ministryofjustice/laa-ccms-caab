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
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch;

@ExtendWith(SpringExtension.class)
class ClientAddressSearchValidatorTest {

  @InjectMocks
  private ClientAddressSearchValidator clientAddressSearchValidator;

  private ClientFormDataAddressSearch addressSearch;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressSearch =
        new ClientFormDataAddressSearch(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(addressSearch, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientAddressSearchValidator.supports(ClientFormDataAddressSearch.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressSearchValidator.supports(Object.class));
  }

  @Test
  public void validate_errors() {
    clientAddressSearchValidator.validate(addressSearch, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.uprn",errors.getAllErrors().get(0).getCode());
  }

  @Test
  public void validate() {
    addressSearch.setUprn("12345");
    clientAddressSearchValidator.validate(addressSearch, errors);
    assertFalse(errors.hasErrors());
  }

}