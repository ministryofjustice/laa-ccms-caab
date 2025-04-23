package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.PrivacyNoticeAgreementValidator;

/**
 * Controller handling privacy notice agreement-related requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_FORM_DATA)
public class PrivacyNoticeAgreementController {

  private final PrivacyNoticeAgreementValidator applicationValidator;

  /**
   * Displays the privacy notice agreement page.
   *
   * @param applicationFormData The application details.
   * @return The name of the view to render.
   */
  @GetMapping("/application/agreement")
  public String privacyNoticeAgreement(
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData) {
    return "application/privacy-notice-agreement";
  }

  /**
   * Handles the submission of the privacy notice agreement.
   *
   * @param applicationFormData The application details.
   * @param bindingResult Validation result.
   * @return Redirects to the appropriate page based on agreement acceptance.
   */
  @PostMapping("/application/agreement")
  public String privacyNoticeAgreement(
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
          BindingResult bindingResult) {
    applicationValidator.validate(applicationFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/privacy-notice-agreement";
    } else {
      if (applicationFormData.isApplicationCreated()) {
        //using an existing client
        return "redirect:/application/sections";
      } else {
        //registering a new client
        return "redirect:/application/client/details/basic";
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
    return "application/privacy-notice-agreement-printable";
  }

}
