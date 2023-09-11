package uk.gov.laa.ccms.caab.controller.application;


import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for handling category of law selection in the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class CategoryOfLawController {

  private final ApplicationDetailsValidator applicationValidator;

  private final ProviderService providerService;

  private final CommonLookupService commonLookupService;

  /**
   * Handles the GET request for category of law selection page.
   *
   * @param exceptionalFunding A flag indicating exceptional funding.
   * @param applicationDetails The application details from session.
   * @param userDetails The user details from session.
   * @param model The model for the view.
   * @return The view name for the category of law selection page.
   */
  @GetMapping("/application/category-of-law")
  public String categoryOfLaw(
          @RequestParam(value = "exceptional_funding", defaultValue = "false")
                boolean exceptionalFunding,
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          Model model) {
    log.info("GET /application/category-of-law: {}", applicationDetails);

    applicationDetails.setExceptionalFunding(exceptionalFunding);

    initialiseCategoriesOfLaw(applicationDetails, userDetails, model);

    return "application/select-category-of-law";
  }


  /**
   * Handles the POST request for category of law selection form submission.
   *
   * @param applicationDetails The application details from session.
   * @param userDetails The user details from session.
   * @param bindingResult The result of data binding/validation.
   * @param model The model for the view.
   * @return A redirect or view name based on validation result and exceptional funding.
   */
  @PostMapping("/application/category-of-law")
  public String categoryOfLaw(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          BindingResult bindingResult,
          Model model) {
    log.info("POST /application/category-of-law: {}", applicationDetails);
    applicationValidator.validateCategoryOfLaw(applicationDetails, bindingResult);

    String viewName = "redirect:/application/application-type";
    if (bindingResult.hasErrors()) {
      initialiseCategoriesOfLaw(applicationDetails, userDetails, model);
      viewName = "application/select-category-of-law";
    } else if (applicationDetails.isExceptionalFunding()) {
      // Exception Funding has been selected, so initialise the ApplicationType to ECF
      // and bypass the ApplicationType screen.
      applicationDetails.setApplicationTypeCategory(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
      viewName = "redirect:/application/client/search";
    }

    return viewName;
  }

  /**
   * Initializes the categories of law for the view.
   *
   * @param applicationDetails The application details.
   * @param user The user details.
   * @param model The model for the view.
   */
  private void initialiseCategoriesOfLaw(ApplicationDetails applicationDetails,
                                         UserDetail user, Model model) {

    List<CommonLookupValueDetail> categoriesOfLaw =
        Optional.ofNullable(commonLookupService.getCategoriesOfLaw().block())
            .orElse(new CommonLookupDetail())
            .getContent();

    if (!applicationDetails.isExceptionalFunding()) {
      List<String> categoryOfLawCodes = providerService.getCategoryOfLawCodes(
              user.getProvider().getId(),
              applicationDetails.getOfficeId(),
              user.getLoginId(),
              user.getUserType(),
              Boolean.TRUE);

      categoriesOfLaw.retainAll(categoriesOfLaw
          .stream().filter(category -> categoryOfLawCodes.contains(category.getCode()))
          .collect(Collectors.toList()));
    }

    model.addAttribute("categoriesOfLaw", categoriesOfLaw);
  }
}
