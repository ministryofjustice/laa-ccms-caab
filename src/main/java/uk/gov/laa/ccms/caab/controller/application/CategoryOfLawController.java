package uk.gov.laa.ccms.caab.controller.application;


import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
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
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CategoryOfLawValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
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
@SessionAttributes(APPLICATION_FORM_DATA)
public class CategoryOfLawController {

  private final CategoryOfLawValidator categoryOfLawValidator;

  private final ProviderService providerService;

  private final LookupService lookupService;

  /**
   * Handles the GET request for category of law selection page.
   *
   * @param exceptionalFunding A flag indicating exceptional funding.
   * @param applicationFormData The application details from session.
   * @param userDetails The user details from session.
   * @param bindingResult The result of data binding/validation.
   * @param model The model for the view.
   * @return The view name for the category of law selection page.
   */
  @GetMapping("/application/category-of-law")
  public String categoryOfLaw(
          @RequestParam(value = "exceptional_funding", defaultValue = "false")
                boolean exceptionalFunding,
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          BindingResult bindingResult,
          Model model) {

    applicationFormData.setExceptionalFunding(exceptionalFunding);

    initialiseCategoriesOfLaw(applicationFormData, userDetails, model, bindingResult);

    return "application/select-category-of-law";
  }


  /**
   * Handles the POST request for category of law selection form submission.
   *
   * @param applicationFormData The application details from session.
   * @param userDetails The user details from session.
   * @param bindingResult The result of data binding/validation.
   * @param model The model for the view.
   * @return A redirect or view name based on validation result and exceptional funding.
   */
  @PostMapping("/application/category-of-law")
  public String categoryOfLaw(
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
          @SessionAttribute(USER_DETAILS) UserDetail userDetails,
          BindingResult bindingResult,
          Model model) {
    categoryOfLawValidator.validate(applicationFormData, bindingResult);

    String viewName = "redirect:/application/application-type";
    if (bindingResult.hasErrors()) {
      initialiseCategoriesOfLaw(applicationFormData, userDetails, model, bindingResult);
      viewName = "application/select-category-of-law";
    } else if (applicationFormData.isExceptionalFunding()) {
      // Exception Funding has been selected, so initialise the ApplicationType to ECF
      // and bypass the ApplicationType screen.
      applicationFormData.setApplicationTypeCategory(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
      viewName = "redirect:/application/client/search";
    }

    return viewName;
  }

  /**
   * Initializes the categories of law for the view.
   *
   * @param applicationFormData The application details.
   * @param user The user details.
   * @param model The model for the view.
   * @param bindingResult The result of data binding/validation.
   */
  private void initialiseCategoriesOfLaw(
      ApplicationFormData applicationFormData,
      UserDetail user,
      Model model,
      BindingResult bindingResult) {

    List<CommonLookupValueDetail> categoriesOfLaw =
        Optional.ofNullable(lookupService.getCategoriesOfLaw().block())
            .orElse(new CommonLookupDetail())
            .getContent();

    if (!applicationFormData.isExceptionalFunding()) {
      List<String> categoryOfLawCodes = providerService.getCategoryOfLawCodes(
              user.getProvider().getId(),
              applicationFormData.getOfficeId(),
              user.getLoginId(),
              user.getUserType(),
              Boolean.TRUE);

      categoriesOfLaw.retainAll(categoriesOfLaw
          .stream().filter(category -> categoryOfLawCodes.contains(category.getCode()))
          .collect(Collectors.toList()));
    }

    model.addAttribute("categoriesOfLaw", categoriesOfLaw);

    if (categoriesOfLaw.isEmpty()) {
      bindingResult.rejectValue("categoryOfLawId", "no.categoriesOfLaw",
          "Warning: The Office selected is not contracted in any Category of Law, "
              + "it is therefore only possible to make applications for 'Exceptional Funding' "
              + "under this Office.");
    }
  }
}
