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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;

@ExtendWith(MockitoExtension.class)
class CategoryOfLawValidatorTest {

  @InjectMocks private CategoryOfLawValidator categoryOfLawValidator;

  private ApplicationFormData applicationFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    applicationFormData =
        new ApplicationFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationFormData, APPLICATION_FORM_DATA);
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(categoryOfLawValidator.supports(ApplicationFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(categoryOfLawValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    categoryOfLawValidator.validate(applicationFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("categoryOfLawId"));
    assertEquals("required.categoryOfLawId", errors.getFieldError("categoryOfLawId").getCode());
  }
}
