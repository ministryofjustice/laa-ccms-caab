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
    if (StringUtils.hasText(form.getOfferDetails())) {
      validateFieldMaxLength(
          "offerDetails", form.getOfferDetails(), 950, "Details of debtor offer", errors);
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
}
