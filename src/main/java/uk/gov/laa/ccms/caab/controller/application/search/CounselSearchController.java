package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.COUNSEL_COST_CATEGORY;
import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COUNSEL;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CounselSearchValidator;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.CounselLookupMapper;
import uk.gov.laa.ccms.caab.model.CategoryDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.service.CounselService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

/** Controller class for Counsel search. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {COUNSEL_SEARCH_CRITERIA, COST_ALLOCATION_FORM_DATA})
public class CounselSearchController {

  private final CounselSearchValidator counselSearchValidator;
  private final CounselService counselService;
  private final CounselLookupMapper counselLookupMapper;
  private static final String SEARCH_URL = "SEARCH_URL";
  protected static final String CURRENT_URL = "currentUrl";

  protected static final String COUNSEL_RESULTS_PAGE = "counselResultsPage";

  /**
   * Provides an instance of {@link CounselSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link CounselSearchCriteria}.
   */
  @ModelAttribute(COUNSEL_SEARCH_CRITERIA)
  public CounselSearchCriteria getCounselSearchCriteria() {
    return new CounselSearchCriteria();
  }

  /**
   * GET method for showing the counsel search screen.
   *
   * @param searchCriteria Criteria for counsel search.
   * @param model Model (MVC) to pass data to view.
   * @return View name in terms of string value.
   */
  @GetMapping("/counsel/search")
  public String getCounsel(
      @ModelAttribute(COUNSEL_SEARCH_CRITERIA) final CounselSearchCriteria searchCriteria,
      Model model) {

    model.addAttribute("counselSearchCriteria", getCounselSearchCriteria());
    model.addAttribute("categoryList", populateCategoryList());
    return "application/counsel-search";
  }

  /**
   * POST method to look for paginated counsel details.
   *
   * @param searchCriteria Criteria for counsel search.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @PostMapping("/counsel/search")
  public String counselLookup(
      @ModelAttribute(COUNSEL_SEARCH_CRITERIA) final CounselSearchCriteria searchCriteria,
      BindingResult bindingResult,
      final HttpSession session,
      Model model) {

    counselSearchValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("categoryList", populateCategoryList());
      return "application/counsel-search";
    }

    CounselLookupDetail searchResult = null;
    try {
      searchResult = counselService.getCounselSearch(searchCriteria);

      if (searchResult.getContent().isEmpty()) {
        return "application/counsel-search-no-results";
      }

    } catch (EbsApiClientException e) {
      if (e.getMessage().contains(TOO_MANY_RESULTS)
          || (e.getCause() != null && e.getCause().getMessage().contains(TOO_MANY_RESULTS))) {
        return "application/counsel-search-too-many-results";
      }
      throw new CaabApplicationException("Error performing counsel search.", e);
    }

    session.setAttribute(COUNSEL_SEARCH_RESULTS, searchResult.getContent());

    return "redirect:/counsel/results";
  }

  /**
   * GET method to look for paginated counsel details.
   *
   * @param page Default page 0 to show when not specified.
   * @param size Default size 10 to show page when not specified.
   * @param model Model (MVC) to pass data to view.
   * @return View name in terms of string value.
   */
  @GetMapping("/counsel/results")
  public String counselLookupGet(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      final HttpServletRequest request,
      final HttpSession httpSession,
      Model model) {

    @SuppressWarnings("unchecked")
    List<CounselLookupValueDetail> lookupValueDetails =
        (List<CounselLookupValueDetail>) httpSession.getAttribute(COUNSEL_SEARCH_RESULTS);

    if (lookupValueDetails == null) {
      return "redirect:/counsel/search";
    }

    final CounselLookupDetail counselLookupDetail =
        counselLookupMapper.toCounselLookupDetail(
            PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), lookupValueDetails));

    String searchUrl = "/counsel/results";
    model.addAttribute(CURRENT_URL, searchUrl);

    model.addAttribute(COUNSEL_RESULTS_PAGE, counselLookupDetail);

    return "application/counsel-search-results";
  }

  /**
   * GET method for selecting a counsel from the search results.
   *
   * @param index The index of the selected counsel in the search results.
   * @param session The current HTTP session.
   * @return A redirect to the counsel confirmation screen.
   */
  @GetMapping("/counsel/select")
  public String selectCounsel(@RequestParam("index") int index, HttpSession session) {

    @SuppressWarnings("unchecked")
    List<CounselLookupValueDetail> lookupValueDetails =
        (List<CounselLookupValueDetail>) session.getAttribute(COUNSEL_SEARCH_RESULTS);

    if (lookupValueDetails != null && index >= 0 && index < lookupValueDetails.size()) {
      CounselLookupValueDetail selectedCounsel = lookupValueDetails.get(index);
      log.debug("Selecting counsel: {}", selectedCounsel.getName());
      session.setAttribute(SELECTED_COUNSEL, selectedCounsel);
    }

    return "redirect:/application/counsel/confirm";
  }

  /**
   * GET method for showing the counsel confirmation screen.
   *
   * @param selectedCounsel The selected counsel from session.
   * @param model Model (MVC) to pass data to view.
   * @return View name for counsel confirmation.
   */
  @GetMapping("/application/counsel/confirm")
  public String confirmCounselGet(
      @SessionAttribute(value = SELECTED_COUNSEL, required = false)
          final CounselLookupValueDetail selectedCounsel,
      Model model) {

    if (selectedCounsel == null) {
      return "redirect:/application/counsel";
    }

    if (!model.containsAttribute("counsel")) {
      model.addAttribute("counsel", selectedCounsel);
    }
    return "application/counsel-confirm";
  }

  /**
   * POST method for confirming the selected counsel.
   *
   * @param selectedCounsel The selected counsel from session.
   * @param session The current HTTP session.
   * @return A redirect to the allocate cost limit screen.
   */
  @PostMapping("/application/counsel/confirm")
  public String confirmCounselPost(
      @SessionAttribute(SELECTED_COUNSEL) CounselLookupValueDetail selectedCounsel,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    AllocateCostsFormData allocateCostsFormData =
        (AllocateCostsFormData) session.getAttribute(COST_ALLOCATION_FORM_DATA);

    if (allocateCostsFormData == null) {
      log.warn("Expected session attribute '{}' not found", COST_ALLOCATION_FORM_DATA);
      return "redirect:/allocate-cost-limit";
    }

    if (allocateCostsFormData.getCostEntries() == null) {
      allocateCostsFormData.setCostEntries(new ArrayList<>());
    }

    // Check for duplicate counsel
    if (isDuplicateCounsel(allocateCostsFormData, selectedCounsel)) {
      BindingResult bindingResult = new BeanPropertyBindingResult(selectedCounsel, "counsel");
      bindingResult.reject("counsel.confirm.duplicate");
      redirectAttributes.addFlashAttribute(
          BindingResult.MODEL_KEY_PREFIX + "counsel", bindingResult);
      redirectAttributes.addFlashAttribute("counsel", selectedCounsel);
      return "redirect:/application/counsel/confirm";
    }

    CostEntryDetail newCostEntry = new CostEntryDetail();
    newCostEntry.setResourceName(selectedCounsel.getName());
    newCostEntry.setLscResourceId(selectedCounsel.getLegalAidSupplierNumber());
    newCostEntry.setCostCategory(COUNSEL_COST_CATEGORY);
    newCostEntry.setAmountBilled(BigDecimal.ZERO);
    newCostEntry.setRequestedCosts(BigDecimal.ZERO);
    newCostEntry.setNewEntry(true);

    if (allocateCostsFormData.getCostEntries() == null) {
      allocateCostsFormData.setCostEntries(new ArrayList<>());
    }
    allocateCostsFormData.getCostEntries().add(newCostEntry);

    session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);
    session.removeAttribute(SELECTED_COUNSEL);

    return "redirect:/allocate-cost-limit";
  }

  /**
   * Checks if the selected counsel is already present in the cost allocation.
   *
   * @param formData The current cost allocation form data.
   * @param selectedCounsel The counsel to check for.
   * @return true if the counsel is already present, false otherwise.
   */
  private boolean isDuplicateCounsel(
      final AllocateCostsFormData formData, final CounselLookupValueDetail selectedCounsel) {
    if (formData.getCostEntries() == null) {
      return false;
    }

    return formData.getCostEntries().stream()
        .anyMatch(
            entry -> {
              boolean refMatch =
                  Objects.equals(
                      entry.getLscResourceId() != null ? entry.getLscResourceId().trim() : null,
                      selectedCounsel.getLegalAidSupplierNumber() != null
                          ? selectedCounsel.getLegalAidSupplierNumber().trim()
                          : null);
              boolean nameMatch =
                  entry.getResourceName() != null
                      && selectedCounsel.getName() != null
                      && entry
                          .getResourceName()
                          .trim()
                          .equalsIgnoreCase(selectedCounsel.getName().trim());
              return refMatch || nameMatch;
            });
  }

  /**
   * Method populateCategoryList() to get the category lists.
   *
   * @return List of category details.
   */
  @ModelAttribute
  public List<CategoryDetail> populateCategoryList() {

    List<CategoryDetail> categoryList = new ArrayList<>();

    categoryList.add(
        new CategoryDetail() {
          {
            setCode("QC");
            setDescription("QC");
          }
        });

    categoryList.add(
        new CategoryDetail() {
          {
            setCode("Junior");
            setDescription("Junior");
          }
        });

    return categoryList;
  }
}
