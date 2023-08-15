package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
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
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller to manage office-related functionalities in the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class OfficeController {

  private final ApplicationDetailsValidator applicationValidator;

  /**
   * Creates a new instance of {@link ApplicationDetails}.
   *
   * @return A new instance of {@link ApplicationDetails}.
   */
  @ModelAttribute(APPLICATION_DETAILS)
  public ApplicationDetails getApplicationDetails() {
    return new ApplicationDetails();
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
    model.addAttribute(APPLICATION_DETAILS, getApplicationDetails());
    model.addAttribute("offices", user.getProvider().getOffices());
    return "/application/select-office";
  }

  /**
   * Handles the selection of an office.
   *
   * @param user Current user details.
   * @param applicationDetails Application details form data.
   * @param bindingResult Validation result.
   * @param model Model to pass attributes to the view.
   * @return Redirect path or current page based on validation.
   */
  @PostMapping("/application/office")
  public String selectOffice(
          @ModelAttribute(USER_DETAILS) UserDetail user,
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          BindingResult bindingResult,
          Model model) {

    log.info("POST /application/office: {}", applicationDetails);
    applicationValidator.validateSelectOffice(applicationDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("offices", user.getProvider().getOffices());
      return "/application/select-office";
    }

    return "redirect:/application/category-of-law";
  }
}


