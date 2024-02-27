package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.bean.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.OpponentOrganisationValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.OpponentRowDisplay;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's opponents and other parties section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    ORGANISATION_SEARCH_CRITERIA,
    ORGANISATION_SEARCH_RESULTS,
    CURRENT_OPPONENT})
public class OpponentsSectionController {

  private final ApplicationService applicationService;

  private final OpponentService opponentService;

  private final LookupService lookupService;

  private final OrganisationSearchCriteriaValidator searchCriteriaValidator;

  private final OpponentOrganisationValidator opponentOrganisationValidator;


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
   * Handles the GET request to fetch and display the opponents and other parties for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param model The Model object to add attributes to for the view.
   * @param session The HttpSession object to add attributes to for the session.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/summary/opponents")
  public String opponents(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model,
      final HttpSession session) {

    final ResultsDisplay<OpponentRowDisplay> opponents =
        applicationService.getOpponents(applicationId);

    model.addAttribute("opponents", opponents);
    session.setAttribute("opponents", opponents);

    return "application/summary/opponents-section";
  }

  /**
   * Handles the GET request to display the organisation search screen.
   *
   * @param searchCriteria The organisation search criteria.
   * @param model The Model object to add attributes to for the view.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/opponents/organisation/search")
  public String organisationSearch(
      @ModelAttribute(ORGANISATION_SEARCH_CRITERIA) final OrganisationSearchCriteria searchCriteria,
      final Model model) {

    populateOrganisationSearchDropdowns(model);

    return "application/opponents/opponents-organisation-search";
  }

  /**
   * Processes the search form submission for organisations.
   *
   * @param searchCriteria The criteria used to search for organisations.
   * @param bindingResult  Validation result of the search criteria form.
   * @param model          The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/application/opponents/organisation/search")
  public String organisationSearch(
      @ModelAttribute(ORGANISATION_SEARCH_CRITERIA) OrganisationSearchCriteria searchCriteria,
      BindingResult bindingResult,
      Model model) {

    searchCriteriaValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      populateOrganisationSearchDropdowns(model);
      return "application/opponents/opponents-organisation-search";
    }

    return "redirect:/application/opponents/organisation/search/results";
  }

  /**
   * Displays the search results for organisations based on specified criteria.
   *
   * @param searchCriteria      Search criteria for finding organisations.
   * @param user                The details of the currently authenticated user.
   * @param page                Current page for pagination.
   * @param size                Size of a page for pagination.
   * @param request             The HttpServletRequest.
   * @param model               Model to pass data to the view.
   * @return The view name for organisation search results or the appropriate error view.
   */
  @GetMapping("/application/opponents/organisation/search/results")
  public String organisationSearchResults(
      @ModelAttribute(ORGANISATION_SEARCH_CRITERIA) OrganisationSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      HttpServletRequest request,
      Model model) {
    ResultsDisplay<OrganisationResultRowDisplay> organisationResults;

    try {
      organisationResults = opponentService.getOrganisations(searchCriteria,
          user.getLoginId(),
          user.getUserType(),
          page,
          size);

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
   * Validates the selected opponent before displaying the 'shared organisation' screen
   * to gather final details for the opponent.
   *
   * @param organisationId The id of the selected organisation.
   * @param searchResults Search results containing organisations.
   * @return The view name for shared organisation confirmation screen.
   */
  @GetMapping("/application/opponents/organisation/{id}/select")
  public String selectSharedOrganisation(
      @PathVariable("id") String organisationId,
      @SessionAttribute(ORGANISATION_SEARCH_RESULTS)
      ResultsDisplay<OrganisationResultRowDisplay> searchResults,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      Model model) {

    // Validate that the supplied organisation id is one from the search results in the session
    boolean validOrgId =
        searchResults.getContent().stream().anyMatch(
            org -> org.getPartyId().equals(organisationId));

    if (!validOrgId) {
      log.error("Invalid organisation partyId {} supplied", organisationId);
      throw new CaabApplicationException("Invalid organisation partyId supplied");
    }

    // Retrieve the full Organisation details
    OpponentFormData opponentFormData = opponentService.getOrganisationOpponent(
        organisationId,
        user.getLoginId(),
        user.getUserType());

    model.addAttribute(CURRENT_OPPONENT, opponentFormData);

    populateConfirmSharedOrganisationDropdowns(model);

    return "application/opponents/opponents-organisation-shared-confirm";
  }

  /**
   * Validates the selected opponent before displaying the 'shared organisation' screen
   * to gather final details for the opponent.
   *
   * @param opponentFormData the opponent form data.
   * @param applicationId The id of the opponents related application.
   * @param bindingResult - the binding result for validation.
   * @param model - the model.
   * @return The view name for shared organisation confirmation screen.
   */
  @PostMapping("/application/opponents/organisation/confirm")
  public String confirmSharedOrganisation(
      @ModelAttribute(CURRENT_OPPONENT) OpponentFormData opponentFormData,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final BindingResult bindingResult,
      Model model) {

    // Validate the complete opponent form data
    opponentOrganisationValidator.validate(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateConfirmSharedOrganisationDropdowns(model);
      return "application/opponents/opponents-organisation-shared-confirm";
    }

    // Call the service to add the opponent to the application.
    applicationService.addOpponent(
        applicationId,
        opponentFormData,
        user);

    return "redirect:/application/summary/opponents";
  }

  private void populateOrganisationSearchDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("organisationTypes",
            lookupService.getOrganisationTypes())
        .build();
  }

  private void populateConfirmSharedOrganisationDropdowns(final Model model) {
    Tuple2<RelationshipToCaseLookupDetail, CommonLookupDetail> combinedLookup =
        Optional.ofNullable(Mono.zip(
            lookupService.getOrganisationToCaseRelationships(),
            lookupService.getRelationshipsToClient()).block())
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));

    model.addAttribute("relationshipsToCase", combinedLookup.getT1().getContent());
    model.addAttribute("relationshipsToClient", combinedLookup.getT2().getContent());
  }

}
