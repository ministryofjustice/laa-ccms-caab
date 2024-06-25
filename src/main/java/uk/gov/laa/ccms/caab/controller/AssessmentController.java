package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${laa.ccms.oracle-web-determination-server.url}")
  private final String owdUrl;
  @Value("${laa.ccms.oracle-web-determination-server.resources.interview-styling}")
  private final String interviewStyling;
  @Value("${laa.ccms.oracle-web-determination-server.resources.font-styling}")
  private final String fontStyling;
  @Value("${laa.ccms.oracle-web-determination-server.resources.interview-javascript}")
  private final String interviewJavascript;

  private static final String RETURN_URL = "/civil/assessments/confirm?val=";
  private static final String CANCEL_LINK_TEXT = "Return to create application";
  private static final String CANCEL_LINK_URL = "/civil/application/summary";

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
   * @return the view that displays the OPA assessment.
   */
  @GetMapping("/assessments")
  public String assessmentGet(
      @RequestParam(value = "assessment") final String assessment,
      @RequestParam("invoked-from") final String invokedFrom,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    final ApplicationDetail application;

    //get the assessment or case data, check if application id is in the session if is then we get
    // the application, otherwise it's a case
    if (session.getAttribute(APPLICATION_ID) != null) {
      final String applicationId = String.valueOf(session.getAttribute(APPLICATION_ID));
      application =
          Optional.ofNullable(applicationService.getApplication(applicationId).block())
              .orElseThrow(() -> new CaabApplicationException(
                  "Failed to retrieve application"));
    } else {
      application = null;
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
          application.getCaseReferenceNumber()).block();

      if (assessmentDetails != null && assessmentDetails.getContent() != null) {
        assessmentDetails.getContent().stream().findFirst().ifPresent(prepopAssessment -> {
          //is deletion of checkpoint required
          if (assessmentService.isAssessmentCheckpointToBeDeleted(application, prepopAssessment)) {
            if (prepopAssessment.getCheckpoint() != null) {
              prepopAssessment.setCheckpoint(null);
            }
            //cleanup data
            assessmentService.cleanupData(prepopAssessment, application);
            //save the assessment
            assessmentService.saveAssessment(user, prepopAssessment).block();
          }
        });
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

    //if means or merits assessment
    if (!assessmentRulebase.isFinancialAssessment()) {

      //Required for the connector
      final String ezgovId = UUID.randomUUID().toString();

      //Create context token
      final String contextToken = contextSecurityUtil.createHubContext(
          application.getCaseReferenceNumber(), assessmentRulebase.getId(), user.getUsername(),
          user.getProvider().getId().longValue(), session.getId(), invokedFrom,
          ezgovId);

      //start opa assessment
      assessmentService.startAssessment(
          application,
          assessmentRulebase,
          client,
          user);

      final AssessmentDetails assessmentDetails = assessmentService.getAssessments(
          List.of(assessmentRulebase.getPrePopAssessmentName()),
          String.valueOf(user.getProvider().getId()),
          application.getCaseReferenceNumber()).block();

      final AssessmentDetail prepopAssessment = assessmentDetails
          .getContent()
          .stream()
          .findFirst()
          .get();

      populateOpaModel(contextToken, prepopAssessment, user, assessmentRulebase, model);

    } else if (assessment.equalsIgnoreCase("billing")) {
      //todo - later implementation

    } else if (assessment.equalsIgnoreCase("poa")) {
      //todo - later implementation
    }

    return "application/assessments/assessment-get";
  }

  /**
   * Populates the OPA model with assessment and user details.
   *
   * @param contextToken a unique token representing the session context
   * @param prepopAssessment the assessment details for prepopulation
   * @param user the user details
   * @param assessmentRulebase the rulebase for the assessment
   * @param model the model to populate
   */
  private void populateOpaModel(
      final String contextToken,
      final AssessmentDetail prepopAssessment,
      final UserDetail user,
      final AssessmentRulebase assessmentRulebase,
      final Model model) {

    final String submitReturnUrl = RETURN_URL.concat(contextToken);

    if (prepopAssessment.getCheckpoint() != null) {
      model.addAttribute("checkpoint", "RESUME");
    } else {
      model.addAttribute("checkpoint", "START");
    }

    model.addAttribute("cancelUrl", CANCEL_LINK_URL);
    model.addAttribute("owdUrl", owdUrl);
    model.addAttribute("frameTitle", "");
    model.addAttribute("returnLinkText", CANCEL_LINK_TEXT);
    model.addAttribute("deploymentName", assessmentRulebase.getDeploymentName());
    model.addAttribute("interviewsCSS", interviewStyling);
    model.addAttribute("fontsCSS", fontStyling);
    model.addAttribute("interviewsJS", interviewJavascript);
    model.addAttribute("params", contextToken);
    model.addAttribute("submitReturnUrl", submitReturnUrl);
    model.addAttribute("username", user.getUsername());
    model.addAttribute("resumeId", prepopAssessment.getId().toString());
    model.addAttribute("assessmentType", assessmentRulebase.getType());

  }


  /**
   * Confirms the assessment.
   *
   * @return the view that displays the confirmation of the assessment,
   *         listing the answers to the questions the user has provided.
   */
  @GetMapping("/assessments/confirm")
  public String assessmentConfirm() {

    //todo - CCLS-2241 assessment confirmation screens
    return "application/assessments/assessment-confirm";

  }

}
