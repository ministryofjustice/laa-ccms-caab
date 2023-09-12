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
class ApplicationTypeValidatorTest {

  @InjectMocks
  private ApplicationTypeValidator applicationTypeValidator;

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
    assertTrue(applicationTypeValidator.supports(ApplicationDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(applicationTypeValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    applicationTypeValidator.validate(applicationDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("applicationTypeCategory"));
    assertEquals("required.applicationTypeCategory", errors.getFieldError("applicationTypeCategory").getCode());
  }

}