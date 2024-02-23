package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EMERGENCY_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PROCEEDINGS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_SCOPE_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA_OLD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SCOPE_LIMITATION_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.scopelimitation.ScopeLimitationDetailsValidator;
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
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
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
    PROCEEDING_SCOPE_LIMITATIONS,
    SCOPE_LIMITATION_FLOW_FORM_DATA,
    CURRENT_SCOPE_LIMITATION})
@SuppressWarnings("unchecked")
public class EditProceedingsAndCostsSectionController {

  private final ApplicationService applicationService;

  private final LookupService lookupService;

  private final ProceedingMatterTypeDetailsValidator matterTypeValidator;

  private final ProceedingDetailsValidator proceedingTypeValidator;

  private final ProceedingFurtherDetailsValidator furtherDetailsValidator;

  private final ScopeLimitationDetailsValidator scopeLimitationDetailsValidator;

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
   * Handles the GET request to display the confirmation page for removing a proceeding.
   *
   * @param proceedingId The ID of the proceeding to be removed, obtained from the path variable.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/proceedings/{proceeding-id}/remove")
  public String proceedingsRemove(
      @PathVariable("proceeding-id") final Integer proceedingId,
      final Model model) {

    model.addAttribute("proceedingId", proceedingId);

    return "/application/proceedings-remove";
  }

