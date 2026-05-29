package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EMERGENCY_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_SCOPE_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SCOPE_LIMITATION_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.scopelimitation.ScopeLimitationDetailsValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for adding, editing and removing scope limitations for a proceeding.
 *
 * <p>The controller stores the in-progress scope limitation flow in the session while users select
 * a limitation, review or edit its wording, and confirm the change back onto the proceeding.
 */
@Controller
@RequiredArgsConstructor
@SessionAttributes(
    value = {
      APPLICATION,
      CURRENT_PROCEEDING,
      PROCEEDING_FLOW_FORM_DATA,
      PROCEEDING_SCOPE_LIMITATIONS,
      SCOPE_LIMITATION_FLOW_FORM_DATA,
      CURRENT_SCOPE_LIMITATION
    })
@SuppressWarnings("unchecked")
public class ScopeLimitationController {

  private static final String ACTION_EDIT = "edit";
  private static final String ACTION_ADD = "add";

  private final ApplicationService applicationService;
  private final LookupService lookupService;
  private final ScopeLimitationDetailsValidator scopeLimitationDetailsValidator;
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;

  /**
   * Starts editing an existing scope limitation.
   *
   * <p>For newly added proceedings, the path variable is treated as the index of the scope
   * limitation in the session list. For existing proceedings, it is treated as the stored scope
   * limitation identifier.
   *
   * @param caseContext the current case context.
   * @param scopeLimitationId the scope limitation index or stored identifier.
   * @param proceedingFlow the current proceeding flow data.
   * @param model the model used to update session-backed attributes.
   * @param session the HTTP session containing proceeding state.
   * @return a redirect to the scope limitation confirmation page.
   */
  @GetMapping("/{caseContext}/proceedings/scope-limitations/{scope-limitation-id}/edit")
  public String scopeLimitationEdit(
      @PathVariable("caseContext") final CaseContext caseContext,
      @PathVariable("scope-limitation-id") final Integer scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      final Model model,
      final HttpSession session) {

    if (ACTION_ADD.equals(proceedingFlow.getAction())) {
      final List<ScopeLimitationDetail> scopeLimitations =
          (List<ScopeLimitationDetail>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      final ScopeLimitationDetail scopeLimitation = scopeLimitations.get(scopeLimitationId);
      model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);

      final ScopeLimitationFlowFormData scopeLimitationFlow =
          proceedingAndCostsMapper.toScopeLimitationFlow(scopeLimitation);
      scopeLimitationFlow.setScopeLimitationIndex(scopeLimitationId);

      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
    } else {
      final ProceedingDetail proceeding =
          (ProceedingDetail) session.getAttribute(CURRENT_PROCEEDING);
      final ScopeLimitationDetail scopeLimitation =
          proceeding.getScopeLimitations().stream()
              .filter(sl -> sl.getId().equals(scopeLimitationId))
              .findFirst()
              .orElseThrow();

      model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);

      final ScopeLimitationFlowFormData scopeLimitationFlow =
          proceedingAndCostsMapper.toScopeLimitationFlow(scopeLimitation);

      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
    }

