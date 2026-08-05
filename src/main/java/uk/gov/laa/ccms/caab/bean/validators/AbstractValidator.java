package uk.gov.laa.ccms.caab.bean.validators;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CURRENCY_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.FIRST_CHARACTER_MUST_BE_ALPHA;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.INTERNATIONAL_POSTCODE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.MONETARY_INPUT_2DP;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.TELEPHONE_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UK_POSTCODE;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;
import static uk.gov.laa.ccms.caab.util.DateUtils.convertToDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.laa.ccms.caab.bean.common.Individual;

/** Abstract validator used for all form validation. */
@Slf4j
public abstract class AbstractValidator implements Validator {

  /** Generic error message. */
  private static final String GENERIC_REQUIRED_ERROR = "Please complete '%s'.";

  private static final String GENERIC_NUMERIC_REQUIRED = "Please enter a numeric value for '%s'.";
  private static final String GENERIC_CURRENCY_REQUIRED = "Please enter a currency value for '%s'.";
  private static final String GENERIC_DATEFIELD_ENTRY =
      "Your date range is invalid. Please amend your entry for the %s field.";
  private static final String GENERIC_DATEFIELD_PAST =
      "You must provide a date in the past for the %s field. Please amend your entry.";
  private static final String GENERIC_DATEFIELD_AFTER_DATE =
      "You must provide a date after %s for the %s field. Please amend your entry.";
  protected static String GENERIC_INCORRECT_FORMAT =
      "Your input for '%s' is in an incorrect format. Please amend your entry.";

  protected static String GENERIC_MISSING_DATE_FIELDS_FORMAT =
      "Your input for '%s' is incomplete. Please enter a value for day, month and year.";

  protected static final String SPECIFIC_DATEFIELD_ENTRY =
      "Your input for '%s' is invalid. Please enter the date in %s format.";

  protected static String GENERIC_FIRST_CHAR_ALPHA =
      "Your input for %s is invalid. "
          + "The first character must be a letter. Please amend your entry.";

  protected static String GENERIC_NUMERIC_LIMIT_ERROR = "'%s' must be less than %s";

  protected static final String GENERIC_MAX_LENGTH =
      "Please enter a maximum of %s characters for '%s'.";

  protected static final String GENERIC_MIN_LENGTH =
      "Please enter a minimum of %s characters for '%s'.";

  protected static final String GENERIC_DOUBLE_SPACES =
      "Your input for '%s' contains double spaces. Please amend your entry.";

  protected static final String GENERIC_DECIMAL_PLACES =
      "Your input for '%s' cannot contain more than %s decimal places. Please amend your entry.";

  protected static final String GENERIC_AT_LEAST_ONE_SEARCH_REQUIRED =
      "You must provide at least one search criteria below. Please amend your entry.";

  private static final String GENERIC_VALID_CHARACTER_INPUT_REQUIRED =
      "Your input for '%s' contains an invalid character. Please amend your entry.";

