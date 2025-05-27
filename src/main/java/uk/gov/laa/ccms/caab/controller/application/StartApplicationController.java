package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;
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
import uk.gov.laa.ccms.caab.constants.ContextConstants;

@Controller
@RequiredArgsConstructor
@Slf4j
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

  @GetMapping("/{" + CONTEXT_NAME + "}/new")
  public String startNewApplication(Model model,
      @PathVariable(CONTEXT_NAME) final String caseContext) {
    log.info("Starting application");

    model.addAttribute(APPLICATION_FORM_DATA, getApplicationDetails());

    if (ContextConstants.AMENDMENTS.equals(caseContext)) {
      return "redirect:/amendments/application-type";
    } else {
      return "redirect:/application/office";
    }
  }
}
