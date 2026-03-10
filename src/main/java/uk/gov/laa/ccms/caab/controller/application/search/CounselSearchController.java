package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CounselSearchValidator;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CategoryDetail;
import uk.gov.laa.ccms.caab.service.CounselService;

/** Controller class for Counsel search. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CounselSearchController {

  private final CounselSearchValidator counselSearchValidator;
  private final CounselService counselService;

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

    // Allocation Cost Limit Validation
    // allocateCostLimitValidator.validate(allocateCostsFormData, bindingResult);
    // Store billed amount somewhere if validation is successful

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
   * @param ebsCase Application details received from the calling page.
   * @param model Model (MVC) to pass data to view.
   * @param bindingResult handler validation errors.
   * @return View name in terms of string value.
   */
  @RequestMapping(value = "/lookup/counsels", method = RequestMethod.POST)
  public String counselLookup(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @ModelAttribute(COUNSEL_SEARCH_CRITERIA) final CounselSearchCriteria searchCriteria,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      Model model,
      BindingResult bindingResult) {

    CounselSearchCriteria criteria = getCounselSearchCriteria();
    counselSearchValidator.validate(criteria, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/counsel-search";
    }

    return "application/counsel-search";
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
