package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;


/**
 * Controller responsible for managing the searching of applications and cases.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {CASE_SEARCH_CRITERIA})
public class ApplicationSearchController {
  private final ProviderService providerService;

  private final ApplicationService applicationService;

  private final LookupService lookupService;

  private final CaseSearchCriteriaValidator searchCriteriaValidator;

  /**
   * Provides an instance of {@link CaseSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link CaseSearchCriteria}.
   */
  @ModelAttribute(CASE_SEARCH_CRITERIA)
  public CaseSearchCriteria getCaseSearchCriteria() {
    return new CaseSearchCriteria();
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
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail userDetails,
      Model model) {

    populateDropdowns(userDetails, model);

    return "application/application-case-search";
  }

  /**
   * Processes the search form submission for applications and cases.
   *
   * @param searchCriteria The criteria used to search for applications and cases.
   * @param userDetails    The details of the currently authenticated user.
   * @param bindingResult  Validation result of the search criteria form.
   * @param model          The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/application/search")
  public String applicationSearch(
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail userDetails,
      BindingResult bindingResult,
      Model model) {

    searchCriteriaValidator.validate(searchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      populateDropdowns(userDetails, model);
      return "application/application-case-search";
    }
    return "redirect:/application/search/results";
  }

  private final SearchConstants searchConstants;

  /**
   * Displays the search results of applications and cases.
   *
   * @param page Page number for pagination.
   * @param size Size of results per page.
   * @param caseSearchCriteria Criteria used for the search.
   * @param user Current logged in user.
   * @param request The HTTP request.
   * @param model Model to store attributes for the view.
   * @return The appropriate view based on the search results.
   */
  @GetMapping("/application/search/results")
  public String applicationSearchResults(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria caseSearchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      HttpServletRequest request,
      Model model) {

    Page<BaseApplication> searchResults = Page.empty();

    try {
      searchResults = applicationService.getCases(caseSearchCriteria,
          user.getLoginId(),
          user.getUserType(),
          page,
          size);

      if (!searchResults.hasContent()) {
        return "application/application-copy-case-search-no-results";
      }
    } catch (TooManyResultsException e) {
      return "application/application-case-search-too-many-results";
    }

    String currentUrl = request.getRequestURL().toString();
    model.addAttribute("currentUrl", currentUrl);
    model.addAttribute(CASE_SEARCH_RESULTS, searchResults);
    return "application/application-copy-case-search-results";
  }

  private void populateDropdowns(UserDetail user, Model model) {
    Tuple2<ProviderDetail, CaseStatusLookupDetail> combinedResults =
        Optional.ofNullable(Mono.zip(
            providerService.getProvider(user.getProvider().getId()),
            lookupService.getCaseStatusValues()).block()).orElseThrow(
                () -> new RuntimeException("Failed to retrieve lookup data"));

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
