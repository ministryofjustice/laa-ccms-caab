package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COPY_CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;


/**
 * Controller responsible for managing the search operations related to copy cases.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, COPY_CASE_SEARCH_CRITERIA})
public class CopyCaseSearchController {

  private final CopyCaseSearchCriteriaValidator searchCriteriaValidator;

  private final DataService dataService;

  /**
   * Provides an instance of {@link CopyCaseSearchCriteria} for use in the model.
   *
   * @return A new instance of {@link CopyCaseSearchCriteria}.
   */
  @ModelAttribute(COPY_CASE_SEARCH_CRITERIA)
  public CopyCaseSearchCriteria getClientSearchDetails() {
    return new CopyCaseSearchCriteria();
  }

  /**
   * Displays the copy case search form.
   *
   * @param searchCriteria The search criteria used for finding copy cases.
   * @param userDetails The details of the currently authenticated user.
   * @param model The model used to pass data to the view.
   * @return The copy case search view.
   */

  @GetMapping("/application/copy-case/search")
  public String copyCaseSearch(
          @ModelAttribute(COPY_CASE_SEARCH_CRITERIA) CopyCaseSearchCriteria searchCriteria,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          Model model) {
    log.info("GET /application/copy-case/search");

    model.addAttribute("feeEarners",
            getFeeEarners(userDetails.getProvider().getId()));
    model.addAttribute("offices",
            userDetails.getProvider().getOffices());

    return "application/application-copy-case-search";
  }

  /**
   * Processes the search form submission for copy cases.
   *
   * @param searchCriteria The criteria used to search for copy cases.
   * @param userDetails The details of the currently authenticated user.
   * @param bindingResult Validation result of the search criteria form.
   * @param model The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/application/copy-case/search")
  public String copyCaseSearch(
          @ModelAttribute(COPY_CASE_SEARCH_CRITERIA) CopyCaseSearchCriteria searchCriteria,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          BindingResult bindingResult,
          Model model) {
    log.info("POST /application/copy-case/search: criteria={}", searchCriteria.toString());

    searchCriteriaValidator.validate(searchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute("feeEarners",
              getFeeEarners(userDetails.getProvider().getId()));
      model.addAttribute("offices",
              userDetails.getProvider().getOffices());
      return "application/application-copy-case-search";
    }
    return "redirect:application/copy-case/results";
  }

  /**
   * Fetches the list of fee earners associated with a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return List of contact details representing fee earners.
   */
  private List<ContactDetail> getFeeEarners(Integer providerId) {
    FeeEarnerDetail feeEarners = dataService.getFeeEarners(providerId).block();
    return Optional.ofNullable(feeEarners)
            .map(FeeEarnerDetail::getContent)
            .orElse(Collections.emptyList());
  }
}
