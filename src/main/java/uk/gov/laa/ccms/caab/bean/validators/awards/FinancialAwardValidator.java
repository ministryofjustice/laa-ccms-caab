package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.MONETARY_INPUT_2DP;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Validates financial award input using the legacy AW03 constraints. */
@Component
public class FinancialAwardValidator extends AbstractValidator {

  private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);
  private static final BigDecimal MAX_AWARD_AMOUNT = new BigDecimal("99999999.99");

  @Override
  public boolean supports(final Class<?> clazz) {
    return FinancialAwardFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final FinancialAwardFormData formData = (FinancialAwardFormData) target;

    validateRequiredField("awardType", formData.getAwardType(), "Award type", errors);
    validateRequiredField("description", formData.getDescription(), "Award description", errors);
    validateRequiredField("awardCode", formData.getAwardCode(), "Award code", errors);
    validateRequiredField(
        "dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateRequiredField("awardAmount", formData.getAwardAmount(), "Amount of Award", errors);
    validateRequiredField("interimAward", formData.getInterimAward(), "Interim Award", errors);
    validateRequiredField("awardedBy", formData.getAwardedBy(), "Awarded By", errors);
    validateText(
        "awardType", formData.getAwardType(), 50, "Award type", STANDARD_CHARACTER_SET, errors);
    validateText(
        "description",
        formData.getDescription(),
        50,
        "Award description",
        STANDARD_CHARACTER_SET,
        errors);
    validateText(
        "awardCode", formData.getAwardCode(), 30, "Award code", STANDARD_CHARACTER_SET, errors);
    validateText(
        "awardedBy", formData.getAwardedBy(), 50, "Awarded By", STANDARD_CHARACTER_SET, errors);
    validateText(
        "interimAward",
        formData.getInterimAward(),
        50,
        "Interim Award",
        STANDARD_CHARACTER_SET,
        errors);

    validateDate("dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateDate("orderServedDate", formData.getOrderServedDate(), "Date Order was served", errors);
    validateAmount(formData.getAwardAmount(), errors);

    validateText(
        "awardJustifications",
        formData.getAwardJustifications(),
        950,
        "Justification for Award",
        STANDARD_CHARACTER_SET,
        errors);
    validateAddress("addressLine1", formData.getAddressLine1(), 70, "Address Line 1", errors);
    validateAddress("addressLine2", formData.getAddressLine2(), 35, "Address Line 2", errors);
    validateAddress("addressLine3", formData.getAddressLine3(), 35, "Address Line 3", errors);
    validateText(
        "statutoryChargeExemptReason",
        formData.getStatutoryChargeExemptReason(),
        950,
        "Reason for Statutory Charge Exemption",
        STANDARD_CHARACTER_SET,
        errors);
    validateText(
        "otherDetails",
        formData.getOtherDetails(),
        950,
        "Any other information regarding Award",
        STANDARD_CHARACTER_SET,
        errors);
  }

  private void validateDate(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }

    try {
      final LocalDate date = DateUtils.convertToLocalDate(value);
      if (date.isBefore(EARLIEST_DATE) || date.isAfter(LocalDate.now())) {
        errors.rejectValue(
            field,
            "invalid.date.range",
            "'%s' must be between 01/01/1900 and today.".formatted(displayName));
      }
    } catch (DateTimeParseException ex) {
      errors.rejectValue(
          field,
          "invalid.date",
          "Your input for '%s' is invalid. Please enter the date in DD/MM/YYYY format."
              .formatted(displayName));
    }
  }

  private void validateAmount(final String value, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    if (!value.matches(MONETARY_INPUT_2DP)) {
      errors.rejectValue(
          "awardAmount",
          "invalid.currency",
          "Please enter 'Amount of Award' as a monetary value with up to 2 decimal places.");
      return;
    }
    if (new BigDecimal(value).compareTo(MAX_AWARD_AMOUNT) > 0) {
      errors.rejectValue(
          "awardAmount",
          "value.exceeds.max",
          "'Amount of Award' must be no more than 99999999.99.");
    }
  }

  private void validateAddress(
      final String field,
      final String value,
      final int maxLength,
      final String displayName,
      final Errors errors) {
    validateText(field, value, maxLength, displayName, CHARACTER_SET_A, errors);
    if (StringUtils.hasText(value)) {
      validateDoubleSpaces(field, value, displayName, errors);
    }
  }

  private void validateText(
      final String field,
      final String value,
      final int maxLength,
      final String displayName,
      final String pattern,
      final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    validateFieldMaxLength(field, value, maxLength, displayName, errors);
    validateFieldFormat(field, value, pattern, displayName, errors);
  }
}
