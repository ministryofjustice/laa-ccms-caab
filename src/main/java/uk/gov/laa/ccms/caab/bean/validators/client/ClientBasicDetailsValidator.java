package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.FIRST_CHARACTER_MUST_BE_ALPHA;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.HOME_OFFICE_NUMBER_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NATIONAL_INSURANCE_NUMBER_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link
 * uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails} objects.
 */
@Component
public class ClientBasicDetailsValidator extends AbstractValidator {
  private static final String HOME_OFFICE_NUMBER_ERROR =
      "Your input for 'Home office number' is in an incorrect format. "
          + "Please amend your entry.";

  private static final String NATIONAL_INSURANCE_NUMBER_ERROR =
      " Your input for 'National insurance number' is not in the correct format. "
          + "The format for 'National insurance number' is AANNNNNNA, where A is "
          + "a letter and N is a number. Please amend your entry.";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link ClientFormDataBasicDetails}, {@code
   *     false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataBasicDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client basic details in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;

    validateRequiredField("title", basicDetails.getTitle(), "Title", errors);
    validateRequiredField("surname", basicDetails.getSurname(), "Surname", errors);
    validateRequiredField(
        "countryOfOrigin", basicDetails.getCountryOfOrigin(), "Country of origin", errors);
    validateRequiredField("gender", basicDetails.getGender(), "Gender", errors);
    validateRequiredField(
        "maritalStatus", basicDetails.getMaritalStatus(), "Marital status", errors);

    validateMiddleNames(target, errors);
    validateSurname(target, errors);
    validateNationalInsuranceNumber(target, errors);
    validateHomeOfficeNumber(target, errors);
  }

  /**
   * Validates the middleNames in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateMiddleNames(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;

    String middleNames = basicDetails.getMiddleNames();
    if (StringUtils.hasText(middleNames)) {
      if (!middleNames.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue(
            "middleNames",
            "invalid.middleNames",
            "Your input for 'Middle name(s)' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!middleNames.matches(CHARACTER_SET_C)) {
        errors.rejectValue(
            "middleNames",
            "invalid.middleNames-char",
            "Your input for 'Middle name(s)' contains an invalid character. "
                + "Please amend your entry.");
      } else if (patternMatches(middleNames, DOUBLE_SPACE)) {
        errors.rejectValue(
            "middleNames",
            "invalid.middleNames",
            "Your input for 'Middle name(s)'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the surname in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateSurname(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;

    String surname = basicDetails.getSurname();
    if (StringUtils.hasText(surname)) {
      if (!surname.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue(
            "surname",
            "invalid.surname",
            "Your input for 'Surname' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!surname.matches(CHARACTER_SET_C)) {
        errors.rejectValue(
            "surname",
            "invalid.surname-char",
            "Your input for 'Surname' contains an invalid character. "
                + "Please amend your entry.");
      } else if (patternMatches(surname, DOUBLE_SPACE)) {
        errors.rejectValue(
            "surname",
            "invalid.surname",
            "Your input for 'Surname'" + " contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the nationalInsuranceNumber in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateNationalInsuranceNumber(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;
    String nationalInsuranceNumber = basicDetails.getNationalInsuranceNumber();

    if (StringUtils.hasText(nationalInsuranceNumber)) {
      if (!nationalInsuranceNumber.matches(NATIONAL_INSURANCE_NUMBER_PATTERN)) {
        errors.rejectValue(
            "nationalInsuranceNumber",
            "invalid.nationalInsuranceNumber",
            NATIONAL_INSURANCE_NUMBER_ERROR);
      }
    }
  }

  /**
   * Validates the homeOfficeNumber in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateHomeOfficeNumber(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;
    String homeOfficeNumber = basicDetails.getHomeOfficeNumber();

    if (StringUtils.hasText(homeOfficeNumber)) {
      if (!homeOfficeNumber.matches(HOME_OFFICE_NUMBER_PATTERN)) {
        errors.rejectValue(
            "homeOfficeNumber", "invalid.homeOfficeNumber", HOME_OFFICE_NUMBER_ERROR);
      }
    }
  }
}
