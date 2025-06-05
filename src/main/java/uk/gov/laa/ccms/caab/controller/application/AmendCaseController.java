package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@Controller
@RequiredArgsConstructor
public class AmendCaseController {

  private final ApplicationService applicationService;

  @GetMapping("/amendments/create")
  public String startAmendment(@SessionAttribute(CASE) final ApplicationDetail detail,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      @SessionAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      HttpSession httpSession) {
    // TODO: Check if application already exists
    ApplicationDetail amendmentApplicationDetail =
        applicationService.createAndSubmitAmendmentForCase(applicationFormData,
        detail.getCaseReferenceNumber(),
        userDetails);
    httpSession.setAttribute(APPLICATION, amendmentApplicationDetail);
    return "redirect:/amendments/summary";
  }

  @GetMapping("/amendments/continue")
  public String continueAmendment(
      @SessionAttribute(APPLICATION) final BaseApplicationDetail tdsApplication,
      final HttpSession httpSession) {
    ApplicationDetail amendmentApplicationDetail =
        applicationService.getApplication(String.valueOf(tdsApplication.getId())).block();
    httpSession.setAttribute(APPLICATION, amendmentApplicationDetail);
    return "redirect:/amendments/summary";
  }

  @GetMapping("/amendments/summary")
  public String amendCaseSummary(
      @SessionAttribute(APPLICATION) final ApplicationDetail amendment,
      Model model) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(amendment))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", applicationSectionDisplay);

    return "application/amendment-summary";
  }

}
