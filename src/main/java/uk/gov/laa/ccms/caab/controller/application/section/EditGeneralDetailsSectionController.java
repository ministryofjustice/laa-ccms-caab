package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.LINKED_CASES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_LINK_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.LinkedCaseValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for the application's general details section. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CASE_SEARCH_CRITERIA, CASE_SEARCH_RESULTS})
public class EditGeneralDetailsSectionController {

  // services
  private final ApplicationService applicationService;
  private final AddressService addressService;
  private final LookupService lookupService;
  private final ProviderService providerService;

  // validators
  private final FindAddressValidator findAddressValidator;
  private final AddressSearchValidator addressSearchValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;
  private final LinkedCaseValidator linkedCaseValidator;
  private final CaseSearchCriteriaValidator searchCriteriaValidator;

  // Mappers
  private final EbsApplicationMapper applicationMapper;
  private final ResultDisplayMapper resultDisplayMapper;

  private static final String ACTION_FIND_ADDRESS = "find_address";
  protected static final String CURRENT_URL = "currentUrl";
  protected static final String CASE_RESULTS_PAGE = "caseResultsPage";

  /**
   * Handles the GET request for editing an application's correspondence address.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param applicationId The id of the application
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/{caseContext}/sections/correspondence-address")
  public String correspondenceDetails(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      final Model model,
      final HttpSession session) {

    AddressFormData addressDetails = (AddressFormData) session.getAttribute("addressDetails");

    if (addressDetails == null) {
      if (caseContext.isAmendment()) {
        addressDetails =
            applicationService.getCorrespondenceAddressFormData(ebsCase.getCorrespondenceAddress());
      } else {
        addressDetails = applicationService.getCorrespondenceAddressFormData(applicationId);
      }
      session.setAttribute("addressDetails", addressDetails);
    }

    model.addAttribute("addressDetails", addressDetails);

    populateCorrespondenceAddressDropdowns(model);

    return "application/sections/correspondence-address-details";
  }

  /**
   * Handles the POST request for editing an application's correspondence address.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param action The action performed, which button pressed. Either "find_address" or
   *     "save_address".
   * @param applicationId The id of the application
   * @param addressDetails The address details model.
   * @param user The user details.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return The view name for the application summary page.
   */
  @PostMapping("/{caseContext}/sections/correspondence-address")
  public String updateCorrespondenceDetails(
      @PathVariable final CaseContext caseContext,
      @RequestParam(required = false) final String action,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute("addressDetails") final AddressFormData addressDetails,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    if (ACTION_FIND_ADDRESS.equals(action)) {
      findAddressValidator.validate(addressDetails, bindingResult);
    } else {
      correspondenceAddressValidator.validate(addressDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateCorrespondenceAddressDropdowns(model);
      session.setAttribute("addressDetails", addressDetails);
      return "application/sections/correspondence-address-details";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      ResultsDisplay<AddressResultRowDisplay> addressSearchResults =
          addressService.getAddresses(addressDetails.getPostcode());

      if (addressSearchResults.getContent() == null) {
        bindingResult.reject(
            "address.none", "Your input for address details has not returned any results");
      } else {
        addressSearchResults =
            addressService.filterByHouseNumber(
                addressDetails.getHouseNameNumber(), addressSearchResults);
        session.setAttribute(ADDRESS_SEARCH_RESULTS, addressSearchResults);
      }

      if (bindingResult.hasErrors()) {
        populateCorrespondenceAddressDropdowns(model);
        session.setAttribute("addressDetails", addressDetails);
        return "application/sections/correspondence-address-details";
      }

      session.setAttribute("addressDetails", addressDetails);
      return "redirect:/%s/sections/correspondence-address/search"
          .formatted(caseContext.getPathValue());

    } else {
      // TODO: Submit address amendments: https://dsdmoj.atlassian.net/browse/CCMSPUI-527
      if (caseContext.isAmendment()) {
        throw new NotImplementedException(
            "Submission of correspondence address amendments is not yet implemented.");
      }
      applicationService.updateCorrespondenceAddress(applicationId, addressDetails, user);
      session.removeAttribute("addressDetails");

      return "redirect:/application/sections/linked-cases";
    }
  }

  private void populateCorrespondenceAddressDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("countries", lookupService.getCountries())
        .addDropdown(
            "caseAddressOptions", lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .build();
  }

  /**
   * Handles the GET request for edit correspondence address search page.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param addressResultsDisplay the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/{caseContext}/sections/correspondence-address/search")
  public String correspondenceAddressGet(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(ADDRESS_SEARCH_RESULTS)
          final ResultsDisplay<AddressResultRowDisplay> addressResultsDisplay,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model) {

    model.addAttribute(ADDRESS_SEARCH_RESULTS, addressResultsDisplay);
    model.addAttribute(
        "formAction",
        "%s/sections/correspondence-address/search".formatted(caseContext.getPathValue()));

    return "application/sections/address-search-results";
  }

  /**
   * Handles the correspondence address results submission.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param addressResultsDisplay the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/{caseContext}/sections/correspondence-address/search")
  public String correspondenceAddressSearchPost(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(ADDRESS_SEARCH_RESULTS)
          final ResultsDisplay<AddressResultRowDisplay> addressResultsDisplay,
      @SessionAttribute("addressDetails") final AddressFormData addressDetails,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    // validate if an address is selected
    addressSearchValidator.validate(addressSearch, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressResultsDisplay);
      model.addAttribute(
          "formAction",
          "%s/sections/correspondence-address/search".formatted(caseContext.getPathValue()));
      return "application/sections/address-search-results";
    }

    // Cleanup
    session.removeAttribute(ADDRESS_SEARCH_RESULTS);

    addressService.filterAndUpdateAddressFormData(
        addressSearch.getUprn(), addressResultsDisplay, addressDetails);

    return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
  }

  /**
   * Handles the GET request for retrieving linked cases associated with an application.
   *
   * @param applicationId the ID of the application, retrieved from the session attribute
   * @param model the Spring Model to pass attributes to the view
   * @param session the HttpSession object
   * @return the path to the linked cases summary view
   */
  @GetMapping("/{caseContext}/sections/linked-cases")
  public String linkedCasesGet(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @PathVariable final CaseContext caseContext,
      final Model model,
      final HttpSession session) {
    log.info("Getting linked-cases for context {}", caseContext);
    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases =
        applicationService.getLinkedCases(applicationId);

    model.addAttribute(LINKED_CASES, linkedCases);
    session.setAttribute(LINKED_CASES, linkedCases);
    model.addAttribute(CASE_SEARCH_CRITERIA, new CaseSearchCriteria());

    return "application/sections/application-linked-case-summary";
  }

  /**
   * Handles the GET request for displaying the page to remove a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case removal view
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(LINKED_CASES) final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    log.info(
        "Getting linked-case to remove with id: {} caseContext: {}", linkedCaseId, caseContext);
    final LinkedCaseResultRowDisplay linkedCase =
        linkedCases.getContent() == null
            ? null
            : linkedCases.getContent().stream()
                .filter(lc -> linkedCaseId.equals(lc.getId()))
                .findFirst()
                .orElse(null);

    model.addAttribute("linkedCase", linkedCase);

    return "application/sections/application-linked-case-remove";
  }

  /**
   * Handles the POST request for removing a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param user the user details from the session attribute
   * @return a redirect path to the linked cases summary
   */
  @PostMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    log.info(
        "Removing linked-case to remove with id: {} caseContext: {}", linkedCaseId, caseContext);
    applicationService.removeLinkedCase(linkedCaseId, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Handles the GET request for displaying the page to confirm details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case confirmation view
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(LINKED_CASES) final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    log.info(
        "Getting linked-case to confirm with id: {} caseContext: {}", linkedCaseId, caseContext);
    final LinkedCaseResultRowDisplay linkedCase =
        linkedCases.getContent() == null
            ? null
            : linkedCases.getContent().stream()
                .filter(lc -> linkedCaseId.equals(lc.getId()))
                .findFirst()
                .orElse(null);

    model.addAttribute("currentLinkedCase", linkedCase);
    model.addAttribute("linkedCaseId", linkedCaseId);

    populateLinkedCasesConfirmDropdowns(model);

    return "application/sections/application-linked-case-confirm";
  }

  /**
   * Handles the POST request for confirming details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param user the user details from the session attribute
   * @param linkedCase the LinkedCaseResultRowDisplay object containing linked case details
   * @param bindingResult the BindingResult to capture validation errors
   * @param model the Spring Model to pass attributes to the view
   * @return a redirect path to the linked cases summary or the confirmation view on error
   */
  @PostMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("currentLinkedCase") final LinkedCaseResultRowDisplay linkedCase,
      final BindingResult bindingResult,
      final Model model) {

    log.info("Confirming linked-case with id: {} caseContext: {}", linkedCaseId, caseContext);
    linkedCaseValidator.validate(linkedCase, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("currentLinkedCase", linkedCase);
      model.addAttribute("linkedCaseId", linkedCaseId);
      populateLinkedCasesConfirmDropdowns(model);
      return "application/sections/application-linked-case-confirm";
    }

    applicationService.updateLinkedCase(linkedCaseId, linkedCase, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Populates the model with dropdown options for confirming a linked case.
   *
   * @param model the Spring Model to pass attributes for dropdowns
   */
  private void populateLinkedCasesConfirmDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("caseLinkOptions", lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
        .build();
  }

  /**
   * Handles the GET request for searching linked cases.
   *
   * @param userDetails The details of the logged-in user.
   * @param model The UI model to add attributes to.
   * @return String The view name for linked cases search.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/search")
  public String linkedCasesSearchGet(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      @PathVariable final CaseContext caseContext,
      final Model model) {

    populateLinkedCasesSearchDropdowns(userDetails, model);

    return "application/sections/application-linked-case-search";
  }

  /**
   * Handles the POST request for searching linked cases, validates the search criteria, and
   * processes search results.
   *
   * @param activeCase Active case details from session attribute.
   * @param caseSearchCriteria Search criteria model attribute.
   * @param user User details from session attribute.
   * @param currentLinkedCases The current Linked cases for the application.
   * @param redirectAttributes Redirect attributes.
   * @param bindingResult Binding result for validation.
   * @param model Spring MVC model.
   * @return The view name for redirection or linked case search view.
   */
  @PostMapping("/{caseContext}/sections/linked-cases/search")
  public String linkedCasesSearchPost(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(LINKED_CASES)
          final ResultsDisplay<LinkedCaseResultRowDisplay> currentLinkedCases,
      final RedirectAttributes redirectAttributes,
      @Validated @ModelAttribute(CASE_SEARCH_CRITERIA) final CaseSearchCriteria caseSearchCriteria,
      final BindingResult bindingResult,
      final Model model) {

    log.info("Searching for linked-case with id: {} caseContext: {}", caseContext, caseContext);
    searchCriteriaValidator.validate(caseSearchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      populateLinkedCasesSearchDropdowns(user, model);
      return "application/sections/application-linked-case-search";
    }

    final List<BaseApplicationDetail> searchResults;

    try {
      searchResults = applicationService.getCases(caseSearchCriteria, user);

      // filter out current linked cases and where application id is the same as the current
      // application
      searchResults.removeIf(
          result ->
              currentLinkedCases.getContent().stream()
                      .anyMatch(
                          lc -> lc.getLscCaseReference().equals(result.getCaseReferenceNumber()))
                  || result.getCaseReferenceNumber().equals(activeCase.getCaseReferenceNumber()));

      if (searchResults.isEmpty()) {
        return "application/sections/application-linked-case-search-no-results";
      }
    } catch (final TooManyResultsException e) {
      return "application/sections/application-linked-case-search-too-many-results";
    }

    redirectAttributes.addFlashAttribute(CASE_SEARCH_RESULTS, searchResults);

    return "redirect:/%s/sections/linked-cases/search/results"
        .formatted(caseContext.getPathValue());
  }

  /**
   * Displays the search results for linked cases with pagination.
   *
   * @param page Requested page number.
   * @param size Size of the page.
   * @param caseSearchResults List of search results.
   * @param request HTTP servlet request.
   * @param model Spring MVC model.
   * @param session HTTP session.
   * @return The view name for search results.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/search/results")
  public String linkedCasesSearchResults(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @PathVariable final CaseContext caseContext,
      @ModelAttribute(CASE_SEARCH_RESULTS) final List<BaseApplicationDetail> caseSearchResults,
      final HttpServletRequest request,
      final Model model,
      final HttpSession session) {

    log.info("Displaying linked cases search results for caseContext: {}", caseContext);
    // Paginate the results list, and convert to the Page wrapper object for display
    final ApplicationDetails linkedCaseSearchResults =
        applicationMapper.toApplicationDetails(
            PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), caseSearchResults));

    model.addAttribute(CURRENT_URL, request.getRequestURL().toString());

    model.addAttribute(CASE_RESULTS_PAGE, linkedCaseSearchResults);
    session.setAttribute(CASE_RESULTS_PAGE, linkedCaseSearchResults);
    return "application/sections/application-linked-case-search-results";
  }

  /**
   * Handles the GET request to initiate the addition of a linked case based on selected case
   * reference ID.
   *
   * @param caseReferenceId The case reference ID path variable.
   * @param linkedCaseSearchResults the results from the linked case search.=.
   * @param model Spring MVC model.
   * @return The view name for adding a linked case.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{case-reference-id}/add")
  public String addLinkedCaseGet(
      @PathVariable("case-reference-id") final String caseReferenceId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(CASE_RESULTS_PAGE) final ApplicationDetails linkedCaseSearchResults,
      final Model model) {

    log.info(
        "Adding linked case with case reference: {} for caseContext: {}",
        caseReferenceId,
        caseContext);
    final BaseApplicationDetail baseApplication =
        linkedCaseSearchResults.getContent() == null
            ? null
            : linkedCaseSearchResults.getContent().stream()
                .filter(lc -> caseReferenceId.equals(lc.getCaseReferenceNumber()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Unable to add linked case with case reference: %s"
                                .formatted(caseReferenceId)));

    // map base application to linked case
    final LinkedCaseResultRowDisplay linkedCase =
        resultDisplayMapper.toLinkedCaseResultRowDisplay(baseApplication);

    model.addAttribute("currentLinkedCase", linkedCase);
    populateLinkedCasesConfirmDropdowns(model);

    return "application/sections/application-linked-case-add";
  }

  /**
   * Processes the POST request for adding a linked case, validating the provided linked case
   * details.
   *
   * @param applicationId Application ID from session attribute.
   * @param user User details from session attribute.
   * @param linkedCase Linked case model attribute.
   * @param bindingResult Binding result for validation.
   * @param model Spring MVC model.
   * @return The redirection view name upon successful addition, or the view name to re-add upon
   *     validation errors.
   */
  @PostMapping("/{caseContext}/sections/linked-cases/add")
  public String addLinkedCasePost(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final CaseContext caseContext,
      @ModelAttribute("currentLinkedCase") final LinkedCaseResultRowDisplay linkedCase,
      final BindingResult bindingResult,
      final Model model) {

    log.info(
        "Adding linked case with case reference: {} for caseContext: {}",
        applicationId,
        caseContext);
    linkedCaseValidator.validate(linkedCase, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("currentLinkedCase", linkedCase);
      populateLinkedCasesConfirmDropdowns(model);
      return "application/sections/application-linked-case-add";
    }

    applicationService.addLinkedCase(applicationId, linkedCase, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Populates the model with dropdown options and additional attributes for linked case search.
   *
   * @param user the user details containing provider information
   * @param model the Spring Model to pass attributes for dropdowns and additional data
   */
  private void populateLinkedCasesSearchDropdowns(final UserDetail user, final Model model) {
    final Tuple2<ProviderDetail, CaseStatusLookupDetail> combinedResults =
        Optional.ofNullable(
                Mono.zip(
                        providerService.getProvider(user.getProvider().getId()),
                        lookupService.getCaseStatusValues())
                    .block())
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));

    final ProviderDetail providerDetail = combinedResults.getT1();
    final CaseStatusLookupDetail caseStatusLookupDetail = combinedResults.getT2();

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(providerDetail));
    model.addAttribute("offices", user.getProvider().getOffices());
    model.addAttribute("statuses", caseStatusLookupDetail.getContent());

    new DropdownBuilder(model)
        .addDropdown("Status", lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS))
        .build();

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(providerDetail));
    model.addAttribute("offices", user.getProvider().getOffices());
  }
}
