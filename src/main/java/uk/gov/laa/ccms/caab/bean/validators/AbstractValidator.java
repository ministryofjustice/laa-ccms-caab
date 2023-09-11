package uk.gov.laa.ccms.caab.bean.validators;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.INTERNATIONAL_POSTCODE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UK_POSTCODE;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Abstract validator used for all form validation.
 */
public abstract class AbstractValidator implements Validator {

  private static String GENERIC_REQUIRED_ERROR = "Please complete '%s'.";

  private static String GENERIC_NUMERIC_REQUIRED = "Please enter a numeric value for %s.";

  private static String GENERIC_INCORRECT_FORMAT = "Your input for '%s' is in an incorrect "
      + "format. Please amend your entry.";

  private static String GENERIC_INVALID_CHARACTER = "Your input for '%s' contains an invalid "
      + "character. Please amend your entry.";

  protected void validateRequiredField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null || fieldValue.isEmpty()) {
      errors.rejectValue(field, "required." + field,
          String.format(GENERIC_REQUIRED_ERROR, displayValue));
    }
  }

  protected void validateRequiredField(
      final String field, final Integer fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null) {
      errors.rejectValue(field, "required." + field,
          String.format(GENERIC_REQUIRED_ERROR, displayValue));
    }
  }

  protected void validateNumericField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null || !fieldValue.matches(NUMERIC_PATTERN)) {
      errors.rejectValue(field, "invalid.numeric",
          String.format(GENERIC_NUMERIC_REQUIRED, displayValue));
    }
  }

  protected void validateFieldFormat(
      final String field, final String fieldValue, final String format,
      String displayValue, Errors errors) {

    if (fieldValue == null || (!fieldValue.matches(format) && !fieldValue.isEmpty())) {
      errors.rejectValue(field, "invalid.format",
          String.format(GENERIC_INCORRECT_FORMAT, displayValue));
    }
  }

  protected void validatePostcodeFormat(
      final String country, final String postcode, Errors errors) {

    if (country != null && !country.isBlank()){
      if (country.equals("GBR")) {
        validateRequiredField("postcode", postcode, "Postcode", errors);
        validateFieldFormat("postcode", postcode, UK_POSTCODE, "Postcode", errors);
      } else {
        if (postcode != null && !postcode.isEmpty()) {
          validateFieldFormat("postcode", postcode, INTERNATIONAL_POSTCODE, "Postcode", errors);
        }
      }
    }
  }
}