    return "redirect:/%s/proceedings/scope-limitations/confirm"
        .formatted(caseContext.getPathValue());
  }

  /**
   * Displays the scope limitation selection page.
   *
   * @param caseContext the current case context.
   * @param scopeLimitationAction the action being performed, such as add or edit.
   * @param application the current application.
   * @param proceedingFlow the current proceeding flow data.
   * @param model the model used to render the page.
   * @param session the HTTP session containing any in-progress scope limitation flow.
   * @return the scope limitation details view.
   */
  @GetMapping("/{caseContext}/proceedings/scope-limitations/{action}/details")
  public String scopeLimitationDetails(
      @PathVariable("caseContext") final CaseContext caseContext,
      @PathVariable("action") final String scopeLimitationAction,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      final Model model,
      final HttpSession session) {

    ScopeLimitationFlowFormData scopeLimitationFlow =
        new ScopeLimitationFlowFormData(scopeLimitationAction);

    if (ACTION_EDIT.equals(scopeLimitationAction)) {
      scopeLimitationFlow =
          (ScopeLimitationFlowFormData) session.getAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA);
    }

    model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
    model.addAttribute("scopeLimitationDetails", scopeLimitationFlow.getScopeLimitationDetails());

    populateScopeLimitationDropdown(model, application, proceedingFlow);

    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return "application/proceedings-scope-limitations-details";
  }

  /**
   * Validates and processes the selected scope limitation.
   *
   * <p>When validation succeeds, the selected reference data is resolved into a {@link
   * ScopeLimitationDetail} and stored as the current scope limitation for confirmation.
   *
   * @param caseContext the current case context.
   * @param application the current application.
   * @param proceedingFlow the current proceeding flow data.
   * @param scopeLimitationFlow the in-progress scope limitation flow.
   * @param scopeLimitationDetails the submitted scope limitation details.
   * @param model the model used to render validation errors or update session attributes.
   * @param bindingResult the validation result.
   * @return a redirect to confirmation, or the details view when validation fails.
   */
  @PostMapping("/{caseContext}/proceedings/scope-limitations/{action}/details")
  public String scopeLimitationDetailsPost(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA)
          final ScopeLimitationFlowFormData scopeLimitationFlow,
      @ModelAttribute("scopeLimitationDetails")
          final ScopeLimitationFormDataDetails scopeLimitationDetails,
      final Model model,
      final BindingResult bindingResult) {

    scopeLimitationDetailsValidator.validate(scopeLimitationDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateScopeLimitationDropdown(model, application, proceedingFlow);
      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
      return "application/proceedings-scope-limitations-details";
    }

    scopeLimitationFlow.setScopeLimitationDetails(scopeLimitationDetails);
    populateScopeLimitationDetails(model, application, proceedingFlow, scopeLimitationFlow);

    return "redirect:/%s/proceedings/scope-limitations/confirm"
        .formatted(caseContext.getPathValue());
  }

  /**
   * Displays the scope limitation confirmation page.
   *
   * @param caseContext the current case context.
   * @param scopeLimitation the current scope limitation being reviewed.
   * @param scopeLimitationFlow the in-progress scope limitation flow.
   * @param model the model used to render the page.
   * @return the scope limitation confirmation view.
   */
  @GetMapping("/{caseContext}/proceedings/scope-limitations/confirm")
  public String scopeLimitationConfirm(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(value = APPLICATION, required = false) final ApplicationDetail application,
      @SessionAttribute(value = PROCEEDING_FLOW_FORM_DATA, required = false)
          final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(CURRENT_SCOPE_LIMITATION) final ScopeLimitationDetail scopeLimitation,
      @SessionAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA)
          final ScopeLimitationFlowFormData scopeLimitationFlow,
      final Model model) {

    patchScopeLimitationNonDefaultWordingRequirement(application, proceedingFlow, scopeLimitation);

    model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
    model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

    return "application/proceedings-scope-limitations-confirm";
  }

  /**
   * Confirms the current scope limitation and applies it to the proceeding.
   *
   * <p>For proceedings being created in-session, the scope limitation list is updated directly in
   * the session. For existing proceedings, the proceeding is updated through the application
   * service and refreshed afterwards.
   *
   * @param caseContext the current case context.
   * @param scopeLimitation the submitted scope limitation, including editable wording.
   * @param bindingResult the validation result.
   * @param proceedingFlow the current proceeding flow data.
   * @param scopeLimitationFlow the in-progress scope limitation flow.
   * @param user the current user.
   * @param model the model used to update session-backed attributes.
   * @param session the HTTP session containing proceeding state.
   * @return a redirect to the proceeding confirmation page, or the confirmation view when
   *     validation fails.
   */
  @PostMapping("/{caseContext}/proceedings/scope-limitations/confirm")
  public String scopeLimitationConfirmPost(
      @PathVariable("caseContext") final CaseContext caseContext,
      @ModelAttribute(value = CURRENT_SCOPE_LIMITATION, binding = false)
          final ScopeLimitationDetail scopeLimitation,
      final BindingResult bindingResult,
      @RequestParam(value = "scopeLimitationWording", required = false)
          final String scopeLimitationWording,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA)
          final ScopeLimitationFlowFormData scopeLimitationFlow,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model,
      final HttpSession session) {

    if (!Boolean.FALSE.equals(scopeLimitation.getNonDefaultWordingReqd())
        && scopeLimitationWording != null) {
      scopeLimitation.setScopeLimitationWording(scopeLimitationWording);
    }

    scopeLimitationDetailsValidator.validateScopeLimitationWording(scopeLimitation, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      return "application/proceedings-scope-limitations-confirm";
    }

    if (ACTION_ADD.equals(proceedingFlow.getAction())) {
      final List<ScopeLimitationDetail> scopeLimitations =
          (List<ScopeLimitationDetail>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

      if (scopeLimitationFlow.getScopeLimitationIndex() == null) {
        scopeLimitations.add(scopeLimitation);
      } else {
        final int index = scopeLimitationFlow.getScopeLimitationIndex();
        if (index >= 0 && index < scopeLimitations.size()) {
          scopeLimitations.set(index, scopeLimitation);
        } else {
          throw new CaabApplicationException("No scope limitation found at index: " + index);
        }
      }

    } else {
      final ProceedingDetail proceeding =
          (ProceedingDetail) session.getAttribute(CURRENT_PROCEEDING);

      if (scopeLimitation.getId() == null) {
        proceeding.getScopeLimitations().add(scopeLimitation);
      } else {
        final List<ScopeLimitationDetail> scopeLimitations = proceeding.getScopeLimitations();
        IntStream.range(0, scopeLimitations.size())
            .filter(i -> scopeLimitations.get(i).getId().equals(scopeLimitation.getId()))
            .findFirst()
            .ifPresent(i -> scopeLimitations.set(i, scopeLimitation));
      }

      applicationService.updateProceeding(proceeding, user);

      final List<ScopeLimitationDetail> scopeLimitations =
          applicationService.getScopeLimitations(proceeding.getId());
      proceeding.setScopeLimitations(scopeLimitations);

      model.addAttribute(CURRENT_PROCEEDING, proceeding);
    }

    return "redirect:/%s/proceedings/%s/confirm"
        .formatted(caseContext.getPathValue(), proceedingFlow.getAction());
  }

  /**
   * Displays the confirmation page for removing a scope limitation.
   *
   * @param caseContext the current case context.
   * @param scopeLimitationId the scope limitation index or stored identifier to remove.
   * @param proceedingFlow the current proceeding flow data.
   * @param model the model used to render the page.
   * @return the scope limitation removal view.
   */
  @GetMapping("/{caseContext}/proceedings/scope-limitations/{scope-limitation-id}/remove")
  public String scopeLimitationRemove(
      @PathVariable("caseContext") final CaseContext caseContext,
      @PathVariable("scope-limitation-id") final int scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      final Model model) {

    model.addAttribute("scopeLimitationId", scopeLimitationId);
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return "application/proceedings-scope-limitations-remove";
  }

  /**
   * Removes a scope limitation from the proceeding.
   *
   * <p>For proceedings being created in-session, the path variable is treated as a list index. For
   * existing proceedings, it is treated as the stored scope limitation identifier and the
   * proceeding is persisted after removal.
   *
   * @param caseContext the current case context.
   * @param scopeLimitationId the scope limitation index or stored identifier to remove.
   * @param proceedingFlow the current proceeding flow data.
   * @param user the current user.
   * @param model the model used to update session-backed attributes.
   * @param session the HTTP session containing proceeding state.
   * @return a redirect to the proceeding confirmation page.
   */
  @PostMapping("/{caseContext}/proceedings/scope-limitations/{scope-limitation-id}/remove")
  public String scopeLimitationRemovePost(
      @PathVariable("caseContext") final CaseContext caseContext,
      @PathVariable("scope-limitation-id") final int scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model,
      final HttpSession session) {

    if (ACTION_ADD.equals(proceedingFlow.getAction())) {
      final List<ScopeLimitationDetail> scopeLimitations =
          (List<ScopeLimitationDetail>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      scopeLimitations.remove(scopeLimitationId);

      model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);

    } else {
      final ProceedingDetail proceeding =
          (ProceedingDetail) session.getAttribute(CURRENT_PROCEEDING);
      final List<ScopeLimitationDetail> scopeLimitations = proceeding.getScopeLimitations();

      scopeLimitations.removeIf(
          scopeLimitation -> scopeLimitation.getId().equals(scopeLimitationId));

      applicationService.updateProceeding(proceeding, user);

      model.addAttribute(CURRENT_PROCEEDING, proceeding);
    }

    return "redirect:/%s/proceedings/%s/confirm"
        .formatted(caseContext.getPathValue(), proceedingFlow.getAction());
  }

  /**
   * Populates the model with scope limitation reference data for the proceeding.
   *
   * @param model the model used to render the dropdown.
   * @param application the current application.
   * @param proceedingFlow the current proceeding flow data.
   */
  private void populateScopeLimitationDropdown(
      final Model model,
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow) {

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        createScopeLimitationCriteria(application, proceedingFlow);

    final List<uk.gov.laa.ccms.data.model.ScopeLimitationDetail> scopeLimitationTypes =
        Optional.ofNullable(lookupService.getScopeLimitationDetails(criteria).block())
            .map(
                result ->
                    result.getContent() != null
                        ? result.getContent().stream()
                            .sorted(
                                Comparator.comparing(
                                    uk.gov.laa.ccms.data.model.ScopeLimitationDetail
                                        ::getDescription))
                            .toList()
                        : null)
            .orElse(Collections.emptyList());

    model.addAttribute("scopeLimitationTypes", scopeLimitationTypes);
  }

  /**
   * Resolves the selected scope limitation reference data and stores it as the current scope
   * limitation.
   *
   * @param model the model used to update session-backed attributes.
   * @param application the current application.
   * @param proceedingFlow the current proceeding flow data.
   * @param scopeLimitationFlow the in-progress scope limitation flow.
   */
  private void populateScopeLimitationDetails(
      final Model model,
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow,
      final ScopeLimitationFlowFormData scopeLimitationFlow) {

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        createScopeLimitationCriteria(application, proceedingFlow);
    criteria.scopeLimitations(scopeLimitationFlow.getScopeLimitationDetails().getScopeLimitation());

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail scopeLimitationDetail =
        Optional.ofNullable(lookupService.getScopeLimitationDetails(criteria).block())
            .map(
                result ->
                    result.getContent() != null
                        ? result.getContent().stream().findFirst().orElse(null)
                        : null)
            .orElseThrow(() -> new CaabApplicationException("No ScopeLimitationDetail found"));

    final ScopeLimitationDetail scopeLimitation =
        proceedingAndCostsMapper.toScopeLimitation(scopeLimitationDetail);
    scopeLimitation.setId(scopeLimitationFlow.getScopeLimitationId());

    model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
  }

  private void patchScopeLimitationNonDefaultWordingRequirement(
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow,
      final ScopeLimitationDetail scopeLimitation) {
    if (application == null
        || proceedingFlow == null
        || scopeLimitation.getScopeLimitation() == null
        || scopeLimitation.getScopeLimitation().getId() == null) {
      return;
    }

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        createScopeLimitationCriteria(application, proceedingFlow);
    criteria.scopeLimitations(scopeLimitation.getScopeLimitation().getId());

    final Boolean nonDefaultWordingRequired =
        Optional.ofNullable(lookupService.getScopeLimitationDetails(criteria).block())
            .map(uk.gov.laa.ccms.data.model.ScopeLimitationDetails::getContent)
            .orElseGet(Collections::emptyList)
            .stream()
            .findFirst()
            .map(uk.gov.laa.ccms.data.model.ScopeLimitationDetail::getNonStandardWordingRequired)
            .orElse(Boolean.FALSE);

    scopeLimitation.setNonDefaultWordingReqd(nonDefaultWordingRequired);
  }

  /**
   * Builds the reference-data search criteria used to find scope limitations for the proceeding.
   *
   * @param application the current application.
   * @param proceedingFlow the current proceeding flow data.
   * @return the populated scope limitation search criteria.
   */
  private uk.gov.laa.ccms.data.model.ScopeLimitationDetail createScopeLimitationCriteria(
      final ApplicationDetail application, final ProceedingFlowFormData proceedingFlow) {

    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
            .categoryOfLaw(application.getCategoryOfLaw().getId())
            .matterType(proceedingFlow.getMatterTypeDetails().getMatterType())
            .proceedingCode(proceedingFlow.getProceedingDetails().getProceedingType())
            .levelOfService(proceedingFlow.getFurtherDetails().getLevelOfService());

    if (EMERGENCY_APPLICATION_TYPE_CODES.contains(application.getApplicationType().getId())) {
      criteria.emergency(true);
    }

    return criteria;
  }
}
