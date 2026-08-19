package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COURT;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CourtSearchValidator;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({COURT_SEARCH_CRITERIA})
public class CourtSearchController {

  private final CourtSearchValidator courtSearchValidator;
  private final LookupService lookupService;

  protected static final String CURRENT_URL = "currentUrl";
  protected static final String COURT_RESULTS_PAGE = "courtResultsPage";

  /**
   * Provides an instance of {@link CourtSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link CourtSearchCriteria}.
   */
  @ModelAttribute(COURT_SEARCH_CRITERIA)
  public CourtSearchCriteria getCourtSearchCriteria() {
    return new CourtSearchCriteria();
  }

  /**
   * Displays the court search screen.
   *
   * @param proceedingIndex The index of the proceeding.
   * @param searchCriteria The court search criteria.
   * @param model The model used to pass data to the view.
   * @return The court search view.
   */
  @GetMapping("/court/search")
  public String courtSearch(
      @RequestParam(value = "proceedingIndex") final int proceedingIndex,
      @ModelAttribute(COURT_SEARCH_CRITERIA) final CourtSearchCriteria searchCriteria,
      Model model) {

    model.addAttribute("proceedingIndex", proceedingIndex);
    return "application/court-search";
  }

  /**
   * POST method to look for paginated court details.
   *
   * @param proceedingIndex The index of the proceeding.
   * @param searchCriteria Criteria for court search.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @PostMapping("/court/search")
  public String courtSearch(
      @RequestParam(value = "proceedingIndex") final int proceedingIndex,
      @ModelAttribute(COURT_SEARCH_CRITERIA) final CourtSearchCriteria searchCriteria,
      BindingResult bindingResult,
      final HttpSession session,
      Model model) {

    courtSearchValidator.validate(searchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute("proceedingIndex", proceedingIndex);
      return "application/court-search";
    }

    CommonLookupDetail result = null;

    try {
      result =
          lookupService
              .getCourts(searchCriteria.getCourtCode(), searchCriteria.getCourtName())
              .block();

      if (result == null || result.getContent() == null || result.getContent().isEmpty()) {
        model.addAttribute("proceedingIndex", proceedingIndex);
        return "application/court-search-no-results";
      }

    } catch (EbsApiClientException e) {
      if (e.getMessage().contains(TOO_MANY_RESULTS)
          || (e.getCause() != null && e.getCause().getMessage().contains(TOO_MANY_RESULTS))) {
        model.addAttribute("proceedingIndex", proceedingIndex);
        return "application/court-search-too-many-results";
      }
      throw new CaabApplicationException("Error performing court search.", e);
    }

    session.setAttribute(COURT_SEARCH_RESULTS, result.getContent());
    return "redirect:/court/results?proceedingIndex=" + proceedingIndex;
  }

  /**
   * GET method to look for paginated court details.
   *
   * @param proceedingIndex The index of the proceeding.
   * @param page Default page 0 to show when not specified.
   * @param size Default size 10 to show page when not specified.
   * @param model Model (MVC) to pass data to view.
   * @return View name in terms of string value.
   */
  @GetMapping("/court/results")
  public String courtLookupGet(
      @RequestParam(value = "proceedingIndex") final int proceedingIndex,
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      final HttpSession httpSession,
      Model model) {

    @SuppressWarnings("unchecked")
    List<CommonLookupValueDetail> lookupValueDetails =
        (List<CommonLookupValueDetail>) httpSession.getAttribute(COURT_SEARCH_RESULTS);

    if (lookupValueDetails == null) {
      return "redirect:/court/search?proceedingIndex=" + proceedingIndex;
    }

    var pagedResults =
        PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), lookupValueDetails);

    String searchUrl = "/court/results";
    model.addAttribute(CURRENT_URL, searchUrl);
    model.addAttribute(COURT_RESULTS_PAGE, pagedResults);

    model.addAttribute("proceedingIndex", proceedingIndex);
    return "application/court-search-results";
  }

  /**
   * GET method for selecting a court from the search results.
   *
   * @param index The index of the selected court in the search results.
   * @param session The current HTTP session.
   * @param proceedingIndex The index of the proceeding.
   * @return A redirect to the court confirmation screen.
   */
  @GetMapping("/court/select")
  public String selectCourt(
      @RequestParam("index") int index,
      @RequestParam("proceedingIndex") int proceedingIndex,
      HttpSession session) {

    @SuppressWarnings("unchecked")
    List<CommonLookupValueDetail> lookupValueDetails =
        (List<CommonLookupValueDetail>) session.getAttribute(COURT_SEARCH_RESULTS);

    if (lookupValueDetails != null && index >= 0 && index < lookupValueDetails.size()) {
      CommonLookupValueDetail selectedCourt = lookupValueDetails.get(index);
      log.debug("Selecting court: {}", selectedCourt.getCode());
      session.setAttribute(SELECTED_COURT, selectedCourt);
    }

    return "redirect:/case/outcome-and-awards/proceeding/" + proceedingIndex + "/outcome";
  }
}
