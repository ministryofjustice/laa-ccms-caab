package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;

@ExtendWith(SpringExtension.class)
class OfficeValidatorTest {

  @InjectMocks
  private OfficeValidator officeValidator;

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
    assertTrue(officeValidator.supports(ApplicationFormData.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(officeValidator.supports(Object.class));
  }

  @Test
  void validate() {
    officeValidator.validate(applicationFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("officeId"));
    assertEquals("required.officeId", errors.getFieldError("officeId").getCode());
  }

}