  protected void validateRequiredField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (!StringUtils.hasText(fieldValue)) {
      errors.rejectValue(
          field, "required." + field, GENERIC_REQUIRED_ERROR.formatted(displayValue));
    }
  }

  protected void validateRequiredField(
      final String field, final Integer fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null) {
      errors.rejectValue(
          field, "required." + field, GENERIC_REQUIRED_ERROR.formatted(displayValue));
    }
  }

  protected void validateNumericField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null || !fieldValue.matches(NUMERIC_PATTERN)) {
      errors.rejectValue(
          field, "invalid.numeric", GENERIC_NUMERIC_REQUIRED.formatted(displayValue));
    }
  }

  protected void validateCurrencyField(
      final String field, final String fieldValue, final String displayValue, Errors errors) {

    if (fieldValue == null || !fieldValue.matches(CURRENCY_PATTERN)) {
      errors.rejectValue(
          field, "invalid.currency", GENERIC_CURRENCY_REQUIRED.formatted(displayValue));
    }
  }

  protected void validateNumericLimit(
      final String field,
      final String fieldValue,
      final String displayValue,
      final BigDecimal maxLimit,
      final Errors errors) {
    try {
      final BigDecimal value = new BigDecimal(fieldValue);
      if (value.compareTo(maxLimit) >= 0) {
        errors.rejectValue(
            field,
            "value.exceeds.max",
            GENERIC_NUMERIC_LIMIT_ERROR.formatted(displayValue, maxLimit));
      }
    } catch (final NumberFormatException e) {
      errors.rejectValue(
          field, "invalid.numeric", GENERIC_NUMERIC_REQUIRED.formatted(displayValue));
    }
  }

  protected void validateNumberAllowed2dp(
      final String field, final String fieldValue, Errors errors) {

    if (!fieldValue.matches(MONETARY_INPUT_2DP)) {
      errors.rejectValue(
          field,
          "invalid.decimal.places",
          GENERIC_DECIMAL_PLACES.formatted("Provider Cost Breakdown", 2));
    }
  }

  protected void validateFieldFormat(
      final String field,
      final String fieldValue,
      final String format,
      String displayValue,
      Errors errors) {

    if (fieldValue == null || (!fieldValue.matches(format) && !fieldValue.isEmpty())) {
      errors.rejectValue(field, "invalid.format", GENERIC_INCORRECT_FORMAT.formatted(displayValue));
    }
  }

  protected void validateFirstCharAlpha(
      final String field, final String fieldValue, String displayValue, Errors errors) {

    if (fieldValue == null
        || (!fieldValue.matches(FIRST_CHARACTER_MUST_BE_ALPHA) && !fieldValue.isEmpty())) {
      errors.rejectValue(
          field, "first.char.alpha", GENERIC_FIRST_CHAR_ALPHA.formatted(displayValue));
    }
  }

  protected void validateDoubleSpaces(
      final String field, final String fieldValue, String displayValue, Errors errors) {

    if (fieldValue != null && fieldValue.contains("\s\s")) {
      errors.rejectValue(field, "double.spaces", GENERIC_DOUBLE_SPACES.formatted(displayValue));
    }
  }

  protected void validateFieldMaxLength(
      final String field,
      final String fieldValue,
      final int maxLength,
      String displayValue,
      Errors errors) {

    if (fieldValue == null || fieldValue.length() > maxLength) {
      errors.rejectValue(
          field,
          "length.exceeds.max",
          new Object[] {maxLength, displayValue},
          GENERIC_MAX_LENGTH.formatted(maxLength, displayValue));
    }
  }

  protected void validateFieldMinLength(
      final String field,
      final String fieldValue,
      final int minLength,
      String displayValue,
      Errors errors) {

    if (fieldValue == null || fieldValue.length() < minLength) {
      errors.rejectValue(
          field, "length.below.min", GENERIC_MIN_LENGTH.formatted(minLength, displayValue));
    }
  }

  protected void validatePostcodeFormat(
      final String country, final String postcode, Errors errors) {

    if (StringUtils.hasText(country)) {
      if ("GBR".equals(country)) {
        validateUkPostcodeFormat(postcode, true, errors);
      } else {
        validateInternationalPostcodeFormat(postcode, false, errors);
      }
    }
  }

  protected void validateUkPostcodeFormat(
      final String postcode, final boolean required, Errors errors) {

    if (required) {
      validateRequiredField("postcode", postcode, "Postcode", errors);
    }

    if (StringUtils.hasText(postcode)) {
      validateFieldFormat("postcode", postcode, UK_POSTCODE, "Postcode", errors);
    }
  }

  protected void validateInternationalPostcodeFormat(
      final String postcode, final boolean required, Errors errors) {

    if (required) {
      validateRequiredField("postcode", postcode, "Postcode", errors);
    }

    if (StringUtils.hasText(postcode)) {
      validateFieldFormat("postcode", postcode, INTERNATIONAL_POSTCODE, "Postcode", errors);
    }
  }

  protected void validateTelephoneNumber(
      final String field,
      final String fieldValue,
      final boolean required,
      final String displayValue,
      final Errors errors) {

    if (required) {
      validateRequiredField(field, fieldValue, displayValue, errors);
    }

    if (StringUtils.hasText(fieldValue)) {
      validateFieldMinLength(field, fieldValue, 8, displayValue, errors);
      validateFieldFormat(field, fieldValue, TELEPHONE_PATTERN, displayValue, errors);
      validateDoubleSpaces(field, fieldValue, displayValue, errors);
    }
  }

  protected Date validateValidDateField(
      final String dateString,
      final String field,
      final String displayName,
      final String datePattern,
      Errors errors) {
    final Date validDate;
    final String strictPattern = datePattern.replace("y", "u");

    final DateTimeFormatter strictFormatter =
        DateTimeFormatter.ofPattern(strictPattern).withResolverStyle(ResolverStyle.STRICT);
    LocalDate parsedDate;
    try {
      if (dateString.length() < datePattern.length()) {
        final String strictFlexiblePattern = strictPattern.replace("dd", "d").replace("MM", "M");
        final DateTimeFormatter strictFlexibleFormatter =
            DateTimeFormatter.ofPattern(strictFlexiblePattern)
                .withResolverStyle(ResolverStyle.STRICT);
        parsedDate = LocalDate.parse(dateString, strictFlexibleFormatter);
      } else {
        parsedDate = LocalDate.parse(dateString, strictFormatter);
      }
      validDate = Date.from(parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    } catch (DateTimeParseException ex) {
      if (dateErrorCausedByDateFormat(dateString, datePattern)) {
        errors.rejectValue(
            field,
            "invalid.format",
            SPECIFIC_DATEFIELD_ENTRY.formatted(displayName, datePattern.toUpperCase()));
      } else {
        reportInvalidDate(field, displayName, errors);
      }
      return null;
    }
    validateDateAfterGivenDate(
        validDate,
        Date.from(LocalDate.of(1901, 12, 13).atStartOfDay(ZoneId.systemDefault()).toInstant()),
        field,
        displayName,
        errors);
    if (errors.hasFieldErrors(field)) {
      return null;
    }
    return validDate;
  }

  private boolean dateErrorCausedByDateFormat(final String dateString, final String datePattern) {
    String separator;
    final Matcher matcher = Pattern.compile("[^A-Za-z]").matcher(datePattern);
    if (matcher.find()) {
      separator = matcher.group();
    } else {
      return false;
    }
    final String[] patternParts = datePattern.split(separator, -1);
    final String[] dateParts = dateString.split(separator, -1);
    boolean dateFormatError = false;
    if (patternParts.length != dateParts.length) {
      dateFormatError = true;
    } else {
      for (int i = 0; i < patternParts.length; i++) {
        if (!dateParts[i].matches("\\d+")) {
          dateFormatError = false;
          break;
        } else if ((patternParts[i].contains("y")
                && patternParts[i].length() != dateParts[i].length())
            || (!patternParts[i].contains("y")
                && dateParts[i].length() > patternParts[i].length())) {
          dateFormatError = true;
        }
      }
    }
    return dateFormatError;
  }

  protected static void reportInvalidDate(String field, String displayName, Errors errors) {
    log.warn("invalid %s".formatted(field));
    errors.rejectValue(field, "invalid.format", GENERIC_INCORRECT_FORMAT.formatted(displayName));
  }

  protected static void reportMissingDateFields(String field, String displayName, Errors errors) {
    log.warn("missing input for %s".formatted(field));
    errors.rejectValue(
        field, "invalid.input", GENERIC_MISSING_DATE_FIELDS_FORMAT.formatted(displayName));
  }

  /**
   * Validates the date of birth of the {@link Individual}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateDateOfBirth(Object target, Errors errors, boolean required) {
    Individual individual = (Individual) target;
    String dateOfBirth = individual.getDateOfBirth();

    if (required && !StringUtils.hasText(dateOfBirth)) {
      errors.rejectValue("dateOfBirth", "required.dob", "Please complete 'Date of birth'");
      return;
    }

    if (StringUtils.hasText(dateOfBirth)) {
      validateValidDateField(
          dateOfBirth, "dateOfBirth", "Date of birth", COMPONENT_DATE_PATTERN, errors);
      if (!errors.hasFieldErrors("dateOfBirth")) {
        try {
          validateDateInPast(convertToDate(dateOfBirth), "dateOfBirth", "Date of birth", errors);
        } catch (DateTimeParseException ex) {
          errors.rejectValue(
              "dateOfBirth",
              "invalid.format",
              SPECIFIC_DATEFIELD_ENTRY.formatted(
                  "Date of birth", COMPONENT_DATE_PATTERN.toUpperCase()));
        }
      }
    }
  }

  protected void validateFromBeforeToDates(
      final Date fromDate, final String fieldName, final Date toDate, Errors errors) {
    if (!toDate.after(fromDate)) {
      errors.rejectValue(fieldName, "invalid.input", GENERIC_DATEFIELD_ENTRY.formatted(fieldName));
    }
  }

  protected void validateDateInPast(
      final Date dateToCheck, final String fieldName, String field, Errors errors) {
    Date today = Date.from(Instant.now());
    if (!today.after(dateToCheck)) {
      errors.rejectValue(fieldName, "invalid.input", GENERIC_DATEFIELD_PAST.formatted(field));
    }
  }

  protected void validateDateAfterGivenDate(
      final Date dateToCheck,
      final Date givenDate,
      final String fieldName,
      String field,
      Errors errors) {
    if (!dateToCheck.after(givenDate)) {
      errors.rejectValue(
          fieldName,
          "invalid.input",
          GENERIC_DATEFIELD_AFTER_DATE.formatted(
              givenDate
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate()
                  .format(
                      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.UK)),
              field));
    }
  }

  protected boolean patternMatches(String inputString, String pattern) {
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(inputString);
    return m.find();
  }

  protected void validateAllFieldsAreEmpty(
      final String field, Errors errors, final String... fieldValue) {

    if (Stream.of(fieldValue).allMatch(StringUtils::isEmpty)) {
      errors.rejectValue(
          field, "at.least.one.search.required", GENERIC_AT_LEAST_ONE_SEARCH_REQUIRED);
    }
  }

  protected void validateFieldPattern(
      final String field,
      final String fieldValue,
      String pattern,
      final String displayValue,
      final Errors errors) {

    if (!StringUtils.hasText(fieldValue)) {
      if (fieldValue.matches(pattern)) {
        errors.rejectValue(
            field,
            "invalid.input.character.pattern",
            GENERIC_VALID_CHARACTER_INPUT_REQUIRED.formatted(fieldValue));
      }
    }
  }
}
