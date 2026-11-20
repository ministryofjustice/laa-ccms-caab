package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.math.BigDecimal;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates optional Recovery inputs using shared form validation rules. */
@Component
public class RecoveryValidator extends AbstractValidator {
  private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("100000000");
  private static final BigDecimal MAX_TOTAL = new BigDecimal("99999999.99");
  private static final String MONEY_PATTERN = "(?:[0-9]+(?:\\.[0-9]{0,2})?|\\.[0-9]{1,2})";

  @Override
  public boolean supports(final Class<?> clazz) {
    return RecoveryFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final RecoveryFormData form = (RecoveryFormData) target;
    validateAmount(
        "solicitorRecoveryAmount", form.getSolicitorRecoveryAmount(), "Solicitor Amount", errors);
    validateAmount(
        "solicitorAmountPaidToLsc",
        form.getSolicitorAmountPaidToLsc(),
        "Solicitor Paid to LAA",
        errors);
    validateAmount("courtRecoveryAmount", form.getCourtRecoveryAmount(), "Court Amount", errors);
    validateAmount(
        "courtAmountPaidToLsc", form.getCourtAmountPaidToLsc(), "Court Paid to LAA", errors);
    validateAmount("clientRecoveryAmount", form.getClientRecoveryAmount(), "Client Amount", errors);
    validateAmount(
        "clientAmountPaidToLsc", form.getClientAmountPaidToLsc(), "Client Paid to LAA", errors);
    validateAmount("offeredAmount", form.getOfferedAmount(), "Debtor's means amount", errors);
    validateDate(
        "solicitorRecoveryDate",
        form.getSolicitorRecoveryDate(),
        "Solicitor Date Received",
        errors);
    validateDate("courtRecoveryDate", form.getCourtRecoveryDate(), "Court Date Received", errors);
    validateDate(
        "clientRecoveryDate", form.getClientRecoveryDate(), "Client Date Received", errors);
    if (form.getOfferDetails() != null) {
      validateFieldMaxLength(
          "offerDetails", form.getOfferDetails(), 950, "Details of debtor offer", errors);
    }
    // Legacy character validation skips whitespace-only text, but the length limit still applies.
    if (StringUtils.hasText(form.getOfferDetails())) {
      validateFieldFormat(
          "offerDetails",
          form.getOfferDetails(),
          STANDARD_CHARACTER_SET,
          "Details of debtor offer",
          errors);
    }
  }

  private void validateAmount(
      final String field, final String value, final String label, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    validateFieldFormat(field, value, MONEY_PATTERN, label, errors);
    if (!errors.hasFieldErrors(field)) {
      validateNumericLimit(field, value, label, AMOUNT_LIMIT, errors);
    }
  }

  private void validateDate(
      final String field, final String value, final String label, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    final Date date = validateValidDateField(value, field, label, COMPONENT_DATE_PATTERN, errors);
    if (date != null) {
      validateDateInPast(date, field, label, errors);
    }
  }

  /** Checks persistence limits without restricting the legacy Calculate action. */
  public void validateTotals(
      final BigDecimal awardAmount,
      final BigDecimal recoveredAmount,
      final BigDecimal unrecoveredAmount,
      final Errors errors) {
    if (awardAmount.abs().compareTo(MAX_TOTAL) > 0) {
      errors.reject(
          "recovery.awardAmount.exceedsMax",
          "The award amount must be between -99,999,999.99 and 99,999,999.99."
              + " Return to the award to amend it before saving recovery details.");
    }
    if (recoveredAmount.abs().compareTo(MAX_TOTAL) > 0) {
      errors.rejectValue(
          "solicitorRecoveryAmount",
          "recovery.recoveredAmount.exceedsMax",
          "Total recovered must not exceed 99,999,999.99."
              + " Amend the solicitor, court or client amounts.");
    }
    if (unrecoveredAmount.abs().compareTo(MAX_TOTAL) > 0) {
      errors.rejectValue(
          "clientRecoveryAmount",
          "recovery.unrecoveredAmount.exceedsMax",
          "Amount unrecovered must be between -99,999,999.99 and 99,999,999.99."
              + " Check the award amount and the solicitor, court and client amounts.");
    }
  }
}
