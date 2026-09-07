package uk.gov.laa.ccms.caab.bean.validators.application;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.OFFICE_CODE_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.UNIQUE_FILE_NUMBER_PATTERN;

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
    }

    if (StringUtils.hasText(formData.getLegalHelpCosts())) {
      validateCurrencyField(
          "legalHelpCosts", formData.getLegalHelpCosts().trim(), "Legal help costs", errors);
    }

    if (StringUtils.hasText(formData.getOfficeCode())) {
      validateFieldFormat(
          "officeCode",
          formData.getOfficeCode().trim(),
          OFFICE_CODE_PATTERN,
          "Office code",
          errors);
    }

    if (StringUtils.hasText(formData.getUniqueFileNumber())) {
      validateFieldFormat(
          "uniqueFileNumber",
          formData.getUniqueFileNumber().trim(),
          UNIQUE_FILE_NUMBER_PATTERN,
          "Unique file number",
          errors);
    }
  }
}
