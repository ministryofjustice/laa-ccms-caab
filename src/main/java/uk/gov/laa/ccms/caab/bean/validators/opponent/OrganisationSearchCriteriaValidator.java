package uk.gov.laa.ccms.caab.bean.validators.opponent;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link OrganisationSearchCriteria} objects.
 */
@Component
public class OrganisationSearchCriteriaValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link CaseSearchCriteria},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return OrganisationSearchCriteria.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the provided target object.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    OrganisationSearchCriteria searchCriteria = (OrganisationSearchCriteria) target;
    validateRequiredField("name", searchCriteria.getName(),
        "Organisation name", errors);
  }

}
