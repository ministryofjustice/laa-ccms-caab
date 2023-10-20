package uk.gov.laa.ccms.caab.bean.validators.application;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link CopyCaseSearchCriteria} objects.
 */
@Component
public class CopyCaseSearchCriteriaValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link CopyCaseSearchCriteria},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return CopyCaseSearchCriteria.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the provided target object.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    validateAtLeastOneSearchCriteria(target, errors);
  }

  /**
   * Validates that at least one search criteria is provided in the {@link CopyCaseSearchCriteria}.
   *
   * @param target The target object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateAtLeastOneSearchCriteria(Object target, Errors errors) {
    CopyCaseSearchCriteria searchCriteria = (CopyCaseSearchCriteria) target;

    if (StringUtils.isBlank(searchCriteria.getCaseReference())
            && StringUtils.isBlank(searchCriteria.getClientSurname())
            && StringUtils.isBlank(searchCriteria.getProviderCaseReference())
            && searchCriteria.getFeeEarnerId() == null
            && searchCriteria.getOfficeId() == null
    ) {
      errors.rejectValue(null, "required.atLeastOneSearchCriteria",
              "You must provide at least one search criteria below. Please amend your entry.");
    }

  }
}
