package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PROCEEDINGS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA_OLD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupValueDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's proceedings and costs section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    APPLICATION,
    APPLICATION_PROCEEDINGS,
    APPLICATION_COSTS,
    APPLICATION_PRIOR_AUTHORITIES,
    CURRENT_PROCEEDING,
    PROCEEDING_FLOW_FORM_DATA,
    PROCEEDING_FLOW_FORM_DATA_OLD,
    PROCEEDING_SCOPE_LIMITATIONS})
public class EditProceedingsAndCostsSectionController {

  private final ApplicationService applicationService;

  private final LookupService lookupService;

  private final ProceedingMatterTypeDetailsValidator matterTypeValidator;
  private final ProceedingDetailsValidator proceedingTypeValidator;
  private final ProceedingFurtherDetailsValidator furtherDetailsValidator;

  private final ProceedingAndCostsMapper proceedingAndCostsMapper;

  private static final String ACTION_EDIT = "edit";
  private static final String ACTION_ADD = "add";
  private static final String STATUS_DISPLAY_VALUE_ADDED = "Added";
  private static final String STATUS_DISPLAY_VALUE_UPDATED = "Updated";
  private static final String STATUS_DRAFT = "Draft";

  /**
   * Handles the GET request to fetch and display the proceedings and costs for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings-and-costs")
  public String proceedingsAndCosts(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException(
                "No application found with id: " + applicationId));

    applicationService.prepareProceedingSummary(applicationId, application, user);
    model.addAttribute(APPLICATION, application);

    model.addAttribute(APPLICATION_COSTS, application.getCosts());

    final List<Proceeding> proceedings =
        Optional.ofNullable(application.getProceedings())
            .orElse(Collections.emptyList());

    model.addAttribute(APPLICATION_PROCEEDINGS, proceedings);

    final List<PriorAuthority> priorAuthorities =
        Optional.ofNullable(application.getPriorAuthorities())
            .orElse(Collections.emptyList());

    model.addAttribute(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities);

    return "application/proceedings-and-costs-section";
  }

  /**
   * Handles the GET request to set a specific proceeding as the lead proceeding for a given
   * application.
   *
   * @param applicationId The ID of the application, obtained from the session.
   * @param proceedings The list of proceedings associated with the application, obtained from the
   *                    session.
   * @param proceedingId The ID of the proceeding that is to be set as the lead proceeding.
   * @param user The UserDetail object representing the current user, obtained from the session.
   * @return A redirect instruction to the proceedings and costs view.
   */
  @GetMapping("/application/proceedings/{proceeding-id}/make-lead")
  public String proceedingsMakeLead(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(APPLICATION_PROCEEDINGS) final List<Proceeding> proceedings,
      @PathVariable("proceeding-id") final Integer proceedingId,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    // Safety check to make sure proceedingId is already in application.proceedings,
    final boolean proceedingExists = proceedings.stream()
        .anyMatch(proceeding -> proceeding.getId().equals(proceedingId));

    if (proceedingExists) {
      applicationService.makeLeadProceeding(applicationId, proceedingId, user);
    }

    return "redirect:/application/proceedings-and-costs";
  }

