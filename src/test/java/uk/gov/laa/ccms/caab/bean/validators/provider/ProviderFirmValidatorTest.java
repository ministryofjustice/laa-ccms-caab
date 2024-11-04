package uk.gov.laa.ccms.caab.bean.validators.provider;

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
import uk.gov.laa.ccms.caab.bean.provider.ProviderFirmFormData;

@ExtendWith(SpringExtension.class)
public class ProviderFirmValidatorTest {

  @InjectMocks
  private ProviderFirmValidator validator;

  private ProviderFirmFormData providerFirmFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    providerFirmFormData = new ProviderFirmFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(providerFirmFormData, "providerFirmFormData");
  }

  @Test
  public void supports_ReturnsTrueForSupportedClass() {
    assertTrue(validator.supports(ProviderFirmFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_validatesNoProviderFirmSelected() {
    validator.validate(providerFirmFormData, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getFieldErrors().size());
    assertEquals("required.providerFirmId", errors.getFieldErrors().get(0).getCode());
  }

  @Test
  public void validate_noErrors() {
    providerFirmFormData.setProviderFirmId(12345);
    validator.validate(providerFirmFormData, errors);
    assertFalse(errors.hasErrors());
  }

}
