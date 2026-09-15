package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;

class AwardTypeValidatorTest {

  private AwardTypeValidator awardTypeValidator;
  private AwardTypeForm awardTypeForm;
  private Errors errors;

  @BeforeEach
  void setUp() {
    awardTypeValidator = new AwardTypeValidator();
    awardTypeForm = new AwardTypeForm();
    errors = new BeanPropertyBindingResult(awardTypeForm, "awardTypeForm");
  }

  @Test
  void supportsAwardTypeForm() {
    assertTrue(awardTypeValidator.supports(AwardTypeForm.class));
  }

  @Test
  void doesNotSupportUnrelatedClass() {
    assertFalse(awardTypeValidator.supports(Object.class));
  }

  @Test
  void rejectsMissingAwardTypeCode() {
    awardTypeValidator.validate(awardTypeForm, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("awardTypeCode"));
    assertEquals("required.awardTypeCode", errors.getFieldError("awardTypeCode").getCode());
  }

  @Test
  void acceptsValidAwardTypeCode() {
    awardTypeForm.setAwardTypeCode("FIN_ASSET");

    awardTypeValidator.validate(awardTypeForm, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  void rejectsBlankAwardTypeCode() {
    awardTypeForm.setAwardTypeCode("   ");

    awardTypeValidator.validate(awardTypeForm, errors);

    assertTrue(errors.hasFieldErrors("awardTypeCode"));
  }
}