  /**
   * Handles the GET request to retrieve a summary of a specific proceeding.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param application The application details, retrieved from the session.
   * @param proceedings The list of proceedings, retrieved from the session.
   * @param user The UserDetail object representing the user, retrieved from the session.
   * @param proceedingId The id of the proceeding to retrieve the summary for.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{proceeding-id}/summary")
  public String proceedingsSummary(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(APPLICATION_PROCEEDINGS) final List<Proceeding> proceedings,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable("proceeding-id") final Integer proceedingId,
      final Model model) {

    final Proceeding proceeding = proceedings.stream()
        .filter(proceeding1 -> proceeding1.getId().equals(proceedingId))
        .findFirst()
        .orElseThrow();

    if (proceeding.getTypeOfOrder() != null
        && proceeding.getTypeOfOrder().getId() != null) {
      final String orderTypeDisplayValue =
          lookupService.getOrderTypeDescription(
              proceeding.getTypeOfOrder().getId()).block();
      model.addAttribute("orderTypeDisplayValue", orderTypeDisplayValue);
    }

    model.addAttribute(CURRENT_PROCEEDING, proceeding);

    //default cost limitations
    applicationService.prepareProceedingSummary(applicationId, application, user);

    //reset needed to determine navigation
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, new ProceedingFlowFormData(ACTION_EDIT));

    //todo - get proceeding outcomes from tds for amendments
    // see PrepareProceedingSummary in PUI
    // loadProceedingOutcomesFromTds

    return "application/proceedings-summary";
  }

  /**
   * Handles the GET request to display the matter type form for a proceeding.
   *
   * @param application The application details.
   * @param action The action being performed (add or edit).
   * @param model The Model object to add attributes to for the view.
   * @param session The HTTP session.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{action}/matter-type")
  public String proceedingsActionMatterType(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @PathVariable("action") final String action,
      final Model model,
      final HttpSession session) {

    populateMatterTypeDropdown(model, application.getCategoryOfLaw().getId());

    if (action.equals(ACTION_ADD)) {
      final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      model.addAttribute("matterTypeDetails",
          proceedingFlow.getMatterTypeDetails());

    } else {
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
      model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, proceeding.getScopeLimitations());

      final ProceedingFlowFormData proceedingFlow = proceedingAndCostsMapper.toProceedingFlow(
          proceeding, null);

      //set orderTypeRequired flag if typeOfOrder is not null
      Optional.ofNullable(proceeding.getTypeOfOrder().getId())
          .ifPresent(id -> proceedingFlow.getProceedingDetails().setOrderTypeRequired(true));

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      model.addAttribute(PROCEEDING_FLOW_FORM_DATA_OLD, proceedingFlow);

      model.addAttribute("matterTypeDetails",
          proceedingFlow.getMatterTypeDetails());
    }

    return "application/proceedings-matter-type";
  }

  /**
   * Handles the POST request to update the matter type of a proceeding.
   *
   * @param application The application details.
   * @param proceedingFlow The proceeding flow data.
   * @param matterTypeDetails The matter type details of the proceeding.
   * @param action The action being performed (add or edit).
   * @param bindingResult The results of validation.
   * @param model The Model object to add attributes to for the view.
   * @param session The HTTP session.
   * @return A redirect instruction to the proceeding type view.
   */
  @PostMapping("/application/proceedings/{action}/matter-type")
  public String proceedingsActionMatterTypePost(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @ModelAttribute("matterTypeDetails") final ProceedingFormDataMatterTypeDetails
          matterTypeDetails,
      @PathVariable("action") final String action,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    matterTypeValidator.validate(matterTypeDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      populateMatterTypeDropdown(model, application.getCategoryOfLaw().getId());
      return "application/proceedings-matter-type";
    }

    //check if the data has been amended
    if (action.equals(ACTION_ADD)) {
      proceedingFlow.setAmended(Boolean.TRUE);
    } else {
      final ProceedingFlowFormData oldProceedingFlow =
          (ProceedingFlowFormData) session.getAttribute(PROCEEDING_FLOW_FORM_DATA_OLD);
      if (!oldProceedingFlow.getMatterTypeDetails().getMatterType().equals(
          matterTypeDetails.getMatterType())) {
        proceedingFlow.setAmended(Boolean.TRUE);
      }
    }

    proceedingFlow.setMatterTypeDetails(matterTypeDetails);
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return String.format("redirect:/application/proceedings/%s/proceeding-type", action);

  }

  /**
   * Populates the matter type dropdown for the proceedings form.
   *
   * @param model The Model object to add attributes to for the view.
   * @param categoryOfLaw The category of law to fetch the matter types for.
   */
  private void populateMatterTypeDropdown(
      final Model model,
      final String categoryOfLaw) {

    final List<MatterTypeLookupValueDetail> matterTypes = Optional.ofNullable(
        lookupService.getMatterTypes(categoryOfLaw).block())
        .map(MatterTypeLookupDetail::getContent)
        .orElse(Collections.emptyList());

    model.addAttribute("matterTypes", matterTypes);
  }

