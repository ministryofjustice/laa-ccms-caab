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
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;

@ExtendWith(SpringExtension.class)
class AddressSearchValidatorTest {

  @InjectMocks
  private AddressSearchValidator addressSearchValidator;

  private AddressSearchFormData addressSearch;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressSearch =
        new AddressSearchFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(addressSearch, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(addressSearchValidator.supports(AddressSearchFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(addressSearchValidator.supports(Object.class));
  }

  @Test
  public void validate_errors() {
    addressSearchValidator.validate(addressSearch, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.uprn", errors.getAllErrors().getFirst().getCode());
  }

  @Test
  public void validate() {
    addressSearch.setUprn("12345");
    addressSearchValidator.validate(addressSearch, errors);
    assertFalse(errors.hasErrors());
  }

}
