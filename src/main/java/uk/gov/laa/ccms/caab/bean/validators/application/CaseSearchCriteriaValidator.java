package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link CaseSearchCriteria} objects.
 */
@Component
public class CaseSearchCriteriaValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link CaseSearchCriteria},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return CaseSearchCriteria.class.isAssignableFrom(clazz);
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
   * Validates that at least one search criteria is provided in the {@link CaseSearchCriteria}.
   *
   * @param target The target object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateAtLeastOneSearchCriteria(Object target, Errors errors) {
    CaseSearchCriteria searchCriteria = (CaseSearchCriteria) target;

    if (!StringUtils.hasText(searchCriteria.getCaseReference())
            && !StringUtils.hasText(searchCriteria.getClientSurname())
            && !StringUtils.hasText(searchCriteria.getProviderCaseReference())
            && searchCriteria.getFeeEarnerId() == null
            && searchCriteria.getOfficeId() == null
            && !StringUtils.hasText(searchCriteria.getStatus())) {
      errors.rejectValue(null, "required.atLeastOneSearchCriteria",
              "You must provide at least one search criteria below. Please amend your entry.");
    }

  }
}
