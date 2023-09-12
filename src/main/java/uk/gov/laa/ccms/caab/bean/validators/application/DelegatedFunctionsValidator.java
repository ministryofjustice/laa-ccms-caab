package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
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
    return ApplicationDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(Object target, Errors errors) {
    ApplicationDetails applicationDetails = (ApplicationDetails) target;

    if (applicationDetails.isDelegatedFunctions()) {
      validateNumericField("delegatedFunctionUsedDay",
          applicationDetails.getDelegatedFunctionUsedDay(), "the day", errors);

      validateNumericField("delegatedFunctionUsedMonth",
          applicationDetails.getDelegatedFunctionUsedMonth(), "the month", errors);

      validateNumericField("delegatedFunctionUsedYear",
          applicationDetails.getDelegatedFunctionUsedYear(), "the year", errors);
    }
  }
}
