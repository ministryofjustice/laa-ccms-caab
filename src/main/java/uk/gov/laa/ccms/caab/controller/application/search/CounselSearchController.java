package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.ALL_PARAMS_EMPTY;
import static uk.gov.laa.ccms.caab.constants.CounselLookupConstants.TOO_MANY_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_RESULTS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CounselSearchValidator;
import uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.mapper.CounselLookupMapper;
import uk.gov.laa.ccms.caab.model.CategoryDetail;
import uk.gov.laa.ccms.caab.service.CounselService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.CounselLookupDetail;
import uk.gov.laa.ccms.data.model.CounselLookupValueDetail;

/** Controller class for Counsel search. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {COUNSEL_SEARCH_CRITERIA, COUNSEL_SEARCH_RESULTS})
public class CounselSearchController {

  private final CounselSearchValidator counselSearchValidator;
  private final CounselService counselService;
  private final CounselLookupMapper counselLookupMapper;
  private final EbsApiClientErrorHandler ebsApiClientErrorHandler;

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
   * @param allocateCostsFormData Cost details model attribute.
   * @param model Model (MVC) to pass data to view.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @RequestMapping(value = "/application/counsel", method = RequestMethod.GET)
  public String getCounsel(
      @ModelAttribute(COUNSEL_SEARCH_CRITERIA) final CounselSearchCriteria searchCriteria,
      @ModelAttribute("costDetails") AllocateCostsFormData allocateCostsFormData,
      Model model,
      final BindingResult bindingResult) {

    CounselSearchCriteria counselSearchCriteria = getCounselSearchCriteria();

    model.addAttribute("counselSearchCriteria", counselSearchCriteria);
    model.addAttribute("categoryList", populateCategoryList());
    return "application/counsel-search";
  }

  /**
   * POST method to looks for paginated counsel details.
   *
   * @param page Default page 0 to show when not specified.
   * @param size Default size 10 to show page when not specified.
   * @param searchCriteria Criteria for counsel search.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @RequestMapping(value = "/lookup/counsels", method = RequestMethod.POST)
  public String counselLookup(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @ModelAttribute(COUNSEL_SEARCH_CRITERIA) final CounselSearchCriteria searchCriteria,
      BindingResult bindingResult,
      final RedirectAttributes redirectAttributes) {

    counselSearchValidator.validate(searchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/counsel-search";
    }

    CounselLookupDetail searchResult = null;
    try {
      searchResult = counselService.getCounselSearch(searchCriteria);

      if (searchResult.getContent().isEmpty()) {
        return "application/counsel-search-no-results";
      }

    } catch (EbsApiClientException e) {
      if (e.getMessage().equals(TOO_MANY_RESULTS)) {
        return "application/counsel-search-too-many-results";
      } else if (e.getMessage().equals(ALL_PARAMS_EMPTY)) {
        return "application/counsel-search-all-param-empty";
      } else {
        ebsApiClientErrorHandler.handleApiRetrieveError(
            e,
            "Counsel Search",
            new LinkedMultiValueMap<>() {
              {
                add("name", searchCriteria.getName());
                add("company", searchCriteria.getCompany());
                add("legal_aid_supplier_number", searchCriteria.getLaaCounselReference());
                add("category", searchCriteria.getCategory());
              }
            });
      }
    }

    redirectAttributes.addFlashAttribute(COUNSEL_SEARCH_RESULTS, searchResult.getContent());

    return "redirect:/lookup/counsels";
  }

  /**
   * POST method to looks for paginated counsel details.
   *
   * @param page Default page 0 to show when not specified.
   * @param size Default size 10 to show page when not specified.
   * @param lookupValueDetails The full un-paginated search results list.
   * @param model Model (MVC) to pass data to view.
   * @return View name in terms of string value.
   */
  @RequestMapping(value = "/lookup/counsels", method = RequestMethod.GET)
  public String counselLookupGet(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @ModelAttribute(COUNSEL_SEARCH_RESULTS)
          final List<@Valid CounselLookupValueDetail> lookupValueDetails,
      final HttpServletRequest request,
      final HttpSession httpSession,
      Model model) {

    CounselLookupDetail counselLookupDetail =
        counselLookupMapper.toCounselLookupDetail(
            PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), lookupValueDetails));

    String queryString = request.getQueryString();
    String searchUrl = request.getRequestURL().toString();
    if (queryString != null && !queryString.isBlank()) {
      searchUrl += "?" + queryString;
    }

    httpSession.setAttribute(SEARCH_URL, searchUrl);

    model.addAttribute(COUNSEL_RESULTS_PAGE, counselLookupDetail);

    return "application/counsel-search-results";
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
