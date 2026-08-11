package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates proceeding outcome input captured from the record proceeding outcome page. */
@Component
public class ProceedingOutcomeValidator extends AbstractValidator {

  private static final int MAX_ADDITIONAL_INFO_LENGTH = 950;

  @Override
  public boolean supports(final Class<?> clazz) {
    return ProceedingOutcomeFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final ProceedingOutcomeFormData formData = (ProceedingOutcomeFormData) target;

    validateRequiredField(
        "dateOfFinalWork", formData.getDateOfFinalWork(), "Date of Final Work", errors);
    validateRequiredField("stageEnd", formData.getStageEnd(), "Stage End", errors);
    validateRequiredField(
        "resolutionMethod",
        formData.getResolutionMethod(),
        "Ending or Method of Resolution",
        errors);
    validateRequiredField("result", formData.getResult(), "Result", errors);
    validateRequiredField(
        "alternativeResolution",
        formData.getAlternativeResolution(),
        "Alternative Dispute Resolution",
        errors);
    validateRequiredField("widerBenefits", formData.getWiderBenefits(), "Wider Benefits", errors);

    if (StringUtils.hasText(formData.getDateOfFinalWork())) {
      validateValidDateField(
          formData.getDateOfFinalWork(),
          "dateOfFinalWork",
          "Date of Final Work",
          COMPONENT_DATE_PATTERN,
          errors);
    }

    if (formData.getResultInfo() != null) {
      validateFieldMaxLength(
          "resultInfo",
          formData.getResultInfo(),
          MAX_ADDITIONAL_INFO_LENGTH,
          "Additional information about the Result",
          errors);
    }

    if (formData.getAdrInfo() != null) {
      validateFieldMaxLength(
          "adrInfo",
          formData.getAdrInfo(),
          MAX_ADDITIONAL_INFO_LENGTH,
          "If ADR was used, explain why it was acceptable to all parties",
          errors);
    }
  }
}
