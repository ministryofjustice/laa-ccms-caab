package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.AMENDMENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_AMENDMENTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController.NOTIFICATION_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.proceeding.CaseProceedingDisplayStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AvailableAction;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
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

/**
 * Controller responsible for handling requests related to cases.
 */
@RequiredArgsConstructor
@Controller
@Slf4j
public class CaseController {

  private final ApplicationService applicationService;

  /**
   * Displays the case overview screen.
   *
   * @param ebsCase         The case details from EBS.
   * @param tdsApplication  The application details from TDS, if available.
   * @param notificationId  The ID of the notification, if coming from a notification page.
   * @return The case overview view.
   */
  @GetMapping("/case/overview")
  public String caseOverview(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(AMENDMENT) @Nullable final BaseApplicationDetail tdsApplication,
      @SessionAttribute(NOTIFICATION_ID) @Nullable final String notificationId,
      Model model,
      HttpSession session,
      HttpServletRequest request) {

    boolean isAmendment = applicationService.isAmendment(ebsCase, tdsApplication);

    setReturnDetails(model, notificationId, request);

    ApplicationDetail amendments = null;
    List<ProceedingDetail> draftProceedings = new ArrayList<>();
    CostStructureDetail draftCosts = null;
    if (isAmendment) {
      amendments = applicationService.getApplication(tdsApplication.getId().toString()).block();
      draftProceedings = amendments != null
          ? amendments.getProceedings() : ebsCase.getAmendmentProceedingsInEbs();
      draftCosts = amendments != null
          ? amendments.getCosts() : ebsCase.getCosts();
    }
    setProceedingDisplayStatuses(ebsCase, amendments);

    List<AvailableAction> availableActions = getAvailableActions(ebsCase, isAmendment);

    model.addAttribute("case", ebsCase);
    model.addAttribute("isAmendment", isAmendment);
    model.addAttribute("availableActions", availableActions);
    model.addAttribute("hasEbsAmendments", hasEbsAmendments(ebsCase));
    model.addAttribute("draftProceedings", draftProceedings);
    model.addAttribute("draftCosts", draftCosts);
    session.setAttribute(CASE_REFERENCE_NUMBER, ebsCase.getCaseReferenceNumber());
    session.setAttribute(CASE_AMENDMENTS, amendments);
    return "application/case-overview";
  }

  /**
   * Displays the case details screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model   The model used to pass data to the view.
   * @return The case details view.
   */

  @GetMapping("/case/details")
  public String caseDetails(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      Model model,
      HttpSession session) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve case details"));

    model.addAttribute("summary", applicationSectionDisplay);

