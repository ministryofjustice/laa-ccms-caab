package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the application details provided by the user.
 */
@Component
public class DelegatedFunctionsValidator extends AbstractValidator {

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ApplicationFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(Object target, Errors errors) {
    ApplicationFormData applicationFormData = (ApplicationFormData) target;

    if (applicationFormData.isDelegatedFunctions()) {
      validateNumericField("delegatedFunctionUsedDay",
          applicationFormData.getDelegatedFunctionUsedDay(), "the day", errors);

      validateNumericField("delegatedFunctionUsedMonth",
          applicationFormData.getDelegatedFunctionUsedMonth(), "the month", errors);

      validateNumericField("delegatedFunctionUsedYear",
          applicationFormData.getDelegatedFunctionUsedYear(), "the year", errors);
    }
  }
}
