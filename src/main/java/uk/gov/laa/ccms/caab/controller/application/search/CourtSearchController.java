package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_RESULTS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CourtSearchValidator;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({COURT_SEARCH_CRITERIA})
public class CourtSearchController {

  private final CourtSearchValidator courtSearchValidator;

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
   * @return The court search view.
   */
  @GetMapping("/court/search")
  public String courtSearch() {

    return "application/court-search";
  }

  /**
   * POST method to look for paginated court details.
   *
   * @param searchCriteria Criteria for court search.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @PostMapping("/court/search")
  public String courtSearch(
      @ModelAttribute(COURT_SEARCH_CRITERIA) final CourtSearchCriteria searchCriteria,
      BindingResult bindingResult,
      final HttpSession session,
      Model model) {

    courtSearchValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/court-search";
    }

    CourtLookupDetail searchResult = null;
    try {
      searchResult = courtService.getCourtSearch(searchCriteria);

      if (searchResult.getContent().isEmpty()) {
        return "application/court-search-no-results";
      }

    } catch (EbsApiClientException e) {
      if (e.getMessage().contains(TOO_MANY_RESULTS)
          || (e.getCause() != null && e.getCause().getMessage().contains(TOO_MANY_RESULTS))) {
        return "application/court-search-too-many-results.html";
      }
      throw new CaabApplicationException("Error performing court search.", e);
    }

    session.setAttribute(COURT_SEARCH_RESULTS, searchResult.getContent());

    return "redirect:/court/results";
  }
}
