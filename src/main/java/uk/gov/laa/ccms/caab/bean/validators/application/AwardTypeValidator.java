package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the selected award type. */
@Component
public class AwardTypeValidator extends AbstractValidator {

  @Override
  public boolean supports(Class<?> clazz) {
    return AwardTypeForm.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AwardTypeForm awardTypeForm = (AwardTypeForm) target;

    validateRequiredField("awardTypeCode", awardTypeForm.getAwardTypeCode(), "Award type", errors);
  }
}