  /**
   * Handles the GET request to display the proceeding type form for a proceeding.
   *
   * @param proceedingFlow The proceeding flow data.
   * @param application The application details.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{action}/proceeding-type")
  public String proceedingsActionProceedingType(
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      final Model model) {

    populateProceedingTypeDropdown(
        model,
        application,
        proceedingFlow.getMatterTypeDetails().getMatterType(),
        proceedingFlow.isLeadProceeding());

    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
    model.addAttribute("proceedingTypeDetails",
        proceedingFlow.getProceedingDetails());

    return "application/proceedings-proceeding-type";
  }

  /**
   * Handles the POST request to update the proceeding type.
   *
   * @param application The application details.
   * @param proceedingFlow The proceeding flow data.
   * @param proceedingTypeDetails The proceeding type details of the proceeding.
   * @param action The action being performed (add or edit).
   * @param bindingResult The results of validation.
   * @param model The Model object to add attributes to for the view.
   * @param session The HTTP session.
   * @return A redirect instruction to the proceedings further details view.
   */
  @PostMapping("/application/proceedings/{action}/proceeding-type")
  public String proceedingsActionProceedingTypePost(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @ModelAttribute("proceedingTypeDetails") final ProceedingFormDataProceedingDetails
          proceedingTypeDetails,
      @PathVariable("action") final String action,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    proceedingTypeValidator.validate(proceedingTypeDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateProceedingTypeDropdown(
          model,
          application,
          proceedingFlow.getMatterTypeDetails().getMatterType(),
          proceedingFlow.isLeadProceeding());

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

      return "application/proceedings-proceeding-type";
    }

    if (action.equals(ACTION_ADD)) {
      proceedingFlow.setAmended(Boolean.TRUE);
    } else {
      final ProceedingFlowFormData oldProceedingFlow =
          (ProceedingFlowFormData) session.getAttribute(PROCEEDING_FLOW_FORM_DATA_OLD);
      if (!oldProceedingFlow.getProceedingDetails().getProceedingType().equals(
          proceedingTypeDetails.getProceedingType())) {
        proceedingFlow.setAmended(Boolean.TRUE);
      }
    }

    proceedingFlow.setProceedingDetails(proceedingTypeDetails);
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return String.format("redirect:/application/proceedings/%s/further-details", action);

  }

  /**
   * Populates the proceeding type dropdown for the proceedings form.
   *
   * @param model The Model object to add attributes to for the view.
   * @param application The application details.
   * @param matterType The type of the matter.
   * @param isLead A boolean indicating if the proceeding is a lead proceeding.
   */
  private void populateProceedingTypeDropdown(
      final Model model,
      final ApplicationDetail application,
      final String matterType,
      final boolean isLead) {

    final ProceedingDetail searchCriteria = new ProceedingDetail()
        .amendmentOnly(application.getAmendment())
        .matterType(matterType)
        .categoryOfLawCode(application.getCategoryOfLaw().getId());

    final Boolean larScopeFlag = application.getLarScopeFlag();
    final String applicationType = application.getApplicationType().getId();

    final List<ProceedingDetail> proceedingDetails = Optional.ofNullable(
        lookupService.getProceedings(searchCriteria, larScopeFlag, applicationType, isLead).block())
        .map(ProceedingDetails::getContent)
        .orElse(Collections.emptyList());

    model.addAttribute("proceedingTypes", proceedingDetails);
  }

  /**
   * Handles the GET request to display the further details of a proceeding.
   *
   * @param proceedingFlow The proceeding flow data.
   * @param application The application details.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{action}/further-details")
  public String proceedingsActionFurtherDetails(
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      final Model model) {

    populateFurtherDetailsDropdowns(model,
        application,
        proceedingFlow.getProceedingDetails(),
        proceedingFlow.getMatterTypeDetails().getMatterType());

    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
    model.addAttribute("furtherDetails",
        proceedingFlow.getFurtherDetails());

    return "application/proceedings-further-details";
  }

  /**
   * Handles the POST request to update the further details of a proceeding.
   *
   * @param application The application details.
   * @param proceedingFlow The proceeding flow data.
   * @param furtherDetails The further details of the proceeding.
   * @param action The action being performed (add or edit).
   * @param bindingResult The results of validation.
   * @param model The Model object to add attributes to for the view.
   * @param session The HTTP session.
   * @return A redirect instruction to the proceedings confirm view.
   */
  @PostMapping("/application/proceedings/{action}/further-details")
  public String proceedingsActionFurtherDetailsPost(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @ModelAttribute("furtherDetails") final ProceedingFormDataFurtherDetails furtherDetails,
      @PathVariable("action") final String action,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    proceedingFlow.setFurtherDetails(furtherDetails);
    furtherDetailsValidator.validate(proceedingFlow, bindingResult);

    if (bindingResult.hasErrors()) {
      populateFurtherDetailsDropdowns(model,
          application,
          proceedingFlow.getProceedingDetails(),
          proceedingFlow.getMatterTypeDetails().getMatterType());
      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      return "application/proceedings-further-details";
    }

    if (action.equals(ACTION_ADD)) {
      proceedingFlow.setAmended(Boolean.TRUE);
    } else {

      final ProceedingFlowFormData oldProceedingFlow =
          (ProceedingFlowFormData) session.getAttribute(PROCEEDING_FLOW_FORM_DATA_OLD);

      // Check if clientInvolvementType, levelOfService, or typeOfOrder have changed
      if (oldProceedingFlow.getFurtherDetails().getClientInvolvementType() != null
          && !oldProceedingFlow.getFurtherDetails().getClientInvolvementType().equals(
              furtherDetails.getClientInvolvementType())) {
        proceedingFlow.setAmended(Boolean.TRUE);
      } else if (oldProceedingFlow.getFurtherDetails().getLevelOfService() != null
          && !oldProceedingFlow.getFurtherDetails().getLevelOfService().equals(
              furtherDetails.getLevelOfService())) {
        proceedingFlow.setAmended(Boolean.TRUE);
      } else if (oldProceedingFlow.getFurtherDetails().getTypeOfOrder() != null
          && !oldProceedingFlow.getFurtherDetails().getTypeOfOrder().equals(
              furtherDetails.getTypeOfOrder())) {
        proceedingFlow.setAmended(Boolean.TRUE);
      }

    }

    proceedingFlow.setFurtherDetails(furtherDetails);
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return String.format("redirect:/application/proceedings/%s/confirm", action);
  }

