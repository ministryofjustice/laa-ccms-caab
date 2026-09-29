package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates land award input using the legacy AW05 constraints. */
@Component
public class LandAwardValidator extends AbstractValidator {

  private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

  @Override
  public boolean supports(final Class<?> clazz) {
    return LandAwardFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final LandAwardFormData form = (LandAwardFormData) target;

    validateRequiredField("awardType", form.getAwardType(), "Award type", errors);
    validateRequiredField("awardCode", form.getAwardCode(), "Award code", errors);
    validateRequiredField(
        "dateOfOrder", form.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateRequiredField("description", form.getDescription(), "Description of Award", errors);
    validateRequiredField(
        "addressLine1", form.getAddressLine1(), "Address of Property Line 1", errors);
    validateRequiredField("valuationAmount", form.getValuationAmount(), "Valuation", errors);
    validateRequiredField(
        "valuationCriteria", form.getValuationCriteria(), "Basis of Valuation", errors);
    validateRequiredField("valuationDate", form.getValuationDate(), "Date of Valuation", errors);
    validateRequiredField(
        "disputedPercentage", form.getDisputedPercentage(), "Percent in Dispute", errors);
    validateRequiredField(
        "mortgageAmountDue", form.getMortgageAmountDue(), "Amount due under Mortgage", errors);
    validateRequiredField("awardedBy", form.getAwardedBy(), "Awarded by", errors);
    validateRequiredField("recovery", form.getRecovery(), "Recovery", errors);
    validateRequiredField(
        "recoveryOfAwardTimeRelated",
        form.getRecoveryOfAwardTimeRelated(),
        "Is recovery of the Award time related?",
        errors);

    validateText("awardType", form.getAwardType(), 50, "Award type", errors);
    validateText("awardCode", form.getAwardCode(), 30, "Award code", errors);
    validateText("description", form.getDescription(), 35, "Description of Award", errors);
    validateText("titleNumber", form.getTitleNumber(), 35, "Title No.", errors);
    validateAddress(
        "addressLine1", form.getAddressLine1(), 70, "Address of Property Line 1", errors);
    validateAddress(
        "addressLine2", form.getAddressLine2(), 35, "Address of Property Line 2", errors);
    validateAddress(
        "addressLine3", form.getAddressLine3(), 35, "Address of Property Line 3", errors);
    validateText(
        "noRecoveryDetails",
        form.getNoRecoveryDetails(),
        950,
        "If awarded but not yet recovered please provide details",
        errors);
    validateText(
        "statutoryChargeExemptReason",
        form.getStatutoryChargeExemptReason(),
        950,
        "Reason for Statutory Charge Exemption",
        errors);
    validateText(
        "registrationReference",
        form.getRegistrationReference(),
        35,
        "Registration Reference",
        errors);

    validateDate("dateOfOrder", form.getDateOfOrder(), "Date of Order / Agreement", errors);
    validateDate("valuationDate", form.getValuationDate(), "Date of Valuation", errors);
    validateAmount("valuationAmount", form.getValuationAmount(), "Valuation", errors);
    validateAmount(
        "disputedPercentage", form.getDisputedPercentage(), "Percent in Dispute", errors);
    validateAmount(
        "awardedPercentage", form.getAwardedPercentage(), "Percent Awarded to Client", errors);
    validateAmount(
        "mortgageAmountDue", form.getMortgageAmountDue(), "Amount due under Mortgage", errors);
  }

  private void validateDate(
      final String field, final String value, final String displayName, final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    final Date date =
        validateValidDateField(value, field, displayName, COMPONENT_DATE_PATTERN, errors);
    if (date == null) {
      return;
    }
    final LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    if (localDate.isAfter(LocalDate.now())) {
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

  private void validateAddress(
      final String field,
      final String value,
      final int maxLength,
      final String displayName,
      final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    validateFieldMaxLength(field, value, maxLength, displayName, errors);
    validateFieldFormat(field, value, CHARACTER_SET_A, displayName, errors);
    validateDoubleSpaces(field, value, displayName, errors);
  }

  private void validateText(
      final String field,
      final String value,
      final int maxLength,
      final String displayName,
      final Errors errors) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    validateFieldMaxLength(field, value, maxLength, displayName, errors);
    validateFieldFormat(field, value, STANDARD_CHARACTER_SET, displayName, errors);
  }
}
