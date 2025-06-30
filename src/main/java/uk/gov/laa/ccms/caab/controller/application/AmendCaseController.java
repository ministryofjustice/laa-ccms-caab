package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
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
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller handling requests related to the amendment of cases. This controller includes
 * operations for initializing and summarizing case amendments.
 *
 * @author Jamie Briggs
 */
@Controller
@SessionAttributes({APPLICATION, APPLICATION_ID, APPLICATION_FORM_DATA})
@RequiredArgsConstructor
public class AmendCaseController {

  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;

  /**
   * Initiates the amendment creation and submission process for a specific case.
   * This method processes the provided session attributes, creates an amendment,
   * and redirects to the summary page upon successful completion.
   *
   * @param detail            Session attribute containing application details,
   *                          including the case reference number.
   * @param userDetails       Session attribute containing user details.
   * @param applicationFormData Session attribute containing application form data used
   *                            for the amendment.
   * @param httpSession       The current HTTP session to manage and store session attributes.
   *
   * @return A string representing the redirect URL to the amendments summary page.
   */
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

    httpSession.setAttribute(APPLICATION_SUMMARY, tdsApplication);

    return "redirect:/amendments/summary";
  }

  /**
   * Handles the request to display the summary for a case amendment. Retrieves application details
   * and amendment sections, setting them in the HTTP session and model for rendering on the view.
   *
   * @param tdsApplication the current application details stored in the session
   * @param user the details of the currently logged-in user stored in the session
   * @param model the model to which the amendment summary data is added
   * @param httpSession the HTTP session used to store amendment details
   * @return the name of the view to be rendered, which displays the amendment summary
   * @throws CaabApplicationException if the amendment details cannot be retrieved
   */
  @GetMapping("/amendments/summary")
  public String amendCaseSummary(
      @SessionAttribute(APPLICATION_SUMMARY) final BaseApplicationDetail tdsApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      Model model,
      HttpSession httpSession) {

    final ApplicationDetail amendment =
        applicationService.getApplication(String.valueOf(tdsApplication.getId())).block();
    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(amendmentService.getAmendmentSections(amendment, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    activeCase.setApplicationId(amendment.getId());

    httpSession.setAttribute(APPLICATION, amendment);
    httpSession.setAttribute(APPLICATION_ID, amendment.getId());
    httpSession.setAttribute(ACTIVE_CASE, activeCase);
    httpSession.setAttribute(APPLICATION_COSTS, amendment.getCosts());
    httpSession.setAttribute("Hello", "World");

    model.addAttribute("summary", applicationSectionDisplay);

    return "application/amendment-summary";
  }

}
