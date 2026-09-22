package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates other asset award input against the CAAB API constraints. */
@Component
public class OtherAssetAwardValidator extends AbstractValidator {

  private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
  private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");

  @Override
  public boolean supports(final Class<?> clazz) {
    return OtherAssetAwardFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final OtherAssetAwardFormData formData = (OtherAssetAwardFormData) target;

    validateRequiredField("awardType", formData.getAwardType(), "Award type", errors);
    validateRequiredField("description", formData.getDescription(), "Award description", errors);
    validateRequiredField("awardCode", formData.getAwardCode(), "Award code", errors);
    validateRequiredField(
        "dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateRequiredField("awardedBy", formData.getAwardedBy(), "Awarded By", errors);
    validateRequiredField(
        "valuationAmount", formData.getValuationAmount(), "Valuation Amount", errors);
    validateRequiredField("valuationDate", formData.getValuationDate(), "Valuation Date", errors);
    if (formData.getRecoveryOfAwardTimeRelated() == null) {
      errors.rejectValue(
          "recoveryOfAwardTimeRelated",
          "required.recoveryOfAwardTimeRelated",
          "Please complete 'Is recovery of the award time related?'.");
    }

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
        "valuationCriteria",
        formData.getValuationCriteria(),
        50,
        "Valuation Criteria",
        STANDARD_CHARACTER_SET,
        errors);
    validateText(
        "recovery", formData.getRecovery(), 200, "Recovery", STANDARD_CHARACTER_SET, errors);
    validateText(
        "noRecoveryDetails",
        formData.getNoRecoveryDetails(),
        1000,
        "No Recovery Details",
        STANDARD_CHARACTER_SET,
        errors);
    validateText(
        "statutoryChargeExemptReason",
        formData.getStatutoryChargeExemptReason(),
        1000,
        "Reason for Statutory Charge Exemption",
        STANDARD_CHARACTER_SET,
        errors);

    validateDate("dateOfOrder", formData.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateDate("valuationDate", formData.getValuationDate(), "Valuation Date", errors);

    final Map<String, String> amounts =
        Map.of(
            "valuationAmount", "Valuation Amount",
            "recoveredAmount", "Amount Recovered",
            "disputedAmount", "Amount Disputed",
            "awardedAmount", "Amount Awarded");
    amounts.forEach(
        (field, displayName) ->
            validateAmount(field, getAmountValue(formData, field), displayName, errors));

    validatePercentage(
        "awardedPercentage", formData.getAwardedPercentage(), "Percentage Awarded", errors);
    validatePercentage(
        "recoveredPercentage", formData.getRecoveredPercentage(), "Percentage Recovered", errors);
    validatePercentage(
        "disputedPercentage", formData.getDisputedPercentage(), "Percentage Disputed", errors);
  }

  private String getAmountValue(final OtherAssetAwardFormData formData, final String field) {
    return switch (field) {
      case "valuationAmount" -> formData.getValuationAmount();
      case "recoveredAmount" -> formData.getRecoveredAmount();
      case "disputedAmount" -> formData.getDisputedAmount();
      case "awardedAmount" -> formData.getAwardedAmount();
      default -> throw new IllegalArgumentException("Unknown amount field: " + field);
    };
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

  private void validateAmount(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    validateCurrencyField(field, value, displayName, errors);
    if (!errors.hasFieldErrors(field) && new BigDecimal(value).compareTo(MAX_AMOUNT) > 0) {
      errors.rejectValue(
          field,
          "value.exceeds.max",
          "'%s' must be no more than 99999999.99.".formatted(displayName));
    }
  }

  private void validatePercentage(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    if (!value.matches("\\d+(\\.\\d{1,2})?")) {
      errors.rejectValue(
          field,
          "invalid.percentage",
          "Please enter '%s' as a number with no more than 2 decimal places."
              .formatted(displayName));
      return;
    }
    if (new BigDecimal(value).compareTo(MAX_PERCENTAGE) > 0) {
      errors.rejectValue(
          field, "value.exceeds.max", "'%s' must be no more than 100.".formatted(displayName));
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
