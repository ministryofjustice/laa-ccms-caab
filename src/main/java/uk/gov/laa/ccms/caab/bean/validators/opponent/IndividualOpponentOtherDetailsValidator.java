package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;

/** Validates optional individual employment and assessment details. */
@Component
public class IndividualOpponentOtherDetailsValidator extends AbstractOpponentValidator {
  @Override
  public boolean supports(final Class<?> clazz) {
    return IndividualOpponentOtherDetailsFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final var form = (IndividualOpponentOtherDetailsFormData) target;
    if (StringUtils.hasText(form.getEmploymentStatus())) {
      validateFieldMaxLength(
          "employmentStatus", form.getEmploymentStatus(), 25, "Employment status", errors);
    }
    if (StringUtils.hasText(form.getAssessedIncomeFrequency())) {
      validateFieldMaxLength(
          "assessedIncomeFrequency",
          form.getAssessedIncomeFrequency(),
          25,
          "Assessed income frequency",
          errors);
    }
    validateText(
        "employerName", form.getEmployerName(), "Employer's name", 35, CHARACTER_SET_A, errors);
    validateText(
        "employerAddress",
        form.getEmployerAddress(),
        "Employer's address",
        200,
        STANDARD_CHARACTER_SET,
        errors);
    validateAmount("assessedIncome", form.getAssessedIncome(), "Assessed income", errors);
    validateAmount("assessedAssets", form.getAssessedAssets(), "Assessed assets", errors);
    if (StringUtils.hasText(form.getAssessmentDate())) {
      validateValidDateField(
          form.getAssessmentDate(),
          "assessmentDate",
          "Assessment date",
          COMPONENT_DATE_PATTERN,
          errors);
    }
    validateOtherInformation(form.getOtherInformation(), errors);
  }

  private void validateText(
      final String field,
      final String value,
      final String label,
      final int maximum,
      final String pattern,
      final Errors errors) {
    if (StringUtils.hasText(value)) {
      validateFieldMaxLength(field, value, maximum, label, errors);
      validateFieldFormat(field, value, pattern, label, errors);
    }
  }

  private void validateAmount(
      final String field, final String value, final String label, final Errors errors) {
    if (StringUtils.hasText(value)) {
      validateCurrencyField(field, value, label, errors);
      if (!errors.hasFieldErrors(field)) {
        validateNumericLimit(field, value, label, new BigDecimal("100000000"), errors);
      }
    }
  }
}
