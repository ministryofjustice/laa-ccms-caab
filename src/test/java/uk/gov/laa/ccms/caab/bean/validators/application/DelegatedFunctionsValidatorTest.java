package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;

@ExtendWith(MockitoExtension.class)
class DelegatedFunctionsValidatorTest {

  @InjectMocks private DelegatedFunctionsValidator delegatedFunctionsValidator;

  private ApplicationFormData applicationFormData;

  private Errors errors;

  @BeforeEach
  void setUp() {
    applicationFormData =
        new ApplicationFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationFormData, APPLICATION_FORM_DATA);
  }

  @Test
  void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(delegatedFunctionsValidator.supports(ApplicationFormData.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(delegatedFunctionsValidator.supports(Object.class));
  }

  @Test
  void validate_withoutDelegatedFunctions() {
    delegatedFunctionsValidator.validate(applicationFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_withDelegatedFunctions() {
    applicationFormData.setDelegatedFunctions(true);

    delegatedFunctionsValidator.validate(applicationFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("delegatedFunctionUsedDate"));
    assertEquals(
        "required.delegatedFunctionUsedDate",
        errors.getFieldError("delegatedFunctionUsedDate").getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "abc123", "1a/02/2024", "01/b2/2024", "01/02/20c4", "01/@2/2024"})
  void validate_rejectsInvalidCharactersInDelegatedFunctionDate(String date) {
    applicationFormData.setDelegatedFunctions(true);
    applicationFormData.setDelegatedFunctionUsedDate(date);

    delegatedFunctionsValidator.validate(applicationFormData, errors);

    assertEquals(1, errors.getFieldErrorCount("delegatedFunctionUsedDate"));
    assertEquals("invalid.format", errors.getFieldError("delegatedFunctionUsedDate").getCode());
    assertNotNull(errors.getFieldError("delegatedFunctionUsedDate").getDefaultMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"4/3/2024", "04/03/2024"})
  void validate_acceptsSupportedDelegatedFunctionDateFormats(String date) {
    applicationFormData.setDelegatedFunctions(true);
    applicationFormData.setDelegatedFunctionUsedDate(date);

    delegatedFunctionsValidator.validate(applicationFormData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_ignoresMalformedDateWhenDelegatedFunctionsNotUsed() {
    applicationFormData.setDelegatedFunctions(false);
    applicationFormData.setDelegatedFunctionUsedDate("abc123");

    delegatedFunctionsValidator.validate(applicationFormData, errors);

    assertFalse(errors.hasErrors());
  }
}
