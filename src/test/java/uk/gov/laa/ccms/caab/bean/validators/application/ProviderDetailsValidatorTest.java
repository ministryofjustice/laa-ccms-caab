package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;

@ExtendWith(SpringExtension.class)
class ProviderDetailsValidatorTest {

  @InjectMocks
  private ProviderDetailsValidator providerDetailsValidator;

  private ApplicationFormData applicationFormData;
  private Errors errors;

  @BeforeEach
  public void setUp() {
    applicationFormData = new ApplicationFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationFormData, "APPLICATION_FORM_DATA");
  }

  @Test
  public void supports_ReturnsTrueForApplicationFormDataClass() {
    assertTrue(providerDetailsValidator.supports(ApplicationFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(providerDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithValidContactNameId_NoErrors() {
    applicationFormData.setContactNameId("John Doe");
    providerDetailsValidator.validate(applicationFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_WithNullContactNameId_HasErrors() {
    applicationFormData.setContactNameId(null);
    providerDetailsValidator.validate(applicationFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("contactNameId"));
    assertEquals("required.contactNameId", errors.getFieldError("contactNameId").getCode());
  }

  @Test
  public void validate_WithEmptyContactNameId_HasErrors() {
    applicationFormData.setContactNameId("");
    providerDetailsValidator.validate(applicationFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("contactNameId"));
    assertEquals("required.contactNameId", errors.getFieldError("contactNameId").getCode());
  }

}