  /**
   * Populates the dropdowns for the further details section of a proceeding.
   *
   * @param model The Model object to add attributes to for the view.
   * @param application The application details.
   * @param proceedingDetails The details of the proceeding.
   * @param matterType The type of the matter.
   */
  private void populateFurtherDetailsDropdowns(
      final Model model,
      final ApplicationDetail application,
      final ProceedingFormDataProceedingDetails proceedingDetails,
      final String matterType) {

    final String proceedingCode = proceedingDetails.getProceedingType();

    final Mono<List<ClientInvolvementTypeLookupValueDetail>> clientInvolvementTypesMono =
        lookupService.getClientInvolvementTypes(proceedingCode)
            .map(ClientInvolvementTypeLookupDetail::getContent);

    final Mono<List<LevelOfServiceLookupValueDetail>> levelOfServiceTypesMono =
        lookupService.getProceedingLevelOfServiceTypes(
            application.getCategoryOfLaw().getId(), proceedingCode, matterType)
            .map(LevelOfServiceLookupDetail::getContent);

    Mono.zip(clientInvolvementTypesMono, levelOfServiceTypesMono)
        .doOnNext(tuple -> {
          model.addAttribute("clientInvolvementTypes", tuple.getT1());
          model.addAttribute("levelOfServiceTypes", tuple.getT2());
        }).block();

    if (proceedingDetails.getOrderTypeRequired() != null
        && proceedingDetails.getOrderTypeRequired()) {
      new DropdownBuilder(model)
          .addDropdown("orderTypes",
              lookupService.getOrderTypes())
          .build();
    }

  }

  /**
   * Handles the GET request to confirm the action (add or edit) on a proceeding.
   *
   * @param application The application details.
   * @param action The action being performed (add or edit).
   * @param model The Model object to add attributes to for the view.
   * @param session The HTTP session.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{action}/confirm")
  public String proceedingsActionConfirm(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @PathVariable("action") final String action,
      final Model model,
      final HttpSession session) {

    ProceedingFlowFormData proceedingFlow;

    //coming directly from proceeding summary - editing scope limitations
    if (isRoutedFromSummaryPage(action, session)) {
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);

      //get order type display value to be visible for the page
      //this is not stored in the proceeding db object so we have to look it up
      final String orderTypeDisplayValue = Optional.ofNullable(proceeding.getTypeOfOrder())
          .map(StringDisplayValue::getId)
          .map(id -> lookupService.getOrderTypeDescription(id).block())
          .orElse(null);

      proceedingFlow = proceedingAndCostsMapper.toProceedingFlow(proceeding, orderTypeDisplayValue);

      //we set this true so when editing scope limitation we won't route back to editing the
      // proceedings screens
      proceedingFlow.setEditingScopeLimitations(true);

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
      model.addAttribute(CURRENT_PROCEEDING, proceeding);

    } else {
      // Came from previous step in the flow - editing further details, proceeding type and
      // matter type
      proceedingFlow = (ProceedingFlowFormData) session.getAttribute(PROCEEDING_FLOW_FORM_DATA);
    }

    // We want to retrieve the default scope limitations if the proceeding has been amended.
    // Otherwise, we just retrieve the ones already stored in the session.
    if (proceedingFlow.isAmended()) {
      final ScopeLimitationDetails scopeLimitationDetails =
          applicationService.getDefaultScopeLimitation(
                  application.getCategoryOfLaw().getId(),
                  proceedingFlow.getMatterTypeDetails().getMatterType(),
                  proceedingFlow.getProceedingDetails().getProceedingType(),
                  proceedingFlow.getFurtherDetails().getLevelOfService(),
                  application.getApplicationType().getId())
              .block();

      if (scopeLimitationDetails != null) {
        final List<ScopeLimitation> scopeLimitations =
            proceedingAndCostsMapper.toScopeLimitationList(
                scopeLimitationDetails.getContent());

        model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);
      }

      // Now we have retrieved the default scope limitations no need to do it again,
      // we do this last in case of errors
      proceedingFlow.setAmended(Boolean.FALSE);
    } else {
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
      model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, proceeding.getScopeLimitations());
    }

    //Add scope limitations to model, use from session if already set
    Optional.ofNullable(session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS))
        .filter(List.class::isInstance)
        .map(List.class::cast)
        .filter(list -> !list.isEmpty() && list.get(0) instanceof ScopeLimitation)
        .ifPresent(list -> model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, list));

    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return "application/proceedings-confirm";
  }

  /**
   * Checks if the current request is routed from the summary page.
   *
   * @param action The action being performed, retrieved from the session.
   * @param session The HttpSession object representing the current session.
   * @return true if the action is 'edit', the proceedingFlow object exists in the session,
   *         and the matterTypeDetails is not null and its matterType is null. Otherwise, returns
   *         false.
   */
  private boolean isRoutedFromSummaryPage(final String action, final HttpSession session) {
    if (ACTION_EDIT.equals(action)) {
      final ProceedingFlowFormData proceedingFlow =
          (ProceedingFlowFormData) session.getAttribute(PROCEEDING_FLOW_FORM_DATA);
      return proceedingFlow != null && proceedingFlow.getMatterTypeDetails() != null
          && proceedingFlow.getMatterTypeDetails().getMatterType() == null;
    }
    return false;
  }

