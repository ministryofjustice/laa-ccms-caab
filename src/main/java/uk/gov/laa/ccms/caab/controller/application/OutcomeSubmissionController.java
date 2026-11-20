package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseDischargeValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OutcomeSubmissionService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Captures discharge; final LAA submission is a separate journey. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class OutcomeSubmissionController {
  private static final String DISCHARGE = "application/request-case-discharge";
  private static final String SUMMARY = "redirect:/case/outcome-and-awards/summary";
  private static final String DISCHARGE_REASONS = "XXCCMS_DISCHARGE_REASON";
  private final OutcomeSubmissionService submissionService;
  private final CaseOutcomeService caseOutcomeService;
  private final LookupService lookupService;
  private final CaseDischargeValidator dischargeValidator;

  @GetMapping("/case/outcome-and-awards/discharge")
  public String discharge(
      @SessionAttribute(CASE) final ApplicationDetail currentCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {
    submissionService.requireSubmissionPermission(currentCase, user);
    final CaseOutcomeDetail draft =
        caseOutcomeService.requireCaseOutcome(
            currentCase.getCaseReferenceNumber(), user.getProvider().getId().intValue());
    requireDischargePermission(currentCase, user, draft);
    final var form = new CaseDischargeFormData();
    form.setDischargeCaseInd(draft.getDischargeCaseInd());
    form.setDischargeReason(draft.getDischargeReason());
    form.setClientContinueInd(draft.getClientContinueInd());
    form.setOtherDetails(draft.getOtherDetails());
    model.addAttribute("discharge", form);
    populateOptions(model);
    return DISCHARGE;
  }

  @PostMapping("/case/outcome-and-awards/discharge")
  public String saveDischarge(
      @SessionAttribute(CASE) final ApplicationDetail currentCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("discharge") final CaseDischargeFormData form,
      final BindingResult errors,
      final Model model) {
    submissionService.requireSubmissionPermission(currentCase, user);
    final CaseOutcomeDetail draft =
        caseOutcomeService.requireCaseOutcome(
            currentCase.getCaseReferenceNumber(), user.getProvider().getId().intValue());
    requireDischargePermission(currentCase, user, draft);
    dischargeValidator.validate(form, errors);
    final List<CommonLookupValueDetail> reasons = populateOptions(model);
    if (StringUtils.hasText(form.getDischargeReason())
        && reasons.stream()
            .noneMatch(reason -> reason.getCode().equals(form.getDischargeReason()))) {
      errors.rejectValue(
          "dischargeReason", "case.discharge.reason.invalid", "Select a valid Discharge Reason.");
    }
    if (errors.hasErrors()) {
      return DISCHARGE;
    }
    try {
      submissionService.saveDischarge(draft, form, user.getLoginId());
    } catch (CaabApiClientException ex) {
      log.warn("Failed to save discharge for case {}", currentCase.getCaseReferenceNumber(), ex);
      errors.reject(
          "case.discharge.save.failed",
          "We could not save the discharge details. Please try again.");
      return DISCHARGE;
    }
    return SUMMARY;
  }

  private void requireDischargePermission(
      final ApplicationDetail currentCase, final UserDetail user, final CaseOutcomeDetail draft) {
    if (!submissionService.canDischarge(currentCase, user, draft)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case discharge is not permitted");
    }
    if (!submissionService.awardErrors(draft).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Return to Outcome and Awards and complete the award details before continuing.");
    }
  }

  private List<CommonLookupValueDetail> populateOptions(final Model model) {
    final var reasons =
        lookupService
            .getCommonValues(DISCHARGE_REASONS)
            .blockOptional()
            .orElseThrow(
                () -> new IllegalStateException("Discharge reasons could not be retrieved"));
    final List<CommonLookupValueDetail> options =
        reasons.getContent() == null ? List.of() : reasons.getContent();
    model.addAttribute("dischargeReasons", options);
    model.addAttribute(
        "yesNoOptions",
        List.of(
            new CommonLookupValueDetail().code("true").description("Yes"),
            new CommonLookupValueDetail().code("false").description("No")));
    return options;
  }
}
