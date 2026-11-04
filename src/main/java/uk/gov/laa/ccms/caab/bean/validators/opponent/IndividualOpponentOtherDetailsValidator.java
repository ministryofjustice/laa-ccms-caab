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
  /**
   * Determines whether the supplied class is an individual other-details form.
   *
   * @param clazz the class to check
   * @return true for the supported form class and its subclasses
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return IndividualOpponentOtherDetailsFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates supplied optional employment, assessment and other-information fields.
   *
   * @param target the individual other-details form to validate
   * @param errors the binding result to receive validation errors
   */
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

  /**
   * Checks the length and permitted characters of a non-blank text value.
   *
   * @param field the form field name
   * @param value the submitted value
   * @param label the field label used in error messages
   * @param maximum the maximum permitted number of characters
   * @param pattern the permitted-character regular expression
   * @param errors the binding result to receive validation errors
   */
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

  /**
   * Checks currency format and the numeric limit for a non-blank assessment amount.
   *
   * @param field the form field name
   * @param value the submitted amount
   * @param label the field label used in error messages
   * @param errors the binding result to receive validation errors
   */
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