  /**
   * Handles the POST request to confirm the action (add or edit) on a proceeding.
   *
   * @param application The application details.
   * @param applicationId The ID of the application.
   * @param proceedingFlow The proceeding flow data.
   * @param scopeLimitations The list of scope limitations.
   * @param proceedings The list of proceedings.
   * @param user The user performing the operation.
   * @param action The action being performed (add or edit).
   * @param session The HTTP session.
   * @return A redirect instruction to the proceedings and costs view.
   */
  @PostMapping("/application/proceedings/{action}/confirm")
  public String proceedingsActionConfirmPost(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(PROCEEDING_SCOPE_LIMITATIONS) final List<ScopeLimitation> scopeLimitations,
      @SessionAttribute(APPLICATION_PROCEEDINGS) final List<Proceeding> proceedings,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable("action") final String action,
      final HttpSession session) {

    //get proceeding cost limitation
    final BigDecimal costLimitation = applicationService.getProceedingCostLimitation(
        application.getCategoryOfLaw().getId(),
        proceedingFlow.getMatterTypeDetails().getMatterType(),
        proceedingFlow.getProceedingDetails().getProceedingType(),
        proceedingFlow.getFurtherDetails().getLevelOfService(),
        application.getApplicationType().getId(),
        scopeLimitations);

    //get proceeding stage
    final String stage = Optional.ofNullable(applicationService.getProceedingStage(
            application.getCategoryOfLaw().getId(),
            proceedingFlow.getMatterTypeDetails().getMatterType(),
            proceedingFlow.getProceedingDetails().getProceedingType(),
            proceedingFlow.getFurtherDetails().getLevelOfService(),
            scopeLimitations, application.getAmendment()))
        .map(Object::toString)
        .orElse(null);

    if (action.equals(ACTION_ADD)) {

      final Proceeding proceeding = proceedingAndCostsMapper.toProceeding(
          proceedingFlow,
          costLimitation,
          stage);

      if (proceedings == null || proceedings.isEmpty()) {
        proceeding.setLeadProceedingInd(true);
        application.setLeadProceedingChanged(true);
      }

      proceeding.setEdited(Boolean.TRUE);
      proceeding.setScopeLimitations(scopeLimitations);

      proceeding.setStatus(new StringDisplayValue()
          .id(STATUS_DRAFT)
          .displayValue(STATUS_DISPLAY_VALUE_ADDED));

      applicationService.addProceeding(applicationId, proceeding, user);

    } else {

      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
      proceedingAndCostsMapper.toProceeding(
          proceeding,
          proceedingFlow,
          costLimitation,
          stage);

      proceeding.setScopeLimitations(scopeLimitations);

      if (proceeding.getStatus().getDisplayValue() != null
          && !STATUS_DISPLAY_VALUE_ADDED.equals(proceeding.getStatus().getDisplayValue())) {
        proceeding.getStatus().setDisplayValue(STATUS_DISPLAY_VALUE_UPDATED);
      }

      applicationService.updateProceeding(proceeding, user);
    }

    //todo - need to save the application updates
    // requires CCLS-2055

    return "redirect:/application/proceedings-and-costs";
  }


}
