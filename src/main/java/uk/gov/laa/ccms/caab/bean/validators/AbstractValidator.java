package uk.gov.laa.ccms.caab.bean.validators;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CURRENCY_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.INTERNATIONAL_POSTCODE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UK_POSTCODE;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Abstract validator used for all form validation.
 */
@Slf4j
public abstract class AbstractValidator implements Validator {

  /**
   * Generic error message.
   */
  private static final String GENERIC_REQUIRED_ERROR = "Please complete '%s'.";
  private static final String GENERIC_NUMERIC_REQUIRED = "Please enter a numeric value for %s.";
  private static final String GENERIC_CURRENCY_REQUIRED = "Please enter a currency value for %s.";
  private static final String GENERIC_DATEFIELD_ENTRY = "Your date range is invalid."
      + " Please amend your entry for the %s field.";
  protected static String GENERIC_INCORRECT_FORMAT = "Your input for '%s' is in an incorrect "
      + "format. Please amend your entry.";


  protected void validateRequiredField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (!StringUtils.hasText(fieldValue)) {
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

  protected void validateCurrencyField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null || !fieldValue.matches(CURRENCY_PATTERN)) {
      errors.rejectValue(field, "invalid.currency",
          String.format(GENERIC_CURRENCY_REQUIRED, displayValue));
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

    if (StringUtils.hasText(country)) {
      if (country.equals("GBR")) {
        validateRequiredField("postcode", postcode, "Postcode", errors);
        if (StringUtils.hasText(postcode)) {
          validateFieldFormat("postcode", postcode, UK_POSTCODE, "Postcode",
              errors);
        }
      } else {
        if (StringUtils.hasText(postcode)) {
          validateFieldFormat("postcode", postcode, INTERNATIONAL_POSTCODE,
              "Postcode", errors);
        }
      }
    }
  }

  protected Date validateValidDateField(final String dateString,
      final String field, final String datePattern, Errors errors) {
    SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
    sdf.setLenient(false);
    ParsePosition pos = new ParsePosition(0);
    Date validDate = sdf.parse(dateString, pos);
    if (pos.getIndex() == 0) {
      validDate = null;
      log.warn(String.format("invalid %s", field));
      errors.rejectValue(field, "invalid.format",
          String.format(GENERIC_INCORRECT_FORMAT, field));
    }
    return validDate;
  }

  protected void validateFromAfterToDates(final Date fromDate, final String fieldName,
      final Date toDate, Errors errors) {
    if (!toDate.after(fromDate)) {
      errors.rejectValue(fieldName, "invalid.input",
          String.format(GENERIC_DATEFIELD_ENTRY, fieldName));
    }
  }

  protected void validateDateInPast(final Date dateToCheck, final String fieldName,
      String field, Errors errors) {
    Date today = Date.from(Instant.now());
    if (!today.after(dateToCheck)) {
      errors.rejectValue(fieldName, "invalid.input",
          String.format(GENERIC_DATEFIELD_ENTRY, field));
    }
  }

  protected boolean patternMatches(String inputString, String pattern) {
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(inputString);
    //return inputString.matches(pattern);
    return m.find();
  }



}
