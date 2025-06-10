package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.AMENDMENT;
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
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@Controller
@SessionAttributes({APPLICATION, APPLICATION_FORM_DATA})
@RequiredArgsConstructor
public class AmendCaseController {

  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;

  @GetMapping("/amendments/create")
  public String startAmendment(@SessionAttribute(CASE) final ApplicationDetail detail,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      @SessionAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      HttpSession httpSession) {
    amendmentService.createAndSubmitAmendmentForCase(applicationFormData,
        detail.getCaseReferenceNumber(),
        userDetails);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference(detail.getCaseReferenceNumber());
    BaseApplicationDetail tdsApplication =
        applicationService.getTdsApplications(caseSearchCriteria, userDetails, 0, 1)
            .getContent().stream().findFirst().orElse(null);

    httpSession.setAttribute(APPLICATION, tdsApplication);

    return "redirect:/amendments/summary";
  }

  @GetMapping("/amendments/summary")
  public String amendCaseSummary(
      @SessionAttribute(APPLICATION) final BaseApplicationDetail tdsApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession httpSession,
      Model model) {

    final ApplicationDetail amendment =
        applicationService.getApplication(String.valueOf(tdsApplication.getId())).block();
    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(amendmentService.getAmendmentSections(amendment, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    httpSession.setAttribute(AMENDMENT, amendment);
    model.addAttribute("summary", applicationSectionDisplay);

    return "application/amendment-summary";
  }

}
