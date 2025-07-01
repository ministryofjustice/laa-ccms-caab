package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/**
 * Controller responsible for handling the initiation of a new application process. Manages
 * interactions related to starting a new application, including determining the appropriate routing
 * based on whether it's a new application, or an amendment to an existing case.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes(APPLICATION_FORM_DATA)
public class StartApplicationController {

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
   * Initiates the process for starting a new application or amendment based on the context, whilst
   * adding the application form data to the model for subsequent use.
   *
   * @param model The model used to store attributes for rendering views.
   * @param caseContext The context indicating whether the process is for a new application or an
   *     amendment. It must match one of the predefined context values.
   * @return A redirection string to the appropriate route based on the provided context.
   */
  @GetMapping("/{caseContext}/new")
  public String startNewApplication(
      Model model, @PathVariable("caseContext") final CaseContext caseContext) {
    log.info("Starting application");

    model.addAttribute(APPLICATION_FORM_DATA, getApplicationDetails());

    if (caseContext.isAmendment()) {
      return "redirect:/amendments/application-type";
    } else {
      return "redirect:/application/office";
    }
  }
}
