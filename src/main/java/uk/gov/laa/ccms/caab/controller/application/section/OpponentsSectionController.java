package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_OPPONENTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for the application's opponents and other parties section. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(
    value = {
      APPLICATION_OPPONENTS,
      ORGANISATION_SEARCH_CRITERIA,
      ORGANISATION_SEARCH_RESULTS,
      CURRENT_OPPONENT
    })
public class OpponentsSectionController {

  private final ApplicationService applicationService;

  private final AmendmentService amendmentService;

  private final OpponentService opponentService;

  private final LookupService lookupService;

  private final OrganisationSearchCriteriaValidator searchCriteriaValidator;

  private final OrganisationOpponentValidator organisationOpponentValidator;

  private final IndividualOpponentValidator individualOpponentValidator;

  /**
   * Provides an instance of {@link OrganisationSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link OrganisationSearchCriteria}.
   */
  @ModelAttribute(ORGANISATION_SEARCH_CRITERIA)
  public OrganisationSearchCriteria getOrganisationSearchCriteria() {
    return new OrganisationSearchCriteria();
  }

  /**
   * This method registers a custom editor to automatically trim leading and trailing whitespace
   * from String fields and convert empty strings to null. This method is executed before binding
   * request parameters to the model attributes.
   */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  /**
   * Handles the GET request to fetch and display the opponents and other parties for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents",
    "/application/sections/opponents",
    "/amendments/sections/opponents"
  })
  public String opponents(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      @SessionAttribute(value = USER_DETAILS, required = false) final UserDetail user,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    addCaseContext(model, resolvedCaseContext);

    if (resolvedCaseContext.isAmendment()) {
      ApplicationDetail application = applicationService.getApplication(applicationId).block();
      ApplicationSectionDisplay amendmentSections =
          amendmentService.getAmendmentSections(application, user);

      List<OpponentSectionDisplay> opponents =
          amendmentSections.getOpponentsAndOtherParties().getOpponents();

      opponents.forEach(opponent -> opponent.setEditable(opponent.getEbsId() == null));
      model.addAttribute(APPLICATION_OPPONENTS, opponents);
    } else {
      final List<AbstractOpponentFormData> opponents =
          applicationService.getOpponents(applicationId);
      model.addAttribute(APPLICATION_OPPONENTS, opponents);
    }

    return "application/sections/opponents-section";
  }

  /**
   * Handles the GET request to display the organisation search screen.
   *
   * @param searchCriteria The organisation search criteria.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/organisation/search",
    "/application/opponents/organisation/search",
    "/amendments/sections/opponents/organisation/search"
  })
  public String organisationSearch(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @ModelAttribute(ORGANISATION_SEARCH_CRITERIA) final OrganisationSearchCriteria searchCriteria,
      final HttpServletRequest request,
      final Model model) {

    addCaseContext(model, resolveCaseContext(caseContext, request));
    populateOrganisationSearchDropdowns(model);

    return "application/opponents/opponents-organisation-search";
  }

  /**
   * Processes the search form submission for organisations.
   *
   * @param searchCriteria The criteria used to search for organisations.
   * @param bindingResult Validation result of the search criteria form.
   * @param model The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/organisation/search",
    "/application/opponents/organisation/search",
    "/amendments/sections/opponents/organisation/search"
  })
  public String organisationSearch(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @Validated @ModelAttribute(ORGANISATION_SEARCH_CRITERIA)
          OrganisationSearchCriteria searchCriteria,
      BindingResult bindingResult,
      HttpServletRequest request,
      Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    searchCriteriaValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      addCaseContext(model, resolvedCaseContext);
      populateOrganisationSearchDropdowns(model);
      return "application/opponents/opponents-organisation-search";
    }

    return "redirect:/%s/sections/opponents/organisation/search/results"
        .formatted(resolvedCaseContext.getPathValue());
  }

  /**
   * Displays the search results for organisations based on specified criteria.
   *
   * @param searchCriteria Search criteria for finding organisations.
   * @param user The details of the currently authenticated user.
   * @param page Current page for pagination.
   * @param size Size of a page for pagination.
   * @param request The HttpServletRequest.
   * @param model Model to pass data to the view.
   * @return The view name for organisation search results or the appropriate error view.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/organisation/search/results",
    "/application/opponents/organisation/search/results",
    "/amendments/sections/opponents/organisation/search/results"
  })
  public String organisationSearchResults(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @ModelAttribute(ORGANISATION_SEARCH_CRITERIA) OrganisationSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      HttpServletRequest request,
      Model model) {
    ResultsDisplay<OrganisationResultRowDisplay> organisationResults;
    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    addCaseContext(model, resolvedCaseContext);

    try {
      organisationResults =
          opponentService.getOrganisations(
              searchCriteria, user.getLoginId(), user.getUserType(), page, size);

      if (organisationResults.getContent().isEmpty()) {
        return "application/opponents/opponents-organisation-search-no-results";
      }
    } catch (TooManyResultsException e) {
      return "application/opponents/opponents-organisation-search-too-many-results";
    }

    String currentUrl = request.getRequestURL().toString();
    model.addAttribute("currentUrl", currentUrl);

    model.addAttribute(ORGANISATION_SEARCH_RESULTS, organisationResults);

    return "application/opponents/opponents-organisation-search-results";
  }

  /**
   * Validates the selected opponent before displaying the 'shared organisation' screen to gather
   * final details for the opponent.
   *
   * @param organisationId The id of the selected organisation.
   * @param searchResults Search results containing organisations.
   * @param user The user details.
   * @param model The model.
   * @return The view name for shared organisation confirmation screen.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/organisation/{id}/select",
    "/application/opponents/organisation/{id}/select",
    "/amendments/sections/opponents/organisation/{id}/select"
  })
  public String selectSharedOrganisation(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @PathVariable("id") final String organisationId,
      @SessionAttribute(ORGANISATION_SEARCH_RESULTS)
          final ResultsDisplay<OrganisationResultRowDisplay> searchResults,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpServletRequest request,
      final Model model) {

    // Validate that the supplied organisation id is one from the search results in the session
    boolean validOrgId =
        searchResults.getContent().stream()
            .anyMatch(org -> org.getPartyId().equals(organisationId));

    if (!validOrgId) {
      log.error("Invalid organisation partyId {} supplied", organisationId);
      throw new CaabApplicationException("Invalid organisation partyId supplied");
    }

    // Retrieve the full Organisation details
    OrganisationOpponentFormData opponentFormData =
        opponentService.getOrganisationOpponent(
            organisationId, user.getLoginId(), user.getUserType());

    model.addAttribute(CURRENT_OPPONENT, opponentFormData);
    addCaseContext(model, resolveCaseContext(caseContext, request));

    populateConfirmSharedOrganisationDropdowns(model);

    return "application/opponents/opponents-organisation-shared-create";
  }

  /**
   * Validates the form data before adding a new shared organisation opponent to the application.
   *
   * @param opponentFormData the opponent form data.
   * @param applicationId The id of the opponents related application.
   * @param user The user details.
   * @param bindingResult - the binding result for validation.
   * @param model - the model.
   * @return The view name for shared organisation confirmation screen.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/organisation/shared/create",
    "/application/opponents/organisation/shared/create",
    "/amendments/sections/opponents/organisation/shared/create"
  })
  public String createSharedOrganisation(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @ModelAttribute(CURRENT_OPPONENT) final AbstractOpponentFormData opponentFormData,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      @SessionAttribute(value = APPLICATION_OPPONENTS, required = false)
          final List<?> applicationOpponents,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final BindingResult bindingResult,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    // Validate the complete opponent form data
    organisationOpponentValidator.validate(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      addCaseContext(model, resolvedCaseContext);
      populateConfirmSharedOrganisationDropdowns(model);
      return "application/opponents/opponents-organisation-shared-create";
    }

    if (isDuplicateSharedOrganisation(applicationId, applicationOpponents, opponentFormData)) {
      bindingResult.reject(
          "opponent.organisation.duplicate",
          "This organisation has already been added as an opponent.");
      addCaseContext(model, resolvedCaseContext);
      populateConfirmSharedOrganisationDropdowns(model);
      return "application/opponents/opponents-organisation-shared-create";
    }

    // Call the service to add the opponent to the application.
    applicationService.addOpponent(applicationId, opponentFormData, user);

    return redirectToOpponents(resolvedCaseContext);
  }

  /**
   * Displays the view to gather the form data for a new organisation opponent.
   *
   * @param model - the model
   * @return The view name for the organisation creation screen.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/organisation/create",
    "/application/opponents/organisation/create",
    "/amendments/sections/opponents/organisation/create"
  })
  public String createNewOrganisation(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      final HttpServletRequest request,
      final Model model) {

    addCaseContext(model, resolveCaseContext(caseContext, request));
    populateOrganisationCreateDropdowns(model);

    model.addAttribute(CURRENT_OPPONENT, new OrganisationOpponentFormData());

    return "application/opponents/opponents-organisation-create";
  }

  /**
   * Processes the form submission for creating a new organisation opponent.
   *
   * @param opponentFormData The form data to create the organisation.
   * @param applicationId The application id.
   * @param user The user details.
   * @param bindingResult Validation result of the form.
   * @param model The model used to pass data to the view.
   * @return Either redirects to the opponent list or reloads the form with validation errors.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/organisation/create",
    "/application/opponents/organisation/create",
    "/amendments/sections/opponents/organisation/create"
  })
  public String createNewOrganisation(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(CURRENT_OPPONENT) final AbstractOpponentFormData opponentFormData,
      final BindingResult bindingResult,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    organisationOpponentValidator.validate(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      addCaseContext(model, resolvedCaseContext);
      populateOrganisationCreateDropdowns(model);
      return "application/opponents/opponents-organisation-create";
    }

    applicationService.addOpponent(applicationId, opponentFormData, user);

    return redirectToOpponents(resolvedCaseContext);
  }

  /**
   * Displays the view to gather the form data for a new individual opponent.
   *
   * @param model - the model
   * @return The view name for the individual opponent creation screen.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/individual/create",
    "/application/opponents/individual/create",
    "/amendments/sections/opponents/individual/add"
  })
  public String createNewIndividual(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      final HttpServletRequest request,
      final Model model) {

    addCaseContext(model, resolveCaseContext(caseContext, request));
    populateIndividualCreateDropdowns(model);

    model.addAttribute(CURRENT_OPPONENT, new IndividualOpponentFormData());

    return "application/opponents/opponents-individual-create";
  }

  /**
   * Processes the form submission for creating a new organisation opponent.
   *
   * @param opponentFormData The form data to create the opponent.
   * @param applicationId The application id.
   * @param user The user details.
   * @param bindingResult Validation result of the form.
   * @param model The model used to pass data to the view.
   * @return Either redirects to the opponent list or reloads the form with validation errors.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/individual/create",
    "/application/opponents/individual/create",
    "/amendments/sections/opponents/individual/add"
  })
  public String createNewIndividual(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(CURRENT_OPPONENT)
          final IndividualOpponentFormData opponentFormData,
      final BindingResult bindingResult,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    // If the user has selected a relationship to case, we need to lookup this
    // record again to determine if date of birth is mandatory.
    if (StringUtils.hasText(opponentFormData.getRelationshipToCase())) {
      RelationshipToCaseLookupValueDetail relationshipToCase =
          lookupService
              .getPersonToCaseRelationship(opponentFormData.getRelationshipToCase())
              .map(
                  relationshipToCaseLookupValueDetail ->
                      relationshipToCaseLookupValueDetail.orElse(
                          new RelationshipToCaseLookupValueDetail()
                              .code(opponentFormData.getRelationshipToCase())
                              .description(opponentFormData.getRelationshipToCase())))
              .blockOptional()
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Failed to retrieve relationship to case with code: %s"
                              .formatted(opponentFormData.getRelationshipToCase())));

      opponentFormData.setDateOfBirthMandatory(relationshipToCase.getDateOfBirthMandatory());
    }

    individualOpponentValidator.validate(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      addCaseContext(model, resolvedCaseContext);
      populateIndividualCreateDropdowns(model);
      return "application/opponents/opponents-individual-create";
    }

    applicationService.addOpponent(applicationId, opponentFormData, user);

    return redirectToOpponents(resolvedCaseContext);
  }

  /**
   * Displays the view to edit the form data for an opponent.
   *
   * @param opponentId - the opponent id.
   * @param applicationOpponents - the list of opponents currently attached to the application.
   * @param model - the model
   * @return The view name for the appropriate opponent edit screen.
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/{opponent-id}/edit",
    "/application/opponents/{opponent-id}/edit",
    "/amendments/sections/opponents/individual/{opponent-id}/edit",
    "/amendments/sections/opponents/organisation/{opponent-id}/edit"
  })
  public String editOpponent(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @PathVariable("opponent-id") final Integer opponentId,
      @SessionAttribute(value = APPLICATION_OPPONENTS, required = false)
          final List<?> applicationOpponents,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    AbstractOpponentFormData currentOpponent =
        getCurrentOpponent(opponentId, applicationOpponents, applicationId);

    if (resolvedCaseContext.isAmendment() && Boolean.FALSE.equals(currentOpponent.getEditable())) {
      throw new CaabApplicationException("Original opponents cannot be edited.");
    }

    model.addAttribute(CURRENT_OPPONENT, currentOpponent);
    addCaseContext(model, resolvedCaseContext);

    if (currentOpponent instanceof OrganisationOpponentFormData organisationOpponentFormData) {
      if (Boolean.TRUE.equals(organisationOpponentFormData.getShared())) {
        populateConfirmSharedOrganisationDropdowns(model);

        return "application/opponents/opponents-organisation-shared-edit";
      } else {
        populateOrganisationCreateDropdowns(model);

        return "application/opponents/opponents-organisation-edit";
      }
    } else {
      populateIndividualCreateDropdowns(model);

      return "application/opponents/opponents-individual-edit";
    }
  }

  /**
   * Processes the form submission for editing an opponent.
   *
   * @param opponentId The opponent id.
   * @param currentOpponent The form data to edit the opponent.
   * @param user The user details.
   * @param bindingResult Validation result of the form.
   * @param model The model used to pass data to the view.
   * @return Either redirects to the opponent list or reloads the form with validation errors.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/{opponent-id}/edit",
    "/application/opponents/{opponent-id}/edit",
    "/amendments/sections/opponents/individual/{opponent-id}/edit",
    "/amendments/sections/opponents/organisation/{opponent-id}/edit"
  })
  public String editOpponent(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @PathVariable("opponent-id") final Integer opponentId,
      @ModelAttribute(CURRENT_OPPONENT) final AbstractOpponentFormData currentOpponent,
      final BindingResult bindingResult,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    if (currentOpponent instanceof OrganisationOpponentFormData organisationOpponentFormData) {
      // Validate organisation opponent
      organisationOpponentValidator.validate(currentOpponent, bindingResult);

      if (bindingResult.hasErrors()) {
        addCaseContext(model, resolvedCaseContext);
        if (Boolean.TRUE.equals(organisationOpponentFormData.getShared())) {
          populateConfirmSharedOrganisationDropdowns(model);
          return "application/opponents/opponents-organisation-shared-edit";
        } else {
          populateOrganisationCreateDropdowns(model);
          return "application/opponents/opponents-organisation-edit";
        }
      }
    } else {
      // Validate individual opponent.
      individualOpponentValidator.validate(currentOpponent, bindingResult);

      if (bindingResult.hasErrors()) {
        addCaseContext(model, resolvedCaseContext);
        populateIndividualCreateDropdowns(model);
        return "application/opponents/opponents-individual-edit";
      }
    }

    opponentService.updateOpponent(applicationId, opponentId, currentOpponent, user);

    return redirectToOpponents(resolvedCaseContext);
  }

  /**
   * Displays the confirmation view for removing an Opponent.
   *
   * @param opponentId - the opponent id.
   * @param applicationOpponents - the list of opponents currently attached to the application.
   * @param model - the model.
   * @return The view name for the remove confirmation screen
   */
  @GetMapping({
    "/{caseContext}/sections/opponents/{opponent-id}/remove",
    "/application/opponents/{opponent-id}/remove",
    "/amendments/sections/opponents/{opponent-id}/remove"
  })
  public String removeOpponent(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @PathVariable("opponent-id") final Integer opponentId,
      @SessionAttribute(value = APPLICATION_OPPONENTS, required = false)
          final List<?> applicationOpponents,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      final HttpServletRequest request,
      final Model model) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    AbstractOpponentFormData currentOpponent =
        getCurrentOpponent(opponentId, applicationOpponents, applicationId);

