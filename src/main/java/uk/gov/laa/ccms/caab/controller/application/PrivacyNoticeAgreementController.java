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
 * Controller handling privacy notice agreement-related requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class PrivacyNoticeAgreementController {

  private final ApplicationDetailsValidator applicationValidator;

  /**
   * Displays the privacy notice agreement page.
   *
   * @param applicationDetails The application details.
   * @return The name of the view to render.
   */
  @GetMapping("/application/agreement")
  public String privacyNoticeAgreement(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails) {
    log.info("GET /application/agreement: {}", applicationDetails);
    return "application/privacy-notice-agreement";
  }

  /**
   * Handles the submission of the privacy notice agreement.
   *
   * @param applicationDetails The application details.
   * @param bindingResult Validation result.
   * @return Redirects to the appropriate page based on agreement acceptance.
   */
  @PostMapping("/application/agreement")
  public String privacyNoticeAgreement(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          BindingResult bindingResult) {
    log.info("POST /application/agreement: {}", applicationDetails);
    applicationValidator.validateAgreementAcceptance(applicationDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/privacy-notice-agreement";
    } else {
      if (applicationDetails.isApplicationCreated()) {
        //using an existing client
        return "redirect:/application/summary";
      } else {
        //registering a new client
        return "redirect:/application/client/basic-details";
      }
    }
  }

  /**
   * Displays a printable version of the privacy notice agreement.
   *
   * @return The name of the printable view.
   */
  @GetMapping("/application/agreement/print")
  public String privacyNoticeAgreementPrintable() {
    log.info("GET /application/agreement/printable");
    return "application/privacy-notice-agreement-printable";
  }

}


