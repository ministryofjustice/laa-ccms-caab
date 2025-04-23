package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.OfficeValidator;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller to manage office-related functionalities in the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_FORM_DATA)
public class OfficeController {

  private final OfficeValidator officeValidator;

  /**
   * Creates a new instance of {@link uk.gov.laa.ccms.caab.bean.ApplicationFormData}.
   *
   * @return A new instance of {@link uk.gov.laa.ccms.caab.bean.ApplicationFormData}.
   */
  @ModelAttribute(APPLICATION_FORM_DATA)
  public ApplicationFormData getApplicationDetails() {
    return new ApplicationFormData();
  }

  /**
   * Displays the office selection page.
   *
   * @param user Current user details.
   * @param model Model to pass attributes to the view.
   * @return Path to the view.
   */
  @GetMapping("/application/office")
  public String selectOffice(
          @ModelAttribute(USER_DETAILS) UserDetail user,
          Model model) {
    model.addAttribute(APPLICATION_FORM_DATA, getApplicationDetails());
    model.addAttribute("offices", user.getProvider().getOffices());
    return "application/select-office";
  }

  /**
   * Handles the selection of an office.
   *
   * @param user Current user details.
   * @param applicationFormData Application details form data.
   * @param bindingResult Validation result.
   * @param model Model to pass attributes to the view.
   * @return Redirect path or current page based on validation.
   */
  @PostMapping("/application/office")
  public String selectOffice(
          @ModelAttribute(USER_DETAILS) UserDetail user,
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
          BindingResult bindingResult,
          Model model) {
    officeValidator.validate(applicationFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("offices", user.getProvider().getOffices());
      return "application/select-office";
    }

    return "redirect:/application/category-of-law";
  }
}