    if (!canRemoveOpponent(currentOpponent, resolvedCaseContext)) {
      throw new CaabApplicationException("Invalid Opponent Id: %s".formatted(opponentId));
    }

    model.addAttribute(CURRENT_OPPONENT, currentOpponent);
    model.addAttribute("opponent", currentOpponent);
    addCaseContext(model, resolvedCaseContext);

    return "application/opponents/opponents-remove";
  }

  /**
   * Removes an opponent from an application.
   *
   * @param opponentId - the opponent id.
   * @param applicationOpponents - the list of opponents currently attached to the application.
   * @param user - the user details.
   * @return a redirect to the opponent summary view.
   */
  @PostMapping({
    "/{caseContext}/sections/opponents/{opponent-id}/remove",
    "/application/opponents/{opponent-id}/remove",
    "/amendments/sections/opponents/{opponent-id}/remove"
  })
  public String removeOpponentPost(
      @PathVariable(value = "caseContext", required = false) final CaseContext caseContext,
      @PathVariable("opponent-id") final Integer opponentId,
      @SessionAttribute(value = APPLICATION_OPPONENTS, required = false)
          final List<?> applicationOpponents,
      @SessionAttribute(value = APPLICATION_ID, required = false) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpServletRequest request) {

    CaseContext resolvedCaseContext = resolveCaseContext(caseContext, request);
    AbstractOpponentFormData currentOpponent =
        getCurrentOpponent(opponentId, applicationOpponents, applicationId);

    if (!canRemoveOpponent(currentOpponent, resolvedCaseContext)) {
      throw new CaabApplicationException("Invalid Opponent Id: %s".formatted(opponentId));
    }

    opponentService.deleteOpponent(opponentId, user);

    return redirectToOpponents(resolvedCaseContext);
  }

  private CaseContext resolveCaseContext(final CaseContext caseContext) {
    return caseContext == null ? CaseContext.APPLICATION : caseContext;
  }

  private CaseContext resolveCaseContext(
      final CaseContext caseContext, final HttpServletRequest request) {
    if (caseContext != null) {
      return caseContext;
    }
    String path = request.getServletPath();
    if (!StringUtils.hasText(path)) {
      path = request.getRequestURI().substring(request.getContextPath().length());
    }
    return path.startsWith("/amendments/") ? CaseContext.AMENDMENTS : CaseContext.APPLICATION;
  }

  private void addCaseContext(final Model model, final CaseContext caseContext) {
    CaseContext resolvedCaseContext = resolveCaseContext(caseContext);
    model.addAttribute("caseContext", resolvedCaseContext);
    model.addAttribute("amendment", resolvedCaseContext.isAmendment());
  }

  private String redirectToOpponents(final CaseContext caseContext) {
    return "redirect:/%s/sections/opponents"
        .formatted(resolveCaseContext(caseContext).getPathValue());
  }

  private AbstractOpponentFormData getCurrentOpponent(
      final Integer opponentId, final List<?> applicationOpponents, final String applicationId) {
    if (applicationOpponents != null
        && applicationOpponents.stream().allMatch(AbstractOpponentFormData.class::isInstance)) {
      return applicationOpponents.stream()
          .map(AbstractOpponentFormData.class::cast)
          .filter(opponentFormData -> opponentFormData.getId().equals(opponentId))
          .findFirst()
          .orElseThrow(
              () -> new CaabApplicationException("Invalid Opponent Id: %s".formatted(opponentId)));
    }

    if (!StringUtils.hasText(applicationId)) {
      throw new CaabApplicationException("Application id is required to retrieve opponent");
    }

    return applicationService.getOpponents(applicationId).stream()
        .filter(opponentFormData -> opponentFormData.getId().equals(opponentId))
        .findFirst()
        .orElseThrow(
            () -> new CaabApplicationException("Invalid Opponent Id: %s".formatted(opponentId)));
  }

  private boolean isDuplicateSharedOrganisation(
      final String applicationId,
      final List<?> applicationOpponents,
      final AbstractOpponentFormData opponentFormData) {
    if (!(opponentFormData instanceof OrganisationOpponentFormData organisationOpponentFormData)
        || !Boolean.TRUE.equals(organisationOpponentFormData.getShared())
        || !StringUtils.hasText(organisationOpponentFormData.getPartyId())) {
      return false;
    }

    final List<AbstractOpponentFormData> existingOpponents =
        getExistingOpponentFormData(applicationId, applicationOpponents);

    return applicationService.hasSharedOrganisationOpponent(
            applicationId, organisationOpponentFormData.getPartyId())
        || existingOpponents.stream()
            .filter(OrganisationOpponentFormData.class::isInstance)
            .map(OrganisationOpponentFormData.class::cast)
            .anyMatch(
                existingOpponent ->
                    Boolean.TRUE.equals(existingOpponent.getShared())
                        && organisationOpponentFormData
                            .getPartyId()
                            .equals(existingOpponent.getPartyId()));
  }

  private List<AbstractOpponentFormData> getExistingOpponentFormData(
      final String applicationId, final List<?> applicationOpponents) {
    if (applicationOpponents != null
        && applicationOpponents.stream().allMatch(AbstractOpponentFormData.class::isInstance)) {
      return applicationOpponents.stream().map(AbstractOpponentFormData.class::cast).toList();
    }

    return List.of();
  }

  private boolean canRemoveOpponent(
      final AbstractOpponentFormData opponent, final CaseContext caseContext) {
    return resolveCaseContext(caseContext).isAmendment()
        ? Boolean.TRUE.equals(opponent.getEditable())
        : Boolean.TRUE.equals(opponent.getDeletable());
  }

  private void populateOrganisationSearchDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown(
            "organisationTypes", lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .build();
  }

  private void populateConfirmSharedOrganisationDropdowns(final Model model) {
    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
        lookupService
            .getOrganisationToCaseRelationships()
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));

    model.addAttribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent());

    new DropdownBuilder(model)
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .build();
  }

  private void populateOrganisationCreateDropdowns(final Model model) {
    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
        lookupService
            .getOrganisationToCaseRelationships()
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));
    model.addAttribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent());

    List<BooleanDropdownOption> currentlyTradingOptions =
        List.of(new BooleanDropdownOption("false", "No"), new BooleanDropdownOption("true", "Yes"));
    model.addAttribute("currentlyTradingOptions", currentlyTradingOptions);

    new DropdownBuilder(model)
        .addDropdown(
            "organisationTypes", lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .addDropdown("countries", lookupService.getCountries())
        .build();
  }

  private void populateIndividualCreateDropdowns(final Model model) {
    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
        lookupService
            .getPersonToCaseRelationships()
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));
    model.addAttribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent());

    List<BooleanDropdownOption> legalAidedOptions =
        List.of(new BooleanDropdownOption("false", "No"), new BooleanDropdownOption("true", "Yes"));
    model.addAttribute("legalAidedOptions", legalAidedOptions);

    new DropdownBuilder(model)
        .addDropdown("contactTitles", lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .addDropdown("countries", lookupService.getCountries())
        .build();
  }

  /** Option for boolean-backed dropdown fields. */
  public record BooleanDropdownOption(String code, String description) {}
}
