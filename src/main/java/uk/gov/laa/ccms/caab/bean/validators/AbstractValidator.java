package uk.gov.laa.ccms.caab.bean.validators;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.INTERNATIONAL_POSTCODE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UK_POSTCODE;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Abstract validator used for all form validation.
 */
public abstract class AbstractValidator implements Validator {

  private static String GENERIC_REQUIRED_ERROR = "Please complete '%s'.";

  private static String GENERIC_NUMERIC_REQUIRED = "Please enter a numeric value for %s.";

  private static String GENERIC_INCORRECT_FORMAT = "Your input for '%s' is in an incorrect "
      + "format. Please amend your entry.";

  //TODO implement invalid character checks
  private static String GENERIC_INVALID_CHARACTER = "Your input for '%s' contains an invalid "
      + "character. Please amend your entry.";

  protected void validateRequiredField(String field, String displayValue, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, field, "required." + field,
        String.format(GENERIC_REQUIRED_ERROR, displayValue));
  }

  protected void validateNumericField(
      String field, String fieldValue, String displayValue, Errors errors) {
    if (!fieldValue.matches(NUMERIC_PATTERN)) {
      errors.rejectValue(field, "invalid.numeric",
          String.format(GENERIC_NUMERIC_REQUIRED, displayValue));
    }
  }

  protected void validateFieldFormat(
      String field, String fieldValue, String format, String displayValue, Errors errors) {
    if (!fieldValue.matches(format) && !fieldValue.isEmpty()) {
      errors.rejectValue(field, "invalid.format",
          String.format(GENERIC_INCORRECT_FORMAT, displayValue));
    }
  }

  protected void validatePostcodeFormat(String country, String postcode, Errors errors) {
    if (country.equals("GBR")) {
      validateRequiredField("postcode", "Postcode", errors);
      validateFieldFormat("postcode", postcode, UK_POSTCODE, "Postcode", errors);
    } else {
      if (!postcode.isEmpty()) {
        validateFieldFormat("postcode", postcode, INTERNATIONAL_POSTCODE, "Postcode", errors);
      }
    }
  }

}