  /**
   * Handles the POST request to remove a specific proceeding from an application.
   *
   * @param proceedingId The ID of the proceeding to be removed, obtained from the path variable.
   * @param proceedings The list of proceedings associated with the application, obtained from the
   *                    session.
   * @param user The UserDetail object representing the current user, obtained from the session.
   * @return A redirect instruction to the proceedings and costs view.
   */
  @PostMapping("/application/proceedings/{proceeding-id}/remove")
  public String proceedingsRemovePost(
      @PathVariable("proceeding-id") final Integer proceedingId,
      @SessionAttribute(APPLICATION_PROCEEDINGS) final List<Proceeding> proceedings,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    final boolean proceedingExists = proceedings.stream()
        .anyMatch(proceeding -> proceeding.getId().equals(proceedingId));

    if (proceedingExists) {
      applicationService.deleteProceeding(proceedingId, user);
    } else {
      throw new CaabApplicationException(
          "No proceeding found in current application with id: " + proceedingId);
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
      //set the proceeding description to the proceeding type display value
      proceedingTypeDetails.setProceedingDescription(
          proceedingTypeDetails.getProceedingTypeDisplayValue());
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
    ProceedingFlowFormData proceedingFlow = null;

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

      //refresh the scope limitations
      final List<ScopeLimitation> scopeLimitations =
          applicationService.getScopeLimitations(proceeding.getId());
      proceeding.setScopeLimitations(scopeLimitations);
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
      if (action.equals(ACTION_EDIT)) {
        final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
        model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, proceeding.getScopeLimitations());
      } else {
        model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS,
            session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS));
      }
    }

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


  /**
   * Handles the GET request to edit a specific scope limitation of a proceeding.
   *
   * @param scopeLimitationId The ID of the scope limitation to be edited, obtained from the path
   *                          variable.
   * @param proceedingFlow The ProceedingFlowFormData object, obtained from the session.
   * @param model The Model object to add attributes to for the view.
   * @param session The HttpSession object representing the current session.
   * @return A redirect instruction to the scope limitation details view.
   */
  @GetMapping("/application/proceedings/scope-limitations/{scope-limitation-id}/edit")
  public String scopeLimitationEdit(
      @PathVariable("scope-limitation-id") final Integer scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      final Model model,
      final HttpSession session) {


    // we need to different logic based off if the proceeding has just been created in memory or is
    // being edited
    if (proceedingFlow.getAction().equals(ACTION_ADD)) {
      // we need to get the scope limitations from the session
      final List<ScopeLimitation> scopeLimitations =
          (List<ScopeLimitation>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      final ScopeLimitation scopeLimitation = scopeLimitations.get(scopeLimitationId);
      model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);

      final ScopeLimitationFlowFormData scopeLimitationFlow =
          proceedingAndCostsMapper.toScopeLimitationFlow(scopeLimitation);
      scopeLimitationFlow.setScopeLimitationIndex(scopeLimitationId);

      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

    } else {
      // we need to get the scope limitations from the stored proceeding
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
      final ScopeLimitation scopeLimitation = proceeding.getScopeLimitations().stream()
          .filter(sl -> sl.getId().equals(scopeLimitationId))
          .findFirst()
          .orElseThrow();

      model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);

      final ScopeLimitationFlowFormData scopeLimitationFlow =
          proceedingAndCostsMapper.toScopeLimitationFlow(scopeLimitation);

      model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
    }

    return "redirect:/application/proceedings/scope-limitations/edit/details";
  }

  /**
   * Handles the request for viewing the details of scope limitations
   * based on a specific action.
   *
   * @param scopeLimitationAction the action related to scope limitations,
   *                              extracted from the URL path.
   * @param application the application details, retrieved from the session.
   * @param proceedingFlow the proceeding flow data, retrieved from the session.
   * @param model the {@link Model} object for passing attributes to the view.
   * @param session the {@link HttpSession} object for accessing session attributes.
   * @return the name of the view to render.
   */
  @GetMapping("/application/proceedings/scope-limitations/{action}/details")
  public String scopeLimitationDetails(
      @PathVariable("action") final String scopeLimitationAction,
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      final Model model,
      final HttpSession session) {

    ScopeLimitationFlowFormData scopeLimitationFlow =
        new ScopeLimitationFlowFormData(scopeLimitationAction);

    if (scopeLimitationAction.equals(ACTION_EDIT)) {
      scopeLimitationFlow = (ScopeLimitationFlowFormData)
          session.getAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA);
    }

    model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
    model.addAttribute("scopeLimitationDetails",
        scopeLimitationFlow.getScopeLimitationDetails());

    populateScopeLimitationDropdown(model, application, proceedingFlow);

    //used for determining the action
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return "application/proceedings-scope-limitations-details";
  }

  /**
   * Processes the submission of scope limitation details.
   *
   * @param application the application details, retrieved from the session.
   * @param proceedingFlow the proceeding flow data, retrieved from the session.
   * @param scopeLimitationFlow the scope limitation flow data, retrieved from
   *                            the session.
   * @param scopeLimitationDetails the submitted details of the scope limitation.
   * @param model the {@link Model} object for passing attributes to the view.
   * @param bindingResult the result of the validation process.
   * @return the name of the view to render or a redirect path.
   */
  @PostMapping("/application/proceedings/scope-limitations/{action}/details")
  public String scopeLimitationDetailsPost(
      @SessionAttribute(APPLICATION) final ApplicationDetail application,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA)
      final ProceedingFlowFormData proceedingFlow,
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

    return "redirect:/application/proceedings/scope-limitations/confirm";
  }

  /**
   * Populates the dropdown for scope limitations in the model.
   *
   * <p>Utilizes application and proceeding flow data to determine the criteria
   * for filtering scope limitations. Adjusts criteria for emergency applications
   * and retrieves a sorted list of scope limitation details for dropdown
   * population.</p>
   *
   * @param model the {@link Model} object for passing attributes to the view.
   * @param application the application details, used to set criteria.
   * @param proceedingFlow the proceeding flow data, used to set criteria.
   */
  private void populateScopeLimitationDropdown(
      final Model model,
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow) {

    final ScopeLimitationDetail criteria = createScopeLimitationCriteria(
        application, proceedingFlow);

    final List<ScopeLimitationDetail> scopeLimitationTypes = Optional.ofNullable(
            lookupService.getScopeLimitationDetails(criteria).block())
        .map(result ->
            result.getContent() != null
                ? result.getContent().stream()
                .sorted(Comparator.comparing(ScopeLimitationDetail::getDescription))
                .collect(Collectors.toList())
                : null)
        .orElse(Collections.emptyList());

    model.addAttribute("scopeLimitationTypes", scopeLimitationTypes);

  }

  /**
   * Populates the model with detailed scope limitation information for the current
   * application and proceeding flow.
   *
   * <p>Builds criteria based on application details, proceeding flow, and selected scope
   * limitations to retrieve a specific scope limitation detail. Handles emergency
   * application criteria separately. Maps the retrieved detail to a scope limitation
   * object and updates the model for view rendering.</p>
   *
   * @param model the {@link Model} object for passing attributes to the view.
   * @param application the application details, used to set criteria.
   * @param proceedingFlow the proceeding flow data, used to set criteria.
   * @param scopeLimitationFlow the selected scope limitation data.
   */
  private void populateScopeLimitationDetails(
      final Model model,
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow,
      final ScopeLimitationFlowFormData scopeLimitationFlow) {

    final ScopeLimitationDetail criteria = createScopeLimitationCriteria(
        application, proceedingFlow);
    criteria.scopeLimitations(scopeLimitationFlow.getScopeLimitationDetails()
        .getScopeLimitation());

    final ScopeLimitationDetail scopeLimitationDetail = Optional.ofNullable(
        lookupService.getScopeLimitationDetails(criteria).block())
        .map(result -> result.getContent() != null
            ? result.getContent().stream().findFirst().orElse(null)
            : null)
        .orElseThrow(() -> new CaabApplicationException("No ScopeLimitationDetail found"));

    final ScopeLimitation scopeLimitation =
        proceedingAndCostsMapper.toScopeLimitation(scopeLimitationDetail);
    scopeLimitation.setId(scopeLimitationFlow.getScopeLimitationId());

    model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
  }

  private ScopeLimitationDetail createScopeLimitationCriteria(
      final ApplicationDetail application,
      final ProceedingFlowFormData proceedingFlow) {

    final ScopeLimitationDetail criteria = new ScopeLimitationDetail()
        .categoryOfLaw(application.getCategoryOfLaw().getId())
        .matterType(proceedingFlow.getMatterTypeDetails().getMatterType())
        .proceedingCode(proceedingFlow.getProceedingDetails().getProceedingType())
        .levelOfService(proceedingFlow.getFurtherDetails().getLevelOfService());

    if (EMERGENCY_APPLICATION_TYPE_CODES.contains(application.getApplicationType().getId())) {
      criteria.emergency(true);
    }

    return criteria;
  }

  /**
   * Displays the confirmation page for scope limitations with the currently
   * selected scope limitation and its details.
   *
   * @param scopeLimitation the current scope limitation, retrieved from the session.
   * @param scopeLimitationFlow the scope limitation flow data, retrieved from the session.
   * @param model the {@link Model} object for passing attributes to the view.
   * @return the name of the view to render.
   */

  @GetMapping("/application/proceedings/scope-limitations/confirm")
  public String scopeLimitationConfirm(
      @SessionAttribute(CURRENT_SCOPE_LIMITATION) final ScopeLimitation scopeLimitation,
      @SessionAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA)
      final ScopeLimitationFlowFormData scopeLimitationFlow,
      final Model model) {

    model.addAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
    model.addAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

    return "application/proceedings-scope-limitations-confirm";
  }

  /**
   * Processes the confirmation of scope limitations, adding or updating them
   * based on the action specified in the proceeding flow. Handles both new additions
   * and updates to existing scope limitations within a session or proceeding.
   *
   * @param scopeLimitation the scope limitation to be confirmed, retrieved from the session.
   * @param proceedingFlow the proceeding flow data, indicating the current action.
   * @param scopeLimitationFlow the scope limitation flow data, containing index information
   *                            for updates.
   * @param user the current user's details, for updating proceedings.
   * @param model the {@link Model} object for passing attributes to the view.
   * @param session the {@link HttpSession} object for accessing session attributes.
   * @return the redirect URL for the proceeding confirmation page.
   */
  @PostMapping("/application/proceedings/scope-limitations/confirm")
  public String scopeLimitationConfirmPost(
      @SessionAttribute(CURRENT_SCOPE_LIMITATION) final ScopeLimitation scopeLimitation,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA) final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA)
      final ScopeLimitationFlowFormData scopeLimitationFlow,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model,
      final HttpSession session) {


    if (proceedingFlow.getAction().equals(ACTION_ADD)) {
      final List<ScopeLimitation> scopeLimitations =
          (List<ScopeLimitation>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

      if (scopeLimitationFlow.getScopeLimitationIndex() == null) {
        scopeLimitations.add(scopeLimitation);
      } else {
        int index = scopeLimitationFlow.getScopeLimitationIndex();
        if (index >= 0 && index < scopeLimitations.size()) {
          scopeLimitations.set(index, scopeLimitation);
        } else {
          throw new CaabApplicationException("No scope limitation found at index: " + index);
        }
      }

    } else {
      //when we are adding a scope limitation to an existing proceeding
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);

      if (scopeLimitation.getId() == null) {
        proceeding.getScopeLimitations().add(scopeLimitation);
      } else {
        //replace the scope limitation in the list with the one that matched the id
        final List<ScopeLimitation> scopeLimitations = proceeding.getScopeLimitations();
        IntStream.range(0, scopeLimitations.size())
            .filter(i -> scopeLimitations.get(i).getId().equals(scopeLimitation.getId()))
            .findFirst()
            .ifPresent(i -> scopeLimitations.set(i, scopeLimitation));
      }

      applicationService.updateProceeding(proceeding, user);

      //need to refresh current proceeding for new scope limitation id
      final List<ScopeLimitation> scopeLimitations =
          applicationService.getScopeLimitations(proceeding.getId());
      proceeding.setScopeLimitations(scopeLimitations);

      model.addAttribute(CURRENT_PROCEEDING, proceeding);
    }

    return String.format("redirect:/application/proceedings/%s/confirm",
        proceedingFlow.getAction());
  }

  /**
   * Displays the page for removing a scope limitation with an option to confirm or cancel.
   *
   * @param scopeLimitationId the ID of the scope limitation to be removed.
   * @param proceedingFlow the proceeding flow data, retrieved from the session.
   * @param model the {@link Model} object for passing attributes to the view.
   * @return the name of the view to render.
   */
  @GetMapping("/application/proceedings/scope-limitations/{scope-limitation-id}/remove")
  public String scopeLimitationRemove(
      @PathVariable("scope-limitation-id") final int scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA)
      final ProceedingFlowFormData proceedingFlow,
      final Model model) {

    model.addAttribute("scopeLimitationId", scopeLimitationId);
    model.addAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

    return "application/proceedings-scope-limitations-remove";
  }

  /**
   * Processes the removal of a scope limitation, either from the session or
   * by updating the database, based on the proceeding action.
   *
   * @param scopeLimitationId the ID of the scope limitation to remove.
   * @param proceedingFlow the proceeding flow data, indicating the current action.
   * @param user the current user's details, for updating proceedings.
   * @param model the {@link Model} object for passing attributes to the view.
   * @param session the {@link HttpSession} object for accessing session attributes.
   * @return the redirect URL for the proceeding confirmation page.
   */
  @PostMapping("/application/proceedings/scope-limitations/{scope-limitation-id}/remove")
  public String scopeLimitationRemovePost(
      @PathVariable("scope-limitation-id") final int scopeLimitationId,
      @SessionAttribute(PROCEEDING_FLOW_FORM_DATA)
      final ProceedingFlowFormData proceedingFlow,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model,
      final HttpSession session) {

    //remove the scope limitation from the session list
    if (proceedingFlow.getAction().equals(ACTION_ADD)) {
      final List<ScopeLimitation> scopeLimitations =
          (List<ScopeLimitation>) session.getAttribute(PROCEEDING_SCOPE_LIMITATIONS);

      scopeLimitations.remove(scopeLimitationId);

      model.addAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);

    } else {
      //remove the scope limitation from the proceeding and update the db
      final Proceeding proceeding = (Proceeding) session.getAttribute(CURRENT_PROCEEDING);
      final List<ScopeLimitation> scopeLimitations = proceeding.getScopeLimitations();

      scopeLimitations.removeIf(scopeLimitation ->
          scopeLimitation.getId().equals(scopeLimitationId));

      applicationService.updateProceeding(proceeding, user);

      model.addAttribute(CURRENT_PROCEEDING, proceeding);
    }

    return String.format("redirect:/application/proceedings/%s/confirm",
        proceedingFlow.getAction());
  }

}
