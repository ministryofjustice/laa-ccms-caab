package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CURRENCY_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates cost award input data. */
@Component
public class CostAwardValidator extends AbstractValidator {

  private static final BigDecimal MAX_AWARD_AMOUNT = new BigDecimal("99999999.99");

  @Override
  public boolean supports(final Class<?> clazz) {
    return CostAwardFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final CostAwardFormData formData = (CostAwardFormData) target;

    validateRequiredField("awardType", formData.getAwardType(), "Award type", errors);
    validateRequiredField("description", formData.getDescription(), "Award description", errors);
    validateRequiredField("awardCode", formData.getAwardCode(), "Award code", errors);
    validateRequiredField(
        "dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateRequiredField(
        "courtAssessmentStatus",
        formData.getCourtAssessmentStatus(),
        "Court Assessment Status",
        errors);
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
        "courtAssessmentStatus",
        formData.getCourtAssessmentStatus(),
        50,
        "Court Assessment Status",
        STANDARD_CHARACTER_SET,
        errors);
    validateText(
        "awardedBy", formData.getAwardedBy(), 50, "Awarded By", STANDARD_CHARACTER_SET, errors);

    validateDate("dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateDate(
        "interestStartDate", formData.getInterestStartDate(), "Interest Start Date", errors);
    validateDate("orderServedDate", formData.getOrderServedDate(), "Date Order was served", errors);

    validateCurrency(
        "laaFundedLegalCosts",
        formData.getLaaFundedLegalCosts(),
        "LAA funded Legal advice / assistance / help costs",
        errors);
    validateCurrency(
        "otherPreCertificateCosts",
        formData.getOtherPreCertificateCosts(),
        "Other Pre-Certificate Costs",
        errors);
    validateCurrency("laaRate", formData.getLaaRate(), "LAA Rate", errors);
    validateCurrency("marketRate", formData.getMarketRate(), "Market Rate", errors);
    validateDecimal("interestRate", formData.getInterestRate(), "Interest Rate", errors);

    validateAddress(
        "addressLine1", formData.getAddressLine1(), 70, "Address of Service Line 1", errors);
    validateAddress(
        "addressLine2", formData.getAddressLine2(), 35, "Address of Service Line 2", errors);
    validateAddress(
        "addressLine3", formData.getAddressLine3(), 35, "Address of Service Line 3", errors);
    validateText(
        "otherDetails",
        formData.getOtherDetails(),
        950,
        "Any other information about Award",
        STANDARD_CHARACTER_SET,
        errors);
  }

  private void validateDate(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }

    final Date date =
        validateValidDateField(value, field, displayName, COMPONENT_DATE_PATTERN, errors);
    if (date != null
        && date.after(
            Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()))) {
      errors.rejectValue(
          field, "invalid.date.range", "'%s' must not be in the future.".formatted(displayName));
    }
  }

  private void validateCurrency(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }

    validateCurrencyField(field, value, displayName, errors);
    if (errors.hasFieldErrors(field)) {
      return;
    }
    if (new BigDecimal(value).compareTo(MAX_AWARD_AMOUNT) > 0) {
      errors.rejectValue(
          field,
          "value.exceeds.max",
          "'%s' must be no more than 99999999.99.".formatted(displayName));
    }
  }

  private void validateDecimal(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    if (!value.matches(CURRENCY_PATTERN)) {
      errors.rejectValue(
          field,
          "invalid.numeric",
          "Please enter a numeric value for '%s'.".formatted(displayName));
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
