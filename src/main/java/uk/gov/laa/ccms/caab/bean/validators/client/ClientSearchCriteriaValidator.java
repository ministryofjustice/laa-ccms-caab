package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CASE_REFERENCE_NUMBER_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.FIRST_CHARACTER_MUST_BE_ALPHA;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.HOME_OFFICE_NUMBER_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NATIONAL_INSURANCE_NUMBER_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator component responsible for validating {@link ClientSearchCriteria} objects. */
@Component
public class ClientSearchCriteriaValidator extends AbstractValidator {

  private static final String GENERIC_UNIQUE_IDENTIFIER_ERROR =
      "Your input for 'Unique Identifier Value' is in an incorrect format. "
          + "Please amend your entry.";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link ClientSearchCriteria}, {@code
   *     false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientSearchCriteria.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the forename in the {@link ClientSearchCriteria}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateForename(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors, "forename", "required.forename", "Please complete 'First name'.");

    ClientSearchCriteria clientSearchCriteria = (ClientSearchCriteria) target;

    String forename = clientSearchCriteria.getForename();
    if (StringUtils.hasText(forename)) {
      if (!forename.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue(
            "forename",
            "invalid.forename",
            "Your input for 'First name' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!forename.matches(CHARACTER_SET_C)) {
        errors.rejectValue(
            "forename",
            "invalid.forename-char",
            "Your input for 'First name' contains an invalid character. Please amend your entry.");
      } else if (patternMatches(forename, DOUBLE_SPACE)) {
        errors.rejectValue(
            "forename",
            "invalid.forename",
            "Your input for 'First name' contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the surname at birth in the {@link ClientSearchCriteria}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateSurnameAtBirth(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors, "surname", "required.surname", "Please complete 'Surname at birth'.");

    ClientSearchCriteria clientSearchCriteria = (ClientSearchCriteria) target;

    String surname = clientSearchCriteria.getSurname();
    if (StringUtils.hasText(surname)) {
      if (!surname.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue(
            "surname",
            "invalid.surname",
            "Your input for 'Surname at birth' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!surname.matches(CHARACTER_SET_C)) {
        errors.rejectValue(
            "surname",
            "invalid.surname-char",
            "Your input for 'Surname at birth' contains an invalid character. "
                + "Please amend your entry.");
      } else if (patternMatches(surname, DOUBLE_SPACE)) {
        errors.rejectValue(
            "surname",
            "invalid.surname",
            "Your input for 'Surname at birth' contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the unique identifier type and value in the {@link ClientSearchCriteria}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateUniqueIdentifierType(Object target, Errors errors) {
    ClientSearchCriteria clientSearchCriteria = (ClientSearchCriteria) target;

    if (clientSearchCriteria.getUniqueIdentifierType() != null) {
      if ((clientSearchCriteria.getUniqueIdentifierType()
              == UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER)
          && (!clientSearchCriteria
              .getUniqueIdentifierValue()
              .matches(NATIONAL_INSURANCE_NUMBER_PATTERN))) {
        errors.rejectValue(
            "uniqueIdentifierValue",
            "invalid.uniqueIdentifierValue",
            "Your input for 'Unique Identifier Value' is not in the correct format. "
                + "The format for 'Unique Identifier Value' is AANNNNNNA, where A is "
                + "a letter and N is a number. Please amend your entry.");

      } else if ((clientSearchCriteria.getUniqueIdentifierType()
              == UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE)
          && (!clientSearchCriteria
              .getUniqueIdentifierValue()
              .matches(HOME_OFFICE_NUMBER_PATTERN))) {
        errors.rejectValue(
            "uniqueIdentifierValue",
            "invalid.uniqueIdentifierValue",
            GENERIC_UNIQUE_IDENTIFIER_ERROR);

      } else if ((clientSearchCriteria.getUniqueIdentifierType()
              == UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER)
          && (!clientSearchCriteria
                  .getUniqueIdentifierValue()
                  .matches(CASE_REFERENCE_NUMBER_PATTERN)
              || clientSearchCriteria
                  .getUniqueIdentifierValue()
                  .matches(CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN))) {
        errors.rejectValue(
            "uniqueIdentifierValue",
            "invalid.uniqueIdentifierValue",
            GENERIC_UNIQUE_IDENTIFIER_ERROR);
      }
    }
  }

  /**
   * Validates the provided target object.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    validateForename(target, errors);
    validateSurnameAtBirth(target, errors);
    validateDateOfBirth(target, errors, true);
    validateUniqueIdentifierType(target, errors);
  }
}
