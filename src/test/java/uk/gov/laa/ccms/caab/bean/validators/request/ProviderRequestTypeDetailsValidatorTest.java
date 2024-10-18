package uk.gov.laa.ccms.caab.bean.validators.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;

@ExtendWith(SpringExtension.class)
class ProviderRequestTypeDetailsValidatorTest {

  @InjectMocks
  private ProviderRequestTypeDetailsValidator providerRequestTypeDetailsValidator;

  private ProviderRequestTypeFormData providerRequestTypeFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    providerRequestTypeFormData = new ProviderRequestTypeFormData();
    errors = new BeanPropertyBindingResult(providerRequestTypeFormData, "providerRequestTypeFormData");
  }

  @Test
  public void supports_ReturnsTrueForProviderRequestTypeFormDataClass() {
    assertTrue(providerRequestTypeDetailsValidator.supports(ProviderRequestTypeFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(providerRequestTypeDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullProviderRequestType_HasErrors() {
    providerRequestTypeFormData.setProviderRequestType(null);
    providerRequestTypeDetailsValidator.validate(providerRequestTypeFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("providerRequestType"));
    assertEquals("Please complete 'Request type'.", errors.getFieldError("providerRequestType").getDefaultMessage());
  }

  @Test
  public void validate_WithValidProviderRequestType_NoErrors() {
    providerRequestTypeFormData.setProviderRequestType("Valid Provider Request Type");
    providerRequestTypeDetailsValidator.validate(providerRequestTypeFormData, errors);
    assertFalse(errors.hasErrors());
  }
}
