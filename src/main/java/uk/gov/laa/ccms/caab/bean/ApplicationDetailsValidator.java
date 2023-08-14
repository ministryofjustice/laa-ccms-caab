package uk.gov.laa.ccms.caab.bean;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Validates the application details provided by the user.
 */
@Component
public class ApplicationDetailsValidator implements Validator {

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
   * Validate the selected office in the application details.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  public void validateSelectOffice(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "officeId",
            "required.officeId", "Please select an office.");
  }

  /**
   * Validate the selected category of law in the application details.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  public void validateCategoryOfLaw(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "categoryOfLawId",
            "required.categoryOfLawId", "Please select a category of law.");
  }

  /**
   * Validate the selected application type category in the application details.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  public void validateApplicationTypeCategory(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "applicationTypeCategory",
            "required.applicationTypeCategory", "Please select an application type.");
  }

  /**
   * Validate the delegated functions information if applicable.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  public void validateDelegatedFunction(Object target, Errors errors) {
    ApplicationDetails applicationDetails = (ApplicationDetails) target;

    if (applicationDetails.isDelegatedFunctions()) {
      String delegatedFunctionUsedDay = applicationDetails.getDelegatedFunctionUsedDay();
      String delegatedFunctionUsedMonth = applicationDetails.getDelegatedFunctionUsedMonth();
      String delegatedFunctionUsedYear = applicationDetails.getDelegatedFunctionUsedYear();

      if (!delegatedFunctionUsedDay.matches(NUMERIC_PATTERN)) {
        errors.rejectValue("delegatedFunctionUsedDay", "invalid.numeric",
                "Please enter a numeric value for the day.");
      }

      if (!delegatedFunctionUsedMonth.matches(NUMERIC_PATTERN)) {
        errors.rejectValue("delegatedFunctionUsedMonth", "invalid.numeric",
                "Please enter a numeric value for the month.");
      }

      if (!delegatedFunctionUsedYear.matches(NUMERIC_PATTERN)) {
        errors.rejectValue("delegatedFunctionUsedYear", "invalid.numeric",
                "Please enter a numeric value for the year.");
      }
    }
  }

  /**
   * Validate the agreement acceptance in the application details.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  public void validateAgreementAcceptance(Object target, Errors errors) {
    ApplicationDetails applicationDetails = (ApplicationDetails) target;

    if (!applicationDetails.isAgreementAccepted()) {
      errors.rejectValue("agreementAccepted",
              "agreement.not.accepted",
              "Please complete 'I confirm my client (or their representative) has read and "
                      + "agreed to the Privacy Notice'.");
    }
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(Object target, Errors errors) {
    validateSelectOffice(target, errors);
    validateCategoryOfLaw(target, errors);
    validateApplicationTypeCategory(target, errors);
  }
}
