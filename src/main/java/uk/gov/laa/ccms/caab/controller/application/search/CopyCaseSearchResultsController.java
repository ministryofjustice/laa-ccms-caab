package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller responsible for handling the search results of copy cases.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    APPLICATION_FORM_DATA,
    CASE_SEARCH_CRITERIA,
    CASE_SEARCH_RESULTS})
public class CopyCaseSearchResultsController {

  private final ApplicationService applicationService;

  private final ApplicationMapper applicationMapper;

  /**
   * Displays the search results of copy cases.
   *
   * @param page Page number for pagination.
   * @param size Size of results per page.
   * @param caseSearchCriteria Criteria used for the search.
   * @param user Current logged in user.
   * @param request The HTTP request.
   * @param model Model to store attributes for the view.
   * @return The appropriate view based on the search results.
   */
  @GetMapping("/application/copy-case/results")
  public String copyCaseSearchResults(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria caseSearchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      HttpServletRequest request,
      Model model) {

    // Get the Copy Case Status
    CaseStatusLookupValueDetail copyCaseStatus = applicationService.getCopyCaseStatus();
    if (copyCaseStatus != null) {
      caseSearchCriteria.setStatus(copyCaseStatus.getCode());
    }

    List<BaseApplication> searchResults;

    try {
      searchResults = applicationService.getCases(caseSearchCriteria,
          user.getLoginId(),
          user.getUserType());

      if (searchResults.isEmpty()) {
        return "application/application-copy-case-search-no-results";
      }
    } catch (TooManyResultsException e) {
      return "application/application-copy-case-search-too-many-results";
    }

    // Now paginate the list, and convert to the Page wrapper object for display
    ApplicationDetails applicationDetails = applicationMapper.toApplicationDetails(
        PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), searchResults));

    String currentUrl = request.getRequestURL().toString();
    model.addAttribute("currentUrl", currentUrl);
    model.addAttribute(CASE_SEARCH_RESULTS, applicationDetails);
    return "application/application-copy-case-search-results";
  }

  /**
   * Validates and selects a specific copy case reference number from the search results.
   *
   * @param copyCaseReferenceNumber The reference number of the selected copy case.
   * @param searchResults Search results containing copy cases.
   * @param applicationFormData Details of the current application.
   * @return Redirects to the client search page after storing the selected case reference number.
   */
  @GetMapping("/application/copy-case/{case-reference-number}/confirm")
  public String selectCopyCaseReferenceNumber(
      @PathVariable("case-reference-number") String copyCaseReferenceNumber,
      @SessionAttribute(CASE_SEARCH_RESULTS) ApplicationDetails searchResults,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData) {

    // Validate that the supplied caseRef is one from the search results in the session
    boolean validCaseRef = Optional.ofNullable(searchResults.getContent())
        .orElse(Collections.emptyList())
        .stream().anyMatch(
            application -> application.getCaseReferenceNumber().equals(copyCaseReferenceNumber));

    if (!validCaseRef) {
      log.error("Invalid copyCaseReferenceNumber {} supplied", copyCaseReferenceNumber);
      throw new CaabApplicationException("Invalid copyCaseReferenceNumber supplied");
    }

    // Store the selected caseReferenceNumber in the ApplicationDetails.
    // This will be used at the point the Application is created.
    applicationFormData.setCopyCaseReferenceNumber(copyCaseReferenceNumber);
    return "redirect:/application/client/search";
  }
}

