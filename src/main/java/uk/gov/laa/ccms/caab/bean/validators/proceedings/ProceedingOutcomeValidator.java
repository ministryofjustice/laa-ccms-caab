package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates proceeding outcome input captured from the record proceeding outcome page. */
@Component
public class ProceedingOutcomeValidator extends AbstractValidator {

  private static final int MAX_ADDITIONAL_INFO_LENGTH = 950;

  private static final String RESULT_INFO_DISPLAY_NAME = "Additional information about the Result";

  private static final String ADR_INFO_DISPLAY_NAME =
      "If ADR was used, explain why it was acceptable to all parties";

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
      final Date dateOfFinalWork =
          validateValidDateField(
              formData.getDateOfFinalWork(),
              "dateOfFinalWork",
              "Date of Final Work",
              COMPONENT_DATE_PATTERN,
              errors);

      if (dateOfFinalWork != null
          && dateOfFinalWork.after(
              Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()))) {
        errors.rejectValue(
            "dateOfFinalWork",
            "invalid.date.range",
            "The date you have entered is not valid. It must be the current date or earlier.");
      }
    }

    if (StringUtils.hasText(formData.getResultInfo())) {
      validateFieldFormat(
          "resultInfo",
          formData.getResultInfo(),
          STANDARD_CHARACTER_SET,
          RESULT_INFO_DISPLAY_NAME,
          errors);
      validateFieldMaxLength(
          "resultInfo",
          formData.getResultInfo(),
          MAX_ADDITIONAL_INFO_LENGTH,
          RESULT_INFO_DISPLAY_NAME,
          errors);
    }

    if (StringUtils.hasText(formData.getAdrInfo())) {
      validateFieldFormat(
          "adrInfo", formData.getAdrInfo(), STANDARD_CHARACTER_SET, ADR_INFO_DISPLAY_NAME, errors);
      validateFieldMaxLength(
          "adrInfo",
          formData.getAdrInfo(),
          MAX_ADDITIONAL_INFO_LENGTH,
          ADR_INFO_DISPLAY_NAME,
          errors);
    }

    // The court case number is mandatory once a court has been selected, otherwise optional.
    if (formData.hasCourtSelection()) {
      validateRequiredField(
          "outcomeCourtCaseNo", formData.getOutcomeCourtCaseNo(), "Court case number", errors);
    }

    if (StringUtils.hasText(formData.getOutcomeCourtCaseNo())) {
      validateFieldFormat(
          "outcomeCourtCaseNo",
          formData.getOutcomeCourtCaseNo(),
          STANDARD_CHARACTER_SET,
          "Court case number",
          errors);
    }
  }
}
