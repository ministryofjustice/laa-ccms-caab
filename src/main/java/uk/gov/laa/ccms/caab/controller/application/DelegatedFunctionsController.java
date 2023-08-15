package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;

/**
 * Controller responsible for handling the application's delegated functions operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class DelegatedFunctionsController {

  private final ApplicationDetailsValidator applicationValidator;

  /**
   * Displays the delegated functions selection page.
   *
   * @param applicationDetails The details of the current application.
   * @return The path to the delegated functions selection view.
   */
  @GetMapping("/application/delegated-functions")
  public String delegatedFunction(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails) {
    log.info("GET /application/delegated-functions: {}", applicationDetails);

    return "/application/select-delegated-functions";
  }

  /**
   * Processes the user's delegated functions selection and redirects accordingly.
   *
   * @param applicationDetails The details of the current application.
   * @param bindingResult Validation result for the delegated functions form.
   * @return The path to the next step in the application process or the current page based on
   *         validation.
   */
  @PostMapping("/application/delegated-functions")
  public String delegatedFunction(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          BindingResult bindingResult) {
    log.info("POST /application/delegated-functions: {}", applicationDetails);
    applicationValidator.validateDelegatedFunction(applicationDetails, bindingResult);

    if (!applicationDetails.isDelegatedFunctions()) {
      applicationDetails.setDelegatedFunctionUsedDay(null);
      applicationDetails.setDelegatedFunctionUsedMonth(null);
      applicationDetails.setDelegatedFunctionUsedYear(null);
    }

    if (bindingResult.hasErrors()) {
      return "/application/select-delegated-functions";
    }

    return "redirect:/application/client/search";
  }

}
