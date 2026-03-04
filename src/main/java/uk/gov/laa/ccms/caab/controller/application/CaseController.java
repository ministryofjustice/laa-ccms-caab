package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AMEND_CLIENT_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.proceeding.CaseProceedingDisplayStatus;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.AmendClientOrigin;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.AvailableAction;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.util.view.ActionViewHelper;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to cases. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class CaseController {

  private final ApplicationService applicationService;
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
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_SUMMARY) @Nullable final BaseApplicationDetail tdsApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(NOTIFICATION_ID) @Nullable final String notificationId,
      Model model,
      HttpSession session,
      HttpServletRequest request) {

    setReturnDetails(model, notificationId, request);

    BaseApplicationDetail resolvedTds = getTdsApplication(tdsApplication, ebsCase, user, session);
    AmendmentState amendmentState = resolveAmendmentState(ebsCase, resolvedTds, session);
    ApplicationDetail amendments = amendmentState.getAmendments();

    setProceedingDisplayStatuses(ebsCase, amendments);

    model.addAttribute("searchUrl", Objects.toString(session.getAttribute(SEARCH_URL), ""));
    model.addAttribute("case", ebsCase);
    final boolean isAmendment = amendmentState.isAmendment();
    model.addAttribute("isAmendment", isAmendment);
    model.addAttribute("availableActions", getAvailableActions(ebsCase, isAmendment, amendments));
    model.addAttribute("hasEbsAmendments", hasEbsAmendments(ebsCase));
    model.addAttribute(
        "draftProceedings",
        isAmendment ? amendments.getProceedings() : ebsCase.getAmendmentProceedingsInEbs());
    model.addAttribute("draftCosts", isAmendment ? amendments.getCosts() : ebsCase.getCosts());

    session.setAttribute(CASE_REFERENCE_NUMBER, ebsCase.getCaseReferenceNumber());
    session.setAttribute(APPLICATION, amendments);
    session.setAttribute(AMEND_CLIENT_ORIGIN, AmendClientOrigin.CASE_OVERVIEW);

    return "application/case-overview";
  }

  private BaseApplicationDetail getTdsApplication(
      @Nullable BaseApplicationDetail tdsApplication,
      ApplicationDetail ebsCase,
      UserDetail user,
      HttpSession session) {
    if (tdsApplication != null) {
      return tdsApplication;
    }

    BaseApplicationDetail fetched = getTdsApplicationSummary(ebsCase, user);
    if (fetched != null) {
      session.setAttribute(APPLICATION_SUMMARY, fetched);
    }

    return fetched;
  }

  private AmendmentState resolveAmendmentState(
      ApplicationDetail ebsCase,
      @Nullable BaseApplicationDetail tdsApplication,
      HttpSession session) {
    if (tdsApplication == null || !applicationService.isAmendment(ebsCase, tdsApplication)) {
      return new AmendmentState(null, false);
    }

    ApplicationDetail amendments = resolveAmendment(tdsApplication, session);
    if (amendments == null) {
      return new AmendmentState(null, false);
    }

    return new AmendmentState(amendments, true);
  }

  private ApplicationDetail resolveAmendment(
      BaseApplicationDetail tdsApplication, HttpSession session) {
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
    clearAmendmentSession(session);
    return null;
  }

  private BaseApplicationDetail getTdsApplicationSummary(
      ApplicationDetail ebsCase, UserDetail user) {
    if (ebsCase.getCaseReferenceNumber() == null) {
      return null;
    }

    CaseSearchCriteria criteria = new CaseSearchCriteria();
    criteria.setCaseReference(ebsCase.getCaseReferenceNumber());

    return Optional.ofNullable(applicationService.getTdsApplications(criteria, user, 0, 1))
        .map(ApplicationDetails::getContent)
        .filter(c -> !c.isEmpty())
        .map(c -> c.get(0))
        .orElse(null);
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

    // Store the expensive object in the session for reuse
    session.setAttribute("applicationSectionDisplay", applicationSectionDisplay);
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
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute("applicationSectionDisplay")
          final ApplicationSectionDisplay applicationSectionDisplay,
      final Model model) {

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
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute("applicationSectionDisplay")
          final ApplicationSectionDisplay applicationSectionDisplay,
      final Model model) {

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

    if (APP_TYPE_EMERGENCY.equals(tdsApplication.getApplicationType().getId())) {
      return "redirect:/amendments/edit-delegated-functions";
    }

    return "redirect:/amendments/sections/linked-cases";
  }

  private static List<AvailableAction> getAvailableActions(
      ApplicationDetail ebsCase, boolean amendment, ApplicationDetail amendments) {

    if (ebsCase.getAvailableFunctions() == null || ebsCase.getAvailableFunctions().isEmpty()) {
      return Collections.emptyList();
    }

    Set<String> caseAvailableFunctions = Set.copyOf(ebsCase.getAvailableFunctions());
    boolean openAmendment = amendment || (hasEbsAmendments(ebsCase) && amendments != null);

    return ActionViewHelper.getAllAvailableActions(openAmendment).stream()
        .filter(availableAction -> caseAvailableFunctions.contains(availableAction.actionCode()))
        .toList();
  }

  private static boolean hasEbsAmendments(ApplicationDetail ebsCase) {
    return ebsCase.getAmendmentProceedingsInEbs() != null
        && !ebsCase.getAmendmentProceedingsInEbs().isEmpty();
  }

  private static final class AmendmentState {
    private final ApplicationDetail amendments;
    private final boolean isAmendment;

    private AmendmentState(ApplicationDetail amendments, boolean isAmendment) {
      this.amendments = amendments;
      this.isAmendment = isAmendment;
    }

    private ApplicationDetail getAmendments() {
      return amendments;
    }

    private boolean isAmendment() {
      return isAmendment;
    }
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
}
