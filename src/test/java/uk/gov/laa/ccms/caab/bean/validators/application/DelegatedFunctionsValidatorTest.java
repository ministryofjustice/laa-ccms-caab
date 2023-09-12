package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;

@ExtendWith(SpringExtension.class)
class DelegatedFunctionsValidatorTest {

  @InjectMocks
  private DelegatedFunctionsValidator delegatedFunctionsValidator;

  private ApplicationDetails applicationDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    applicationDetails =
        new ApplicationDetails(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationDetails, "applicationDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(delegatedFunctionsValidator.supports(ApplicationDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(delegatedFunctionsValidator.supports(Object.class));
  }

  @Test
  public void validate_withoutDelegatedFunctions() {
    delegatedFunctionsValidator.validate(applicationDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_withDelegatedFunctions() {
    applicationDetails.setDelegatedFunctions(true);

    delegatedFunctionsValidator.validate(applicationDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("delegatedFunctionUsedDay"));
    assertEquals("invalid.numeric", errors.getFieldError("delegatedFunctionUsedDay").getCode());
    assertNotNull(errors.getFieldError("delegatedFunctionUsedMonth"));
    assertEquals("invalid.numeric", errors.getFieldError("delegatedFunctionUsedMonth").getCode());
    assertNotNull(errors.getFieldError("delegatedFunctionUsedYear"));
    assertEquals("invalid.numeric", errors.getFieldError("delegatedFunctionUsedYear").getCode());
  }

}