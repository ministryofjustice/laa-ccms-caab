package uk.gov.laa.ccms.caab.bean.validators.application;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.OFFICE_CODE_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UNIQUE_FILE_NUMBER_PATTERN;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.PreCertificateAndLegalHelpCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator class for pre-certificate and legal help costs validation. */
@Slf4j
@Component
public class PreCertificateAndLegalHelpCostsValidator extends AbstractValidator {

  @Override
  public boolean supports(final Class<?> clazz) {
    return PreCertificateAndLegalHelpCostsFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    log.info("Validating Pre-Certificate and Legal Help Costs Form");

    final PreCertificateAndLegalHelpCostsFormData formData =
        (PreCertificateAndLegalHelpCostsFormData) target;

    if (StringUtils.hasText(formData.getPreCertificateCosts())) {
      validateCurrencyField(
          "preCertificateCosts",
          formData.getPreCertificateCosts().trim(),
          "Pre-certificate costs",
          errors);
      validateCostAmount("preCertificateCosts", formData.getPreCertificateCosts(), errors);
    }

    if (StringUtils.hasText(formData.getLegalHelpCosts())) {
      validateCurrencyField(
          "legalHelpCosts", formData.getLegalHelpCosts().trim(), "Legal help costs", errors);
      validateCostAmount("legalHelpCosts", formData.getLegalHelpCosts(), errors);
      if (!errors.hasFieldErrors("legalHelpCosts")
          && new BigDecimal(formData.getLegalHelpCosts().trim()).signum() > 0) {
        validateRequiredField("officeCode", formData.getOfficeCode(), "Office Code", errors);
        validateRequiredField(
            "uniqueFileNumber", formData.getUniqueFileNumber(), "Unique File Number", errors);
      }
    }

    if (StringUtils.hasText(formData.getOfficeCode())) {
      validateFieldFormat(
          "officeCode",
          formData.getOfficeCode().trim(),
          OFFICE_CODE_PATTERN,
          "Office code",
          "NANNNA",
          errors);
    }

    if (StringUtils.hasText(formData.getUniqueFileNumber())) {
      validateFieldFormat(
          "uniqueFileNumber",
          formData.getUniqueFileNumber().trim(),
          UNIQUE_FILE_NUMBER_PATTERN,
          "Unique file number",
          "DDMMYY/NNN",
          errors);
    }
  }

  private void validateCostAmount(final String field, final String value, final Errors errors) {
    if (!errors.hasFieldErrors(field)) {
      final BigDecimal amount = new BigDecimal(value.trim());
      if (amount.compareTo(new BigDecimal("99999999.99")) > 0) {
        errors.rejectValue(
            field,
            "case.outcomeSubmission.cost.invalid",
            "Enter an amount between 0 and 99999999.99 with no more than two decimal places.");
      }
    }
  }
}
