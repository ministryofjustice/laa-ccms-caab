package uk.gov.laa.ccms.caab.controller.application.search;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

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
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;


/**
 * Controller responsible for managing the search operations related to copy cases.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {CASE_SEARCH_CRITERIA})
public class CopyCaseSearchController {
  private final ProviderService providerService;

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
   * Displays the copy case search form.
   *
   * @param searchCriteria The search criteria used for finding copy cases.
   * @param userDetails    The details of the currently authenticated user.
   * @param model          The model used to pass data to the view.
   * @return The copy case search view.
   */

  @GetMapping("/application/copy-case/search")
  public String copyCaseSearch(
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail userDetails,
      Model model) {

    populateDropdowns(userDetails, model);

    return "application/application-copy-case-search";
  }

  /**
   * Processes the search form submission for copy cases.
   *
   * @param searchCriteria The criteria used to search for copy cases.
   * @param userDetails    The details of the currently authenticated user.
   * @param bindingResult  Validation result of the search criteria form.
   * @param model          The model used to pass data to the view.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/application/copy-case/search")
  public String copyCaseSearch(
      @ModelAttribute(CASE_SEARCH_CRITERIA) CaseSearchCriteria searchCriteria,
      @SessionAttribute(USER_DETAILS) UserDetail userDetails,
      BindingResult bindingResult,
      Model model) {

    searchCriteriaValidator.validate(searchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      populateDropdowns(userDetails, model);
      return "application/application-copy-case-search";
    }
    return "redirect:/application/copy-case/results";
  }

  private void populateDropdowns(UserDetail user, Model model) {
    ProviderDetail provider = providerService.getProvider(user.getProvider().getId()).block();
    if (provider == null) {
      throw new CaabApplicationException(
          String.format("Failed to retrieve Provider with id: %s",
              user.getProvider().getId()));
    }

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(provider));
    model.addAttribute("offices", user.getProvider().getOffices());
  }
}
