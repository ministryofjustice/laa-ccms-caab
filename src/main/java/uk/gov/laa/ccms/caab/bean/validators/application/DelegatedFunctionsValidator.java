package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.springframework.util.StringUtils.hasText;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
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
      ValidationUtils.rejectIfEmpty(errors, "delegatedFunctionUsedDate",
          "required.delegatedFunctionUsedDate",
          "Please complete when the delegated function was used?");

      if (hasText(applicationFormData.getDelegatedFunctionUsedDate())) {
        validateValidDateField(applicationFormData.getDelegatedFunctionUsedDate(),
            "delegatedFunctionUsedDate", "when the delegated function was used?",
            COMPONENT_DATE_PATTERN,
            errors);
      }
    }
  }
}
