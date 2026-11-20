package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Conditional requirements from the legacy DC01 screen. */
@Component
public class CaseDischargeValidator extends AbstractValidator {
  @Override
  public boolean supports(final Class<?> clazz) {
    return CaseDischargeFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final CaseDischargeFormData form = (CaseDischargeFormData) target;
    if (form.getDischargeCaseInd() == null && !errors.hasFieldErrors("dischargeCaseInd")) {
      errors.rejectValue(
          "dischargeCaseInd", "case.discharge.required", "Please complete 'Discharge Case'.");
    }
    if (Boolean.TRUE.equals(form.getDischargeCaseInd())) {
      if (!StringUtils.hasText(form.getDischargeReason())) {
        errors.rejectValue(
            "dischargeReason",
            "case.discharge.reason.required",
            "Please complete 'Discharge Reason' as you have indicated that you want to discharge "
                + "this Case, or select No above if you do not want to discharge.");
      }
      if (form.getClientContinueInd() == null && !errors.hasFieldErrors("clientContinueInd")) {
        errors.rejectValue(
            "clientContinueInd",
            "case.discharge.client.required",
            "Please complete 'Client Continuing Privately' as you have indicated that you want "
                + "to discharge this Case, or select No above if you do not want to discharge.");
      }
    }
    if (form.getOtherDetails() != null) {
      validateFieldMaxLength(
          "otherDetails", form.getOtherDetails(), 950, "Any other Discharge Information", errors);
    }
  }
}
