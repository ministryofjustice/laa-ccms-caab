package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_BASELINE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_BASELINES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_OUTCOME_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COURT;

import jakarta.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
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
  protected static final int MAX_COURT_RESULTS = 100;

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
      final HttpSession session,
      Model model) {

    final String searchToken = UUID.randomUUID().toString();
    storeBaseline(session, searchToken, searchCriteria);
    model.addAttribute("proceedingIndex", proceedingIndex);
    model.addAttribute("searchToken", searchToken);
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
      @RequestParam(value = "searchToken", required = false) final String searchToken,
      @ModelAttribute(COURT_SEARCH_CRITERIA) final CourtSearchCriteria searchCriteria,
      BindingResult bindingResult,
      final HttpSession session,
      Model model) {

    CourtSearchCriteria baselineCriteria = resolveBaselineCriteria(session, searchToken);

    clearStaleCourtFilter(searchCriteria, baselineCriteria);
    if (searchCriteria.getCourtCode() == null) {
      searchCriteria.setCourtCode("");
    }
    if (searchCriteria.getCourtName() == null) {
      searchCriteria.setCourtName("");
    }

    courtSearchValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("proceedingIndex", proceedingIndex);
      model.addAttribute("searchToken", searchToken);
      return "application/court-search";
    }

    CommonLookupDetail result = null;

    try {
      result =
          lookupService
              .getCourts(searchCriteria.getCourtCode(), searchCriteria.getCourtName())
              .block();

      if (result == null || result.getContent() == null || result.getContent().isEmpty()) {
        clearBaseline(session, searchToken);
        model.addAttribute("proceedingIndex", proceedingIndex);
        return "application/court-search-no-results";
      }

      if (result.getContent().size() > MAX_COURT_RESULTS) {
        clearBaseline(session, searchToken);
        model.addAttribute("proceedingIndex", proceedingIndex);
        return "application/court-search-too-many-results";
      }

    } catch (EbsApiClientException e) {
      if (e.getMessage().contains(TOO_MANY_RESULTS)
          || (e.getCause() != null && e.getCause().getMessage().contains(TOO_MANY_RESULTS))) {
        clearBaseline(session, searchToken);
        model.addAttribute("proceedingIndex", proceedingIndex);
        return "application/court-search-too-many-results";
      }
      throw new CaabApplicationException("Error performing court search.", e);
    }

    clearBaseline(session, searchToken);
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

    String searchUrl = "/court/results?proceedingIndex=" + proceedingIndex;
    model.addAttribute(CURRENT_URL, searchUrl);
    model.addAttribute(COURT_RESULTS_PAGE, pagedResults);

    model.addAttribute("proceedingIndex", proceedingIndex);
    return "application/court-search-results";
  }

  private void clearStaleCourtFilter(
      final CourtSearchCriteria searchCriteria, final CourtSearchCriteria existingCriteria) {
    if (existingCriteria == null) {
      return;
    }

    final boolean codeChanged =
        StringUtils.hasText(searchCriteria.getCourtCode())
            && !Objects.equals(searchCriteria.getCourtCode(), existingCriteria.getCourtCode());
    final boolean nameChanged =
        StringUtils.hasText(searchCriteria.getCourtName())
            && !Objects.equals(searchCriteria.getCourtName(), existingCriteria.getCourtName());

    if (codeChanged
        && StringUtils.hasText(existingCriteria.getCourtName())
        && Objects.equals(searchCriteria.getCourtName(), existingCriteria.getCourtName())) {
      searchCriteria.setCourtName(null);
      return;
    }

    if (nameChanged
        && StringUtils.hasText(existingCriteria.getCourtCode())
        && Objects.equals(searchCriteria.getCourtCode(), existingCriteria.getCourtCode())) {
      searchCriteria.setCourtCode(null);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, CourtSearchCriteria> getBaselineMap(final HttpSession session) {
    Object baselines = session.getAttribute(COURT_SEARCH_BASELINES);
    if (baselines instanceof Map<?, ?> map) {
      return (Map<String, CourtSearchCriteria>) map;
    }

    Map<String, CourtSearchCriteria> baselineMap = new LinkedHashMap<>();
    session.setAttribute(COURT_SEARCH_BASELINES, baselineMap);
    return baselineMap;
  }

  private void storeBaseline(
      final HttpSession session, final String searchToken, final CourtSearchCriteria criteria) {
    if (searchToken == null) {
      return;
    }

    final CourtSearchCriteria storedCriteria = copyCriteria(criteria);
    Map<String, CourtSearchCriteria> baselines = getBaselineMap(session);
    baselines.put(searchToken, storedCriteria);
    session.setAttribute(baselineAttributeKey(searchToken), storedCriteria);
    while (baselines.size() > 10) {
      Iterator<String> iterator = baselines.keySet().iterator();
      if (iterator.hasNext()) {
        String oldestKey = iterator.next();
        iterator.remove();
        session.removeAttribute(baselineAttributeKey(oldestKey));
      }
    }
  }

  private void clearBaseline(final HttpSession session, final String searchToken) {
    if (searchToken == null) {
      return;
    }

    Map<String, CourtSearchCriteria> baselines = getBaselineMap(session);
    baselines.remove(searchToken);
    session.removeAttribute(baselineAttributeKey(searchToken));
    if (baselines.isEmpty()) {
      session.removeAttribute(COURT_SEARCH_BASELINES);
    }
  }

  private CourtSearchCriteria resolveBaselineCriteria(
      final HttpSession session, final String searchToken) {
    if (searchToken != null) {
      final CourtSearchCriteria tokenBaseline =
          (CourtSearchCriteria) session.getAttribute(baselineAttributeKey(searchToken));
      if (tokenBaseline != null) {
        return tokenBaseline;
      }

      final CourtSearchCriteria mapBaseline = getBaselineMap(session).get(searchToken);
      if (mapBaseline != null) {
        return mapBaseline;
      }
    }

    final CourtSearchCriteria legacyBaseline =
        (CourtSearchCriteria) session.getAttribute(COURT_SEARCH_BASELINE);
    if (legacyBaseline != null) {
      return legacyBaseline;
    }

    CourtSearchCriteria baselineCriteria =
        copyCriteria((CourtSearchCriteria) session.getAttribute(COURT_SEARCH_CRITERIA));
    final ProceedingOutcomeFormData proceedingOutcomeFormData =
        (ProceedingOutcomeFormData) session.getAttribute(PROCEEDING_OUTCOME_FORM_DATA);
    if (baselineCriteria == null || bothMissing(baselineCriteria)) {
      baselineCriteria = new CourtSearchCriteria();
    }
    if (proceedingOutcomeFormData != null) {
      proceedingOutcomeFormData.normaliseLegacyCourtFields();
      baselineCriteria.setCourtCode(proceedingOutcomeFormData.getCourtCode());
      baselineCriteria.setCourtName(proceedingOutcomeFormData.getCourtName());
    }
    return baselineCriteria;
  }

  private String baselineAttributeKey(final String searchToken) {
    return COURT_SEARCH_BASELINE + ":" + searchToken;
  }

  private CourtSearchCriteria copyCriteria(final CourtSearchCriteria criteria) {
    if (criteria == null) {
      return null;
    }
    final CourtSearchCriteria copy = new CourtSearchCriteria();
    copy.setCourtCode(criteria.getCourtCode());
    copy.setCourtName(criteria.getCourtName());
    return copy;
  }

  private boolean bothMissing(final CourtSearchCriteria criteria) {
    return !StringUtils.hasText(criteria.getCourtCode())
        && !StringUtils.hasText(criteria.getCourtName());
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

    session.removeAttribute(SELECTED_COURT);

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
