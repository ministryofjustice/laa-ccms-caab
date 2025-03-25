package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.feature.Feature;
import uk.gov.laa.ccms.caab.feature.FeatureService;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller responsible for managing the searching of applications and cases.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    CASE_SEARCH_CRITERIA,
    CASE_SEARCH_RESULTS})
public class ApplicationSearchController {
  private final ProviderService providerService;

  private final FeatureService featureService;

  private final LookupService lookupService;

  private final ApplicationService applicationService;

  private final EbsApplicationMapper applicationMapper;

  private final CaseSearchCriteriaValidator searchCriteriaValidator;

  protected static final String CURRENT_URL = "currentUrl";

  protected static final String CASE_RESULTS_PAGE = "caseResultsPage";
  private final CaseSearchCriteriaValidator caseSearchCriteriaValidator;

  /**
   * Provides an instance of {@link CaseSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link CaseSearchCriteria}.
   */
  @ModelAttribute(CASE_SEARCH_CRITERIA)
  public CaseSearchCriteria getCaseSearchCriteria() {
    return new CaseSearchCriteria();
  }

  @InitBinder(CASE_SEARCH_CRITERIA)
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(caseSearchCriteriaValidator);
  }
  /**
   * Displays the application or case search form.
   *
   * @param searchCriteria The search criteria used for finding applications and cases.
   * @param userDetails    The details of the currently authenticated user.
   * @param model          The model used to pass data to the view.
   * @return The application case search view.
   */

  @GetMapping("/application/search")
  public String applicationSearch(
      @ModelAttribute(CASE_SEARCH_CRITERIA) final CaseSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      final Model model) {

    populateDropdowns(userDetails, model);

    return "application/application-search";
  }

  /**
   * Processes the search form submission for applications and cases.
   *
   * @param caseSearchCriteria The criteria used to search for applications and cases.
   * @param user    The details of the currently authenticated user.
   * @param bindingResult  Validation result of the search criteria form.
   * @param model          The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/application/search")
  public String applicationSearch(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @Validated @ModelAttribute(CASE_SEARCH_CRITERIA) final CaseSearchCriteria caseSearchCriteria,
      BindingResult bindingResult, Model model,
      final RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      populateDropdowns(user, model);
      return "application/application-search";
    }

    List<BaseApplicationDetail> searchResults;

    try {
      searchResults = applicationService.getCases(caseSearchCriteria, user);

      if (searchResults.isEmpty()) {
        return "application/application-search-no-results";
      }
    } catch (TooManyResultsException e) {
      return "application/application-search-too-many-results";
    }

    redirectAttributes.addFlashAttribute(CASE_SEARCH_RESULTS, searchResults);

    return "redirect:/application/search/results";
  }

  /**
   * Displays the search results of applications and cases.
   *
   * @param page Page number for pagination.
   * @param size Size of results per page.
   * @param caseSearchResults The full un-paginated search results list.
   * @param request The HTTP request.
   * @param model Model to store attributes for the view.
   * @return The appropriate view based on the search results.
   */
  @GetMapping("/application/search/results")
  public String applicationSearchResults(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @ModelAttribute(CASE_SEARCH_RESULTS) final List<BaseApplicationDetail> caseSearchResults,
      final HttpServletRequest request,
      final Model model) {

    // Paginate the results list, and convert to the Page wrapper object for display
    ApplicationDetails applicationDetails = applicationMapper.toApplicationDetails(
        PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), caseSearchResults));

    model.addAttribute(CURRENT_URL,  request.getRequestURL().toString());
    model.addAttribute(CASE_RESULTS_PAGE, applicationDetails);
    model.addAttribute("amendmentsEnabled", featureService.isEnabled(Feature.AMENDMENTS));
    return "application/application-search-results";
  }

  /**
   * Redirects to the correct endpoint to view a Case or Application.
   *
   * @param caseReferenceNumber The caseReferenceNumber of the application of case to view.
   * @param caseSearchResults The full un-paginated search results list.
   * @return The appropriate redirect based on the type of application or case selected.
   */
  @GetMapping("/application/{case-reference-number}/view")
  public String applicationCaseView(
      @PathVariable("case-reference-number") final String caseReferenceNumber,
      @SessionAttribute(CASE_SEARCH_RESULTS) final List<BaseApplicationDetail> caseSearchResults,
      HttpSession session) {

    // First ensure that the supplied caseReferenceNumber refers to an
    // application/case from the search results in the session.
    final BaseApplicationDetail selectedApplication =
        caseSearchResults.stream()
            .filter(baseApplication -> baseApplication.getCaseReferenceNumber().equals(
                caseReferenceNumber))
            .findFirst()
            .orElseThrow(() -> new CaabApplicationException(
                String.format("Invalid case reference: %s", caseReferenceNumber)));

    featureService.featureRequired(Feature.AMENDMENTS,
        () -> Boolean.TRUE.equals(selectedApplication.getAmendment()));

    //
    // TODO: Spike CCLS-2120 to investigate poll and cleanup of pending submissions.
    //

    if (STATUS_UNSUBMITTED_ACTUAL_VALUE.equals(selectedApplication.getStatus().getId())) {
      session.setAttribute(APPLICATION_ID, selectedApplication.getId());
      return "redirect:/application/sections";
    } else {
      session.setAttribute(CASE_REFERENCE_NUMBER, selectedApplication.getCaseReferenceNumber());
      return "redirect:/case/summary/todo";
    }
  }


  private void populateDropdowns(UserDetail user, Model model) {
    Tuple2<ProviderDetail, CaseStatusLookupDetail> combinedResults =
        Optional.ofNullable(Mono.zip(
            providerService.getProvider(user.getProvider().getId()),
            lookupService.getCaseStatusValues()).block()).orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve lookup data"));

    ProviderDetail providerDetail = combinedResults.getT1();
    CaseStatusLookupDetail caseStatusLookupDetail = combinedResults.getT2();

    model.addAttribute("feeEarners",
        providerService.getAllFeeEarners(providerDetail));
    model.addAttribute("offices",
        user.getProvider().getOffices());
    model.addAttribute("statuses",
        caseStatusLookupDetail.getContent());
  }
}
