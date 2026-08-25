package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OUTCOME_ADR;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OUTCOME_RESOLUTION_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_WIDER_BENEFITS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AMEND_CLIENT_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_OUTCOME_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COURT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;
import static uk.gov.laa.ccms.caab.util.DateUtils.convertToComponentDate;
import static uk.gov.laa.ccms.caab.util.view.ActionViewHelper.enhanceActionUrl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.laa.ccms.caab.bean.proceeding.CaseProceedingDisplayStatus;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingOutcomeValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.AmendClientOrigin;
import uk.gov.laa.ccms.caab.constants.PriorAuthorityGroup;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AvailableAction;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.caab.util.PriorAuthorityUtils;
import uk.gov.laa.ccms.caab.util.view.ActionViewHelper;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to cases. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class CaseController {

  private final ApplicationService applicationService;
  private final LookupService lookupService;
  private final CaseOutcomeService caseOutcomeService;
  private final ProceedingOutcomeValidator proceedingOutcomeValidator;
  private static final String SEARCH_URL = "SEARCH_URL";

  /**
   * Displays the case overview screen.
   *
   * @param ebsCase The case details from EBS.
   * @param tdsApplication The application details from TDS, if available.
   * @param notificationId The ID of the notification, if coming from a notification page.
   * @return The case overview view.
   */
  @GetMapping("/case/overview")
  public String caseOverview(
      @SessionAttribute(CASE) @Nullable ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_SUMMARY) @Nullable final BaseApplicationDetail tdsApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(NOTIFICATION_ID) @Nullable final String notificationId,
      Model model,
      HttpSession session,
      HttpServletRequest request) {

    if (ebsCase == null) {
      String caseReferenceNumber = (String) session.getAttribute(CASE_REFERENCE_NUMBER);
      if (caseReferenceNumber != null) {
        ebsCase =
            applicationService.getCase(
                caseReferenceNumber, user.getProvider().getId(), user.getLoginId());
        session.setAttribute(CASE, ebsCase);
      }
    }

    if (ebsCase == null) {
      throw new CaabApplicationException("Failed to retrieve case details");
    }

    setReturnDetails(model, notificationId, request);

    BaseApplicationDetail resolvedTds = getTdsApplication(tdsApplication, ebsCase, user);
    if (resolvedTds != null) {
      session.setAttribute(APPLICATION_SUMMARY, resolvedTds);
    }

    ApplicationDetail amendments = resolveAmendments(ebsCase, resolvedTds);

    boolean isAmendment = amendments != null;

    if (!isAmendment) {
      amendments = getRecentlySubmittedAmendment(ebsCase, user);
      isAmendment = amendments != null;
    }

    if (!isAmendment) {
      clearAmendmentSession(session);
    }

    setProceedingDisplayStatuses(ebsCase, amendments);

    model.addAttribute("searchUrl", Objects.toString(session.getAttribute(SEARCH_URL), ""));
    model.addAttribute("case", ebsCase);
    model.addAttribute("isAmendment", isAmendment);
    model.addAttribute(
        "availableActions",
        getAvailableActions(ebsCase, isAmendment, amendments, ebsCase.getCaseReferenceNumber()));
    model.addAttribute("hasEbsAmendments", hasEbsAmendments(ebsCase));
    model.addAttribute(
        "draftProceedings",
        isAmendment ? amendments.getProceedings() : ebsCase.getAmendmentProceedingsInEbs());
    model.addAttribute("draftCosts", isAmendment ? amendments.getCosts() : ebsCase.getCosts());

    session.setAttribute(CASE_REFERENCE_NUMBER, ebsCase.getCaseReferenceNumber());
    session.setAttribute(APPLICATION, amendments);
    session.setAttribute(AMEND_CLIENT_ORIGIN, AmendClientOrigin.CASE_OVERVIEW);
    session.removeAttribute(COST_ALLOCATION_FORM_DATA);

    return "application/case-overview";
  }

  private BaseApplicationDetail getTdsApplication(
      @Nullable BaseApplicationDetail tdsApplication, ApplicationDetail ebsCase, UserDetail user) {
    if (tdsApplication != null) {
      return tdsApplication;
    }

    return applicationService.getTdsApplicationSummary(ebsCase.getCaseReferenceNumber(), user);
  }

  private ApplicationDetail resolveAmendments(
      ApplicationDetail ebsCase, @Nullable BaseApplicationDetail tdsApplication) {
    if (!applicationService.isAmendment(ebsCase, tdsApplication)) {
      return null;
    }

    return resolveAmendment(tdsApplication);
  }

  private ApplicationDetail resolveAmendment(BaseApplicationDetail tdsApplication) {
    try {
      ApplicationDetail amendments =
          applicationService.getApplication(tdsApplication.getId().toString()).block();
      if (amendments != null) {
        return amendments;
      }
      log.warn(
          "Amendment application {} returned no data, clearing session state.",
          tdsApplication.getId());
    } catch (CaabApiClientException ex) {
      if (!isNotFound(ex)) {
        throw ex;
      }
      log.warn(
          "Amendment application {} no longer available, clearing session state.",
          tdsApplication.getId(),
          ex);
    }
    return null;
  }

  /**
   * Displays the case details screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model The model used to pass data to the view.
   * @return The case details view.
   */
  @GetMapping("/case/details")
  public String caseDetails(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model, HttpSession session) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve case details"));

    model.addAttribute("summary", applicationSectionDisplay);
    session.setAttribute(AMEND_CLIENT_ORIGIN, AmendClientOrigin.VIEW_CASE_DETAILS);

    return "application/case-details";
  }

  /**
   * Displays the case cost details screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model The model used to pass data to the view.
   * @return The case cost details view.
   */
  @GetMapping("/case/details/costs")
  public String caseCostDetails(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, final Model model) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve case details"));

    model.addAttribute("summary", applicationSectionDisplay);
    model.addAttribute("case", ebsCase);
    return "application/case-cost-details";
  }

  /**
   * Displays the case cost allocation screen showing how granted costs are allocated to the
   * provider and counsels.
   *
   * <p>This read-only view corresponds to the old PUI VC04a screen and shows:
   *
   * <ul>
   *   <li>Granted cost limitation
   *   <li>Current provider with their billed amount and remaining allocation
   *   <li>Counsel entries with their billed amounts and requested costs
   * </ul>
   *
   * <p>The main provider allocation is calculated by subtracting the sum of all counsel requested
   * costs from the granted cost limitation.
   *
   * @param ebsCase The case details from EBS.
   * @param model The model used to pass data to the view.
   * @return The case cost allocation view.
   */
  @GetMapping("/case/details/costs/allocation")
  public String caseCostAllocation(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, final Model model) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve case details"));

    final BigDecimal mainProviderAllocation =
        applicationService.calculateMainProviderAllocation(ebsCase);
    final BigDecimal currentProviderBilledAmount =
        applicationService.getCurrentProviderBilledAmount(ebsCase);

    model.addAttribute("summary", applicationSectionDisplay);
    model.addAttribute("case", ebsCase);
    model.addAttribute("mainProviderAllocation", mainProviderAllocation);
    model.addAttribute("currentProviderBilledAmount", currentProviderBilledAmount);
    return "application/cost-limit-allocation";
  }

  /**
   * Displays the outcome and awards screen.
   *
   * @param ebsCase The case details from EBS.
   * @param user The current user details.
   * @param model the model
   * @return The outcome and awards view.
   */
  @GetMapping("/case/outcome-and-awards")
  public String outcomeAndAwards(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      Model model) {
    final List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    final List<ProceedingOutcomeDetail> savedOutcomes =
        caseOutcomeService
            .getCaseOutcome(ebsCase.getCaseReferenceNumber(), user.getProvider().getId().intValue())
            .map(CaseOutcomeDetail::getProceedingOutcomes)
            .orElse(Collections.emptyList());

    // Build a resolved-outcome map per proceeding: prefer CAAB save, fall back to EBS.
    // The session ebsCase is never mutated so each request starts from a clean EBS baseline.
    final Map<String, ProceedingOutcomeDetail> resolvedOutcomes = new HashMap<>();
    if (proceedings != null) {
      for (final ProceedingDetail proceeding : proceedings) {
        if (proceeding.getProceedingCaseId() == null) {
          continue;
        }
        final ProceedingOutcomeDetail outcome =
            savedOutcomes.stream()
                .filter(o -> proceeding.getProceedingCaseId().equals(o.getProceedingCaseId()))
                .findFirst()
                .orElse(proceeding.getOutcome());
        resolvedOutcomes.put(proceeding.getProceedingCaseId(), outcome);
      }
    }

    model.addAttribute("proceedings", proceedings);
    model.addAttribute("resolvedOutcomes", resolvedOutcomes);
    return "application/outcome-and-awards";
  }

  /**
   * Displays the record proceeding outcome screen for a selected proceeding.
   *
   * @param ebsCase The case details from EBS.
   * @param index The zero-based index of the proceeding.
   * @param model The model used to pass data to the view.
   * @return The record proceeding outcome view.
   */
  @GetMapping("/case/outcome-and-awards/proceeding/{index}/outcome")
  public String recordProceedingOutcome(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = SELECTED_COURT, required = false)
          final CommonLookupValueDetail selectedCourt,
      @PathVariable("index") final int index,
      HttpSession session,
      Model model) {
    List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    String errorMessage = "Could not find proceeding with index: %s".formatted(index);
    Assert.notEmpty(proceedings, () -> errorMessage);
    Assert.isTrue(index >= 0 && index < proceedings.size(), () -> errorMessage);

    final ProceedingDetail proceeding = proceedings.get(index);

    // If returning from court search, restore the in-progress form data saved to session
    ProceedingOutcomeFormData formData =
        (ProceedingOutcomeFormData) session.getAttribute(PROCEEDING_OUTCOME_FORM_DATA);
    if (formData != null) {
      session.removeAttribute(PROCEEDING_OUTCOME_FORM_DATA);
    } else {
      // Load any previously saved outcome from the CAAB API; fall back to EBS outcome if absent.
      // Resolved outcome is passed directly — the session proceeding is never mutated.
      final ProceedingOutcomeDetail savedOutcome =
          loadSavedProceedingOutcome(ebsCase, user, proceeding);
      final ProceedingOutcomeDetail effectiveOutcome =
          savedOutcome != null ? savedOutcome : proceeding.getOutcome();
      formData = toProceedingOutcomeFormData(effectiveOutcome);
    }

    if (selectedCourt != null) {
      formData.setCourtCode(selectedCourt.getCode());
      formData.setCourtName(selectedCourt.getDescription());
      session.removeAttribute(SELECTED_COURT);
    }

    model.addAttribute("proceeding", proceeding);
    model.addAttribute("proceedingIndex", index);
    model.addAttribute("proceedingOutcome", formData);
    populateOutcomeDropdowns(model, proceeding);

    return "application/record-proceeding-outcome";
  }

  /**
   * Saves in-progress proceeding outcome form data to session and redirects to court search.
   *
   * @param index The zero-based index of the proceeding.
   * @param proceedingOutcome The in-progress form data to preserve.
   * @param session The current HTTP session.
   * @return Redirect to the court search screen.
   */
  @PostMapping("/case/outcome-and-awards/proceeding/{index}/outcome/court-search")
  public String recordProceedingOutcomeCourtSearch(
      @PathVariable("index") final int index,
      @ModelAttribute("proceedingOutcome") final ProceedingOutcomeFormData proceedingOutcome,
      HttpSession session) {
    session.setAttribute(PROCEEDING_OUTCOME_FORM_DATA, proceedingOutcome);
    return "redirect:/court/search?proceedingIndex=" + index;
  }

  /**
   * Handles record proceeding outcome form submission and returns to the outcome overview.
   *
   * @return Redirect to the outcome and awards overview.
   */
  @PostMapping("/case/outcome-and-awards/proceeding/{index}/outcome")
  public String recordProceedingOutcome(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable("index") final int index,
      @ModelAttribute("proceedingOutcome") final ProceedingOutcomeFormData proceedingOutcome,
      final BindingResult bindingResult,
      final Model model) {

    final ProceedingDetail proceeding = validateProceedingIndex(ebsCase, index);

    proceedingOutcomeValidator.validate(proceedingOutcome, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute("proceeding", proceeding);
      model.addAttribute("proceedingIndex", index);
      populateOutcomeDropdowns(model, proceeding);
      return "application/record-proceeding-outcome";
    }

    final ProceedingOutcomeDetail outcome = buildProceedingOutcome(proceeding, proceedingOutcome);

    try {
      caseOutcomeService.updateProceedingOutcome(
          ebsCase.getCaseReferenceNumber(),
          user.getProvider().getId().intValue(),
          outcome,
          user.getLoginId());
    } catch (CaabApiClientException ex) {
      log.warn(
          "Failed to update proceeding outcome for proceeding caseId: {}",
          proceeding.getProceedingCaseId(),
          ex);
      bindingResult.reject(
          "proceedingOutcome.update.failed",
          "We could not save the proceeding outcome. Please try again.");
      model.addAttribute("proceeding", proceeding);
      model.addAttribute("proceedingIndex", index);
      populateOutcomeDropdowns(model, proceeding);
      return "application/record-proceeding-outcome";
    }

    return "redirect:/case/outcome-and-awards";
  }

  private ProceedingDetail validateProceedingIndex(
      final ApplicationDetail ebsCase, final int index) {
    final List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    String errorMessage = "Could not find proceeding with index: %s".formatted(index);
    Assert.notEmpty(proceedings, () -> errorMessage);
    Assert.isTrue(index < proceedings.size(), () -> errorMessage);
    return proceedings.get(index);
  }

  private ProceedingOutcomeDetail loadSavedProceedingOutcome(
      final ApplicationDetail ebsCase, final UserDetail user, final ProceedingDetail proceeding) {
    return caseOutcomeService
        .getCaseOutcome(ebsCase.getCaseReferenceNumber(), user.getProvider().getId().intValue())
        .map(CaseOutcomeDetail::getProceedingOutcomes)
        .flatMap(
            outcomes ->
                outcomes.stream()
                    .filter(
                        o ->
                            proceeding.getProceedingCaseId() != null
                                && proceeding.getProceedingCaseId().equals(o.getProceedingCaseId()))
                    .findFirst())
        .orElse(null);
  }

  private ProceedingOutcomeDetail buildProceedingOutcome(
      final ProceedingDetail proceeding, final ProceedingOutcomeFormData proceedingOutcome) {
    final String proceedingCode =
        Optional.ofNullable(proceeding.getProceedingType()).map(item -> item.getId()).orElse(null);
    final Map<String, String> stageEndMap =
        buildOutcomeMap(lookupService.getStageEnds(proceedingCode, null).block());
    final Map<String, String> resultMap =
        buildOutcomeMap(lookupService.getOutcomeResults(proceedingCode, null).block());
    final Map<String, String> courtMap =
        StringUtils.hasText(proceedingOutcome.getCourtCode())
            ? buildOutcomeMap(lookupService.getCourts(proceedingOutcome.getCourtCode()).block())
            : Collections.emptyMap();

    final ProceedingOutcomeDetail outcome = new ProceedingOutcomeDetail();
    outcome.setDateOfFinalWork(
        StringUtils.hasText(proceedingOutcome.getDateOfFinalWork())
            ? DateUtils.convertToDate(proceedingOutcome.getDateOfFinalWork())
            : null);
    outcome.setProceedingCaseId(proceeding.getProceedingCaseId());
    outcome.setProceedingType(proceeding.getProceedingType());
    outcome.setMatterType(proceeding.getMatterType());
    outcome.setDescription(proceeding.getDescription());
    outcome.setStageEnd(toStringDisplayValue(proceedingOutcome.getStageEnd(), stageEndMap));
    outcome.setResolutionMethod(proceedingOutcome.getResolutionMethod());
    outcome.setResult(toStringDisplayValue(proceedingOutcome.getResult(), resultMap));
    outcome.setResultInfo(proceedingOutcome.getResultInfo());
    outcome.setAlternativeResolution(proceedingOutcome.getAlternativeResolution());
    outcome.setAdrInfo(proceedingOutcome.getAdrInfo());
    outcome.setCourtCode(proceedingOutcome.getCourtCode());
    outcome.setCourtName(
        StringUtils.hasText(proceedingOutcome.getCourtName())
            ? proceedingOutcome.getCourtName()
            : courtMap.getOrDefault(
                proceedingOutcome.getCourtCode(), proceedingOutcome.getCourtCode()));
    outcome.setOutcomeCourtCaseNo(proceedingOutcome.getOutcomeCourtCaseNo());
    outcome.setWiderBenefits(proceedingOutcome.getWiderBenefits());
    return outcome;
  }

  private uk.gov.laa.ccms.caab.model.StringDisplayValue toStringDisplayValue(
      final String code, final Map<String, String> lookups) {
    if (!StringUtils.hasText(code)) {
      return null;
    }
    return new uk.gov.laa.ccms.caab.model.StringDisplayValue()
        .id(code)
        .displayValue(lookups.getOrDefault(code, code));
  }

  private Map<String, String> buildOutcomeMap(final CommonLookupDetail lookupDetail) {
    if (lookupDetail == null || lookupDetail.getContent() == null) {
      return Collections.emptyMap();
    }

    final Map<String, String> result = new HashMap<>();
    lookupDetail.getContent().stream()
        .filter(Objects::nonNull)
        .forEach(item -> result.put(item.getCode(), item.getDescription()));
    return result;
  }

  private Map<String, String> buildOutcomeMap(final StageEndLookupDetail lookupDetail) {
    if (lookupDetail == null || lookupDetail.getContent() == null) {
      return Collections.emptyMap();
    }

    final Map<String, String> result = new HashMap<>();
    lookupDetail.getContent().stream()
        .filter(Objects::nonNull)
        .forEach(item -> result.put(item.getStageEnd(), item.getDescription()));
    return result;
  }

  private Map<String, String> buildOutcomeMap(final OutcomeResultLookupDetail lookupDetail) {
    if (lookupDetail == null || lookupDetail.getContent() == null) {
      return Collections.emptyMap();
    }

    final Map<String, String> result = new HashMap<>();
    lookupDetail.getContent().stream()
        .filter(Objects::nonNull)
        .forEach(item -> result.put(item.getOutcomeResult(), item.getOutcomeResultDescription()));
    return result;
  }

  private void populateOutcomeDropdowns(final Model model, final ProceedingDetail proceeding) {
    final String proceedingCode =
        Optional.ofNullable(proceeding.getProceedingType()).map(item -> item.getId()).orElse(null);

    final List<StageEndLookupValueDetail> stageEnds =
        Optional.ofNullable(lookupService.getStageEnds(proceedingCode, null).block())
            .map(StageEndLookupDetail::getContent)
            .orElse(Collections.emptyList());
    model.addAttribute(
        "stageEnds",
        stageEnds.stream()
            .map(stageEnd -> option(stageEnd.getStageEnd(), stageEnd.getDescription()))
            .toList());

    final List<OutcomeResultLookupValueDetail> results =
        Optional.ofNullable(lookupService.getOutcomeResults(proceedingCode, null).block())
            .map(OutcomeResultLookupDetail::getContent)
            .orElse(Collections.emptyList());
    model.addAttribute(
        "results",
        results.stream()
            .map(result -> option(result.getOutcomeResult(), result.getOutcomeResultDescription()))
            .toList());

    model.addAttribute(
        "resolutionMethods",
        Optional.ofNullable(
                lookupService.getCommonValues(COMMON_VALUE_OUTCOME_RESOLUTION_METHOD).block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .map(item -> option(item.getCode(), item.getDescription()))
            .toList());
    model.addAttribute(
        "alternativeDisputeResolutions",
        Optional.ofNullable(lookupService.getCommonValues(COMMON_VALUE_OUTCOME_ADR).block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .map(item -> option(item.getCode(), item.getDescription()))
            .toList());
    model.addAttribute(
        "widerBenefitsOptions",
        Optional.ofNullable(lookupService.getCommonValues(COMMON_VALUE_WIDER_BENEFITS).block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .map(item -> option(item.getCode(), item.getDescription()))
            .toList());
  }

  private static ProceedingOutcomeFormData toProceedingOutcomeFormData(
      final ProceedingOutcomeDetail outcome) {
    final ProceedingOutcomeFormData formData = new ProceedingOutcomeFormData();

    if (outcome == null) {
      return formData;
    }

    formData.setDateOfFinalWork(
        Optional.ofNullable(outcome.getDateOfFinalWork())
            .map(date -> convertToComponentDate(date))
            .orElse(null));
    formData.setStageEnd(
        Optional.ofNullable(outcome.getStageEnd()).map(item -> item.getId()).orElse(null));
    formData.setResolutionMethod(outcome.getResolutionMethod());
    formData.setResult(
        Optional.ofNullable(outcome.getResult()).map(item -> item.getId()).orElse(null));
    formData.setResultInfo(outcome.getResultInfo());
    formData.setAlternativeResolution(outcome.getAlternativeResolution());
    formData.setAdrInfo(outcome.getAdrInfo());
    formData.setCourtCode(outcome.getCourtCode());
    formData.setCourtName(outcome.getCourtName());
    formData.setOutcomeCourtCaseNo(outcome.getOutcomeCourtCaseNo());
    formData.setWiderBenefits(outcome.getWiderBenefits());

    return formData;
  }

  private static Map<String, String> option(final String code, final String description) {
    return Map.of(
        "code", Optional.ofNullable(code).orElse(""),
        "description", Optional.ofNullable(description).orElse(""));
  }

  /**
   * Returns a display object containing an other party within a case.
   *
   * @param ebsCase The case details from EBS.
   * @param index Index number of the OtherParty within the ebsCase.
   * @param model The model used to pass data to the view.
   * @return The case details other party view.
   */
  @GetMapping("/case/details/other-party/{index}")
  public String caseDetailsOtherParty(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @PathVariable("index") final int index,
      Model model,
      HttpSession session) {

    if (Objects.isNull(ebsCase.getOpponents()) || index >= ebsCase.getOpponents().size()) {
      throw new CaabApplicationException("Could not find opponent with index " + index);
    }

    final OpponentDetail opponentDetail = ebsCase.getOpponents().get(index);

    if ("Individual".equals(opponentDetail.getType())) {
      final IndividualDetailsSectionDisplay opponentDisplay =
          applicationService.getIndividualDetailsSectionDisplay(opponentDetail);
      model.addAttribute("otherParty", opponentDisplay);
      return "application/case-details-other-party";
    } else if ("Organisation".equals(opponentDetail.getType())) {
      final OrganisationDetailsSectionDisplay opponentDisplay =
          applicationService.getOrganisationDetailsSectionDisplay(opponentDetail);
      model.addAttribute("otherPartyOrganisation", opponentDisplay);
      return "application/case-details-other-party-organisation";
    }

    throw new CaabApplicationException("Unknown Opponent Type");
  }

  /**
   * Displays the prior authority details for a given case. Retrieves a specific prior authority
   * detail using the provided index and adds it to the model to be displayed in the view.
   *
   * @param ebsCase The case details retrieved from the session.
   * @param index The zero-based index of the prior authority to be retrieved from the case details.
   * @param model The model used to pass data to the view.
   * @return The view name for the prior authority review page.
   * @throws IllegalArgumentException if the list of prior authorities is empty or the specified
   *     index is invalid.
   */
  @GetMapping("/case/details/prior-authority/{index}")
  public String getCaseDetailsView(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @PathVariable final int index,
      Model model) {
    List<PriorAuthorityDetail> priorAuthorities = ebsCase.getPriorAuthorities();
    String errorMessage = "Could not find prior authority with index: %s".formatted(index);
    Assert.notEmpty(priorAuthorities, () -> errorMessage);
    Assert.isTrue(index < priorAuthorities.size(), () -> errorMessage);

    Map<PriorAuthorityGroup, List<ReferenceDataItemDetail>> groupedPriorAuthorityItems =
        PriorAuthorityUtils.groupItems(priorAuthorities.get(index));
    model.addAttribute("groupedItems", groupedPriorAuthorityItems);
    model.addAttribute("priorAuthority", priorAuthorities.get(index));
    return "application/prior-authority-review";
  }

  /**
   * Displays the details for an individual proceeding.
   *
   * @param ebsCase The case details from EBS.
   * @param index Index number of the Proceeding within the ebsCase.
   * @param model The model used to pass data to the view.
   * @return The proceeding details view.
   */
  @GetMapping("/case/details/proceeding/{index}")
  public String caseDetailsProceeding(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION) @Nullable final ApplicationDetail amendments,
      @PathVariable("index") final int index,
      Model model) {

    List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    String errorMessage = "Could not find proceeding with index: %s".formatted(index);
    Assert.notEmpty(proceedings, () -> errorMessage);
    Assert.isTrue(index < proceedings.size(), () -> errorMessage);

    final ProceedingDetail proceedingDetail = ebsCase.getProceedings().get(index);

    final String proceedingStatus = getProceedingStatus(proceedingDetail, amendments, ebsCase);

    model.addAttribute("proceeding", proceedingDetail);
    model.addAttribute("proceedingStatus", proceedingStatus);
    model.addAttribute("applicationType", ebsCase.getApplicationType().getId());
    model.addAttribute("categoryOfLaw", ebsCase.getCategoryOfLaw().getDisplayValue());
    return "application/proceeding-details";
  }

  /**
   * Handles the request to abandon amendments for a specific case. This method is triggered by a
   * GET request to display the confirmation page for abandoning amendments.
   *
   * @param amendments the application details for the current case, retrieved from the session
   *     attribute
   * @return a string representing the view name for confirming the abandonment of amendments
   */
  @GetMapping("/case/amendment/abandon")
  public String handleAbandon(
      @SessionAttribute(APPLICATION) @Nullable final ApplicationDetail amendments) {
    Assert.notNull(amendments, "Amendments must not be null");
    log.info("Abandoning amendments requested for application id {}", amendments.getId());
    return "application/amendment-remove";
  }

  /**
   * Handles the confirmation of abandoning amendments for a specific case. This method processes
   * the request to abandon any ongoing amendments for the given case and logs the associated
   * information.
   *
   * @param amendments the application details for the current case, retrieved from the session
   *     attribute
   * @param user the user details of the currently logged-in user, retrieved from the session
   *     attribute
   * @return a string representing the view name to be displayed after the amendments are abandoned
   */
  @PostMapping("/case/amendment/abandon")
  public String handleAbandon(
      @SessionAttribute(APPLICATION) @Nullable final ApplicationDetail amendments,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      final HttpSession httpSession) {
    Assert.notNull(amendments, "Amendments must not be null");
    log.info("Abandoning amendments for case id {}", amendments.getId());
    applicationService.abandonApplication(amendments, user);

    clearAmendmentSession(httpSession);

    return "redirect:/case/overview";
  }

  /**
   * Displays the general details edit page for an amendment case. This method is used to
   *
   * @param tdsApplication the application details for the current case, retrieved from the session
   * @return the view name for editing general details of the case
   */
  @GetMapping("/case/amendment/edit-general-details")
  public String editGeneralDetails(
      @SessionAttribute(APPLICATION) final ApplicationDetail tdsApplication) {
    log.info("Editing general details for case id {}", tdsApplication.getId());
    Assert.notNull(tdsApplication.getApplicationType(), "TDS Application type must not be null");

    if (APP_TYPE_EMERGENCY.equals(tdsApplication.getApplicationType().getId())
        || APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equals(tdsApplication.getApplicationType().getId())
        || APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equals(
            tdsApplication.getApplicationType().getId())) {
      return "redirect:/amendments/edit-delegated-functions";
    }

    return "redirect:/amendments/sections/linked-cases";
  }

  private static List<AvailableAction> getAvailableActions(
      ApplicationDetail ebsCase,
      boolean amendment,
      ApplicationDetail amendments,
      @SessionAttribute(CASE_REFERENCE_NUMBER) String caseReferenceNumber) {

    final Set<String> caseAvailableFunctions;
    if (ebsCase.getAvailableFunctions() != null && !ebsCase.getAvailableFunctions().isEmpty()) {
      caseAvailableFunctions = Set.copyOf(ebsCase.getAvailableFunctions());
    } else {
      caseAvailableFunctions = Collections.emptySet();
    }

    boolean openAmendment = amendment || (hasEbsAmendments(ebsCase) && amendments != null);

    return ActionViewHelper.getAllAvailableActions(openAmendment).stream()
        .filter(availableAction -> caseAvailableFunctions.contains(availableAction.actionCode()))
        .map(action -> enhanceActionUrl(action, caseReferenceNumber))
        .toList();
  }

  private static boolean hasEbsAmendments(ApplicationDetail ebsCase) {
    return ebsCase.getAmendmentProceedingsInEbs() != null
        && !ebsCase.getAmendmentProceedingsInEbs().isEmpty();
  }

  private void setReturnDetails(Model model, String notificationId, HttpServletRequest request) {
    String referer = request.getHeader("referer");
    String returnTo =
        referer != null && referer.contains("notifications") ? "notification" : "caseSearchResults";
    model.addAttribute("returnTo", returnTo);
    model.addAttribute(NOTIFICATION_ID, notificationId);
  }

  private void clearAmendmentSession(HttpSession session) {
    session.removeAttribute(APPLICATION_SUMMARY);
    session.removeAttribute(APPLICATION_ID);
    session.removeAttribute(APPLICATION);
    session.removeAttribute(APPLICATION_COSTS);
    session.removeAttribute(APPLICATION_FORM_DATA);
  }

  private boolean isNotFound(CaabApiClientException ex) {
    if (ex.hasHttpStatus(HttpStatus.NOT_FOUND)) {
      return true;
    }
    Throwable cause = ex.getCause();
    return cause instanceof WebClientResponseException wcre
        && wcre.getStatusCode() == HttpStatus.NOT_FOUND;
  }

  private void setProceedingDisplayStatuses(
      ApplicationDetail ebsCase, ApplicationDetail amendments) {
    List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    if (proceedings == null || proceedings.isEmpty()) {
      return;
    }

    for (ProceedingDetail proceeding : proceedings) {
      proceeding.getStatus().setDisplayValue(getProceedingStatus(proceeding, amendments, ebsCase));
    }

    List<ProceedingDetail> amendmentProceedingsInEbs = ebsCase.getAmendmentProceedingsInEbs();
    if (amendmentProceedingsInEbs != null) {
      for (ProceedingDetail proceeding : amendmentProceedingsInEbs) {
        proceeding.getStatus().setDisplayValue(CaseProceedingDisplayStatus.SUBMITTED.getStatus());
      }
    }
  }

  private String getProceedingStatus(
      ProceedingDetail proceeding, ApplicationDetail amendments, ApplicationDetail ebsCase) {
    if (proceeding.getStatus() == null) {
      return null;
    }
    String statusId = proceeding.getStatus().getId();
    return switch (statusId.toUpperCase()) {
      case "LIVE" -> handleLiveProceeding(proceeding, amendments, ebsCase);
      case "DRAFT" -> CaseProceedingDisplayStatus.SUBMITTED.getStatus();
      default -> proceeding.getStatus().getDisplayValue();
    };
  }

  private String handleLiveProceeding(
      ProceedingDetail proceeding, ApplicationDetail amendments, ApplicationDetail ebsCase) {
    if (proceeding.getOutcome() != null) {
      return CaseProceedingDisplayStatus.OUTCOME.getStatus();
    }
    ProceedingOutcomeDetail draftOutcome =
        getProceedingOutcome(amendments, proceeding.getProceedingCaseId());
    return draftOutcome != null
        ? CaseProceedingDisplayStatus.OUTCOME.getStatus()
        : ebsCase.getStatus().getDisplayValue();
  }

  private ProceedingOutcomeDetail getProceedingOutcome(
      ApplicationDetail amendments, String proceedingCaseId) {
    if (amendments == null || amendments.getProceedings() == null) {
      return null;
    }
    return amendments.getProceedings().stream()
        .filter(
            proceeding ->
                proceeding.getProceedingCaseId() != null
                    && proceeding.getProceedingCaseId().equals(proceedingCaseId))
        .findFirst()
        .map(ProceedingDetail::getOutcome)
        .orElse(null);
  }

  private ApplicationDetail getRecentlySubmittedAmendment(
      ApplicationDetail ebsCase, UserDetail user) {
    if (ebsCase == null) {
      return null;
    }

    try {
      BaseApplicationDetail tdsSummary =
          applicationService.getTdsApplicationSummary(ebsCase.getCaseReferenceNumber(), user);

      if (tdsSummary != null) {
        return applicationService.getApplication(tdsSummary.getId().toString()).block();
      }
    } catch (Exception e) {
      log.debug(
          "No recent submitted amendment found for case {}", ebsCase.getCaseReferenceNumber());
    }
    return null;
  }
}
