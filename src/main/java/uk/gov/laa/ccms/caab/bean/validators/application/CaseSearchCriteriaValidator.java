package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.*;

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
    CaseSearchCriteria searchCriteria = (CaseSearchCriteria) target;
    validateAtLeastOneSearchCriteria(target, errors);

    if (!errors.hasErrors()) {
      validateCaseRef(searchCriteria.getCaseReference(), errors);
      validateClientSurname(searchCriteria.getClientSurname(), errors);
      validateProviderCaseRef(searchCriteria.getProviderCaseReference(), errors);
    }
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

  private void validateCaseRef(final String caseRef, Errors errors) {
    if (StringUtils.hasText(caseRef)) {
      //check no double spaces
      if (!caseRef.matches(ALPHA_NUMERIC_SLASH_SPACE_STRING)) {
        errors.rejectValue("caseReference", "invalid.case-ref",
                "Your input for 'LAA application / case reference' contains an "
                        + "invalid character. Please amend your entry using numbers, "
                        + "letters and spaces only");
      } else if (caseRef.matches(DOUBLE_SPACE)) {
        errors.rejectValue("caseReference", "invalid.case-ref",
                "Your input for 'LAA application / case reference'"
                        + " contains double spaces. Please amend your entry.");
      }
    }
  }

  private void validateClientSurname(String clientSurname, Errors errors) {
    if (StringUtils.hasText(clientSurname)) {
      if (!clientSurname.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue("clientSurname", "invalid.surname",
                "Your input for 'Client surname' is invalid. "
                        + "The first character must be a letter. Please amend your entry.");
      } else if (!clientSurname.matches(CHARACTER_SET_C)) {
        errors.rejectValue("clientSurname", "invalid.surname-char",
                "Your input for 'Client surname' contains an invalid character. "
                        + "Please amend your entry.");
      } else if (patternMatches(clientSurname, DOUBLE_SPACE)) {
        errors.rejectValue("clientSurname", "invalid.surname",
                "Your input for 'Client surname'"
                        + " contains double spaces. Please amend your entry.");
      }
    }
  }

  private void validateProviderCaseRef(String providerCaseReference, Errors errors) {
    if (StringUtils.hasText(providerCaseReference)) {
      //check no double spaces
      if (providerCaseReference.matches(DOUBLE_SPACE)) {
        errors.rejectValue("providerCaseReference", "invalid.providerCaseReference-char",
                "Your input for 'Provider case reference'"
                        + " contains double spaces. Please amend your entry.");
      }
    }
  }
}