    return "application/case-details";
  }

  /**
   * Returns a display object containing an other party within a case.
   *
   * @param ebsCase The case details from EBS.
   * @param index   Index number of the OtherParty within the ebsCase.
   * @param model   The model used to pass data to the view.
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

    if (opponentDetail.getType().equals("Individual")) {
      final IndividualDetailsSectionDisplay opponentDisplay =
          applicationService.getIndividualDetailsSectionDisplay(opponentDetail);
      model.addAttribute("otherParty", opponentDisplay);
      return "application/case-details-other-party";
    } else if (opponentDetail.getType().equals("Organisation")) {
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
   * @param index   The zero-based index of the prior authority to be retrieved from the case
   *                details.
   * @param model   The model used to pass data to the view.
   * @return The view name for the prior authority review page.
   * @throws IllegalArgumentException if the list of prior authorities is empty or the specified
   *                                  index is invalid.
   */
  @GetMapping("/case/details/prior-authority/{index}")
  public String getCaseDetailsView(@SessionAttribute(CASE) final ApplicationDetail ebsCase,
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
   * @param ebsCase   The case details from EBS.
   * @param index     Index number of the Proceeding within the ebsCase.
   * @param model     The model used to pass data to the view.
   * @return The proceeding details view.
   */
  @GetMapping("/case/details/proceeding/{index}")
  public String caseDetailsProceeding(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(CASE_AMENDMENTS) @Nullable final ApplicationDetail amendments,
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
   * @param ebsCase the application details for the current case, retrieved from the session
   *                attribute
   * @return a string representing the view name for confirming the abandonment of amendments
   */
  @GetMapping("/case/amendment/abandon")
  public String handleAbandon(@SessionAttribute(CASE) final ApplicationDetail ebsCase) {
    log.info("Abandoning amendments requested for case id {}", ebsCase.getId());
    return "application/amendment-remove";
  }

  /**
   * Handles the confirmation of abandoning amendments for a specific case. This method processes
   * the request to abandon any ongoing amendments for the given case and logs the associated
   * information.
   *
   * @param ebsCase the application details for the current case, retrieved from the session
   *                attribute
   * @param user    the user details of the currently logged-in user, retrieved from the session
   *                attribute
   * @return a string representing the view name to be displayed after the amendments are abandoned
   */
  @PostMapping("/case/amendment/abandon")
  public String handleAbandon(@SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) UserDetail user) {
    log.info("Abandoning amendments for case id {}", ebsCase.getId());
    applicationService.abandonApplication(ebsCase, user);
    return "home";
  }

  private static List<AvailableAction> getAvailableActions(ApplicationDetail ebsCase,
      boolean amendment) {

    if (ebsCase.getAvailableFunctions() == null
        || ebsCase.getAvailableFunctions().isEmpty()) {
      return Collections.emptyList();
    }

    Set<String> caseAvailableFunctions = Set.copyOf(ebsCase.getAvailableFunctions());
    boolean openAmendment = amendment || hasEbsAmendments(ebsCase);

    return ActionViewHelper.getAllAvailableActions(openAmendment).stream()
        .filter(availableAction -> caseAvailableFunctions.contains(availableAction.actionCode()))
        .toList();
  }

  private static boolean hasEbsAmendments(ApplicationDetail ebsCase) {
    return ebsCase.getAmendmentProceedingsInEbs() != null
        && !ebsCase.getAmendmentProceedingsInEbs().isEmpty();
  }

  private void setReturnDetails(Model model, String notificationId, HttpServletRequest request) {
    String referer = request.getHeader("referer");
    String returnTo = referer != null && referer.contains("notifications")
        ? "notification" : "caseSearchResults";
    model.addAttribute("returnTo", returnTo);
    model.addAttribute(NOTIFICATION_ID, notificationId);
  }

  private void setProceedingDisplayStatuses(ApplicationDetail ebsCase,
      ApplicationDetail amendments) {
    List<ProceedingDetail> proceedings = ebsCase.getProceedings();
    if (proceedings == null || proceedings.isEmpty()) {
      return;
    }

    for (ProceedingDetail proceeding : proceedings) {
      proceeding.getStatus().setDisplayValue(
          getProceedingStatus(proceeding, amendments, ebsCase)
      );
    }

    List<ProceedingDetail> amendmentProceedingsInEbs = ebsCase.getAmendmentProceedingsInEbs();
    if (amendmentProceedingsInEbs != null) {
      for (ProceedingDetail proceeding : amendmentProceedingsInEbs) {
        proceeding.getStatus().setDisplayValue(CaseProceedingDisplayStatus.SUBMITTED.getStatus());
      }
    }
  }

  private String getProceedingStatus(ProceedingDetail proceeding, ApplicationDetail amendments,
      ApplicationDetail ebsCase) {
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

  private String handleLiveProceeding(ProceedingDetail proceeding, ApplicationDetail amendments,
      ApplicationDetail ebsCase) {
    if (proceeding.getOutcome() != null) {
      return CaseProceedingDisplayStatus.OUTCOME.getStatus();
    }
    ProceedingOutcomeDetail draftOutcome =
        getProceedingOutcome(amendments, proceeding.getProceedingCaseId());
    return draftOutcome != null
        ? CaseProceedingDisplayStatus.OUTCOME.getStatus()
        : ebsCase.getStatus().getDisplayValue();
  }

  private ProceedingOutcomeDetail getProceedingOutcome(ApplicationDetail amendments,
      String proceedingCaseId) {
    if (amendments == null || amendments.getProceedings() == null) {
      return null;
    }
    return amendments.getProceedings().stream()
        .filter(proceeding -> proceeding.getProceedingCaseId() != null
            && proceeding.getProceedingCaseId().equals(proceedingCaseId))
        .findFirst()
        .map(ProceedingDetail::getOutcome)
        .orElse(null);
  }
}
