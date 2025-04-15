package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SPACES_COMMAS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
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

    validateCity(searchCriteria.getCity(), errors);
    validateName(searchCriteria.getName(), errors);
  }

  /**
   * Validates the city in the {@link OrganisationSearchCriteria}.
   *
   * @param city The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateCity(final String city, Errors errors) {
    if (StringUtils.hasText(city)) {
      //check no double spaces
      if (!city.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue("city", "invalid.city",
            "Your input for 'City' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (city.matches(DOUBLE_SPACE)) {
        errors.rejectValue("city", "invalid.city",
            String.format(GENERIC_DOUBLE_SPACES, "City"));
      }
    }
  }

  /**
   * Validates the city in the {@link OrganisationSearchCriteria}.
   *
   * @param name The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateName(final String name, Errors errors) {
    if (StringUtils.hasText(name)) {
      //check no double spaces
      if (!name.matches(CHARACTER_SET_A)) {
        errors.rejectValue("name", "invalid.name",
            String.format(GENERIC_INCORRECT_FORMAT, "Organisation name"));
      } else if (name.matches(DOUBLE_SPACE)) {
        errors.rejectValue("name", "invalid.name",
            String.format(GENERIC_DOUBLE_SPACES, "Organisation name"));
      }
    }
  }

}
