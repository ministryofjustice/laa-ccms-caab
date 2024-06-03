package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.opa.util.SecurityUtils;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Controller handling assessment requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

  private final AssessmentService assessmentService;
  private final ApplicationService applicationService;
  private final ClientService clientService;
  private final SecurityUtils contextSecurityUtil;

  /**
   * Displays the page to confirm the removal of an assessment.
   *
   * @param assessment the assessment to remove.
   * @param model the model to populate with data for the view.
   * @return the name of the view to render for removing a prior authority.
   */
  @GetMapping("/assessments/{assessment}/remove")
  public String assessmentRemove(
      @PathVariable("assessment") final String assessment,
      final Model model) {

    model.addAttribute("assessment", assessment);

    return "application/assessments/assessment-remove";
  }

  /**
   * Handles the removal of an assessment.
   *
   * @param assessment the assessment to remove.
   * @param user the user making the request.
   * @param activeCase the active case for which the assessment is being removed.
   * @return the name of the view to render after the assessment has been removed.
   */
  @PostMapping("/assessments/{assessment}/remove")
  public String assessmentRemovePost(
      @PathVariable("assessment") final String assessment,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase) {

    final List<String> assessmentCategories =
        AssessmentName.findAssessmentNamesByCategory(assessment);

    if (assessmentCategories.isEmpty()) {
      throw new CaabApplicationException("Invalid assessment type");
    } else {
      assessmentService.deleteAssessments(
          user,
          assessmentCategories,
          activeCase.getCaseReferenceNumber(),
          null)
          .block();

    }

    return "redirect:/application/summary";
  }


  /**
   * Displays the assessment page.
   *
   * @param assessment the assessment to remove.
   * @param invokedFrom the page from which the assessment is being invoked.
   * @param user the user making the request.
   * @param session the session in which the request is being made.
   * @param request the http servlet request being made.
   * @return the view that displays the OPA assessment.
   */
  @GetMapping("/assessments")
  public String assessmentGet(
      @RequestParam(value = "assessment") final String assessment,
      @RequestParam("invoked-from") final String invokedFrom,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final HttpServletRequest request) {

    ApplicationDetail application = null;

    //get the assessment or case data, check if application id is in the session if is then we get
    // the application, otherwise it's a case
    if (session.getAttribute(APPLICATION_ID) != null) {
      final String applicationId = String.valueOf(session.getAttribute(APPLICATION_ID));
      application =
          Optional.ofNullable(applicationService.getApplication(applicationId).block())
              .orElseThrow(() -> new CaabApplicationException(
                  "Failed to retrieve application"));
    } else {
      //todo - get case details (not part of application process)
      // map case into application object
    }

    //get rulebase from the assessment passed as the parameter
    final AssessmentRulebase assessmentRulebase = AssessmentRulebase.findByType(assessment);

    //amendment stuff
    //todo - later implementation

    //delete opponents and proceedings that have been removed from the application
    // if not a financial assessment type (aka means or merits)
    if (!assessmentRulebase.isFinancialAssessment()) {
      final AssessmentDetails assessmentDetails = assessmentService.getAssessments(
          List.of(assessmentRulebase.getPrePopAssessmentName()),
          String.valueOf(user.getProvider().getId()),
          application.getCaseReferenceNumber(),
          null).block();

      for (final AssessmentDetail prepopAssessment : assessmentDetails.getContent()) {

        if (prepopAssessment.getName()
            .equalsIgnoreCase(assessmentRulebase.getPrePopAssessmentName())) {

          //is deletion of checkpoint required
          if (assessmentService.isAssessmentCheckpointToBeDeleted(application, prepopAssessment)) {
            if (prepopAssessment.getCheckpoint() != null) {
              prepopAssessment.setCheckpoint(null);
            }
            //cleanup data
            assessmentService.cleanupData(prepopAssessment, application);
            //save the assessment
            assessmentService.saveAssessment(user, prepopAssessment).block();
            break;
          }
        }
      }
    }

    //get full client details from soa
    // If user has not modified client details,
    // it is necessary to do an EBS call to get a fully populated client
    //lets leave this for now.
    final ClientDetail client = clientService.getClient(
        application.getClient().getReference(),
        user.getLoginId(),
        user.getUserType()).block();

    if (assessment.equalsIgnoreCase("means") || assessment.equalsIgnoreCase("merits")) {
      final String assessmentType = assessment.toUpperCase();

      //Create temporary context token
      final String contextToken = contextSecurityUtil.createHubContext(
          application.getCaseReferenceNumber(), assessmentRulebase.getId(), user.getUsername(),
          user.getProvider().getId().longValue(), request.getSession().getId(), invokedFrom,
          "1234567890");

      //start opa assessment
      assessmentService.startAssessment(
          application,
          assessmentRulebase,
          application.getCaseReferenceNumber(),
          client,
          user);

    } else if (assessment.equalsIgnoreCase("billing")) {
      final String assessmentType = assessment.toUpperCase();
      //todo - later implementation

    } else if (assessment.equalsIgnoreCase("poa")) {
      final String assessmentType = assessment.toUpperCase();
      //todo - later implementation
    }

    //temporary redirect - to be removed in later story
    return "redirect:/application/summary";
  }



}
