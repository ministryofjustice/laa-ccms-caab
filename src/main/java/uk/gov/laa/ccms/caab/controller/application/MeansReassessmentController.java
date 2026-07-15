package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.DECLARATION_APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_QUICK_EDIT_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getMostRecentAssessmentDetail;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.declaration.DeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for the standalone means reassessment quick amendment journey. */
@Controller
@RequiredArgsConstructor
public class MeansReassessmentController {

  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  private final DeclarationSubmissionValidator declarationSubmissionValidator;

  @GetMapping("/means-reassessment")
  public String startMeansReassessment(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session) {

    BaseApplicationDetail tdsApplication = getOrCreateMeansReassessment(ebsCase, user);
    ApplicationDetail amendment =
        Optional.ofNullable(
                applicationService.getApplication(String.valueOf(tdsApplication.getId())).block())
            .orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve means reassessment"));

    setReassessmentSession(session, amendment, tdsApplication);

    return "redirect:/means-reassessment/summary";
  }

  @GetMapping("/means-reassessment/summary")
  public String meansReassessmentSummary(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    AssessmentDetail meansAssessment = getLatestMeansAssessment(activeCase, user);
    boolean assessmentComplete = isComplete(meansAssessment);
    boolean hasSubmitPermission = hasSubmitPermission(ebsCase);

    model.addAttribute("assessmentStatus", displayStatus(meansAssessment));
    model.addAttribute("assessmentComplete", assessmentComplete);
    model.addAttribute("canSubmit", assessmentComplete && hasSubmitPermission);
    // Mirror old PUI (PUI_ContentID_1069): a user who has completed the means assessment but lacks
    // the MNSB submit function is told why they cannot submit.
    model.addAttribute("noSubmitPermission", assessmentComplete && !hasSubmitPermission);

    return "application/means-reassessment/summary";
  }

  @GetMapping("/means-reassessment/delete")
  public String deleteMeansReassessmentConfirmation() {
    return "application/means-reassessment/delete";
  }

  @PostMapping("/means-reassessment/delete")
  public String deleteMeansReassessment(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session) {

    // Delete only the means assessment data, matching old PUI's DeleteAssessmentController (which
    // removes the means* OPA sessions and never touches the application). The shared draft is left
    // alone; the case overview ignores a means-reassessment draft, so it does not surface as an
    // open amendment.
    assessmentService
        .deleteAssessments(
            user,
            List.of(
                AssessmentRulebase.MEANS.getName(),
                AssessmentRulebase.MEANS.getPrePopAssessmentName()),
            activeCase.getCaseReferenceNumber(),
            null)
        .block();

    clearReassessmentSession(session);

    return "redirect:/case/overview";
  }

  @GetMapping("/means-reassessment/declaration")
  public String meansReassessmentDeclaration(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final Model model) {

    if (!canSubmit(ebsCase, activeCase, user)) {
      return "redirect:/means-reassessment/summary";
    }

    return declarationDetails(model, summarySubmissionFormData);
  }

  @PostMapping("/means-reassessment/declaration")
  public String submitMeansReassessment(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(APPLICATION) final ApplicationDetail amendment,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    if (!canSubmit(ebsCase, activeCase, user)) {
      return "redirect:/means-reassessment/summary";
    }

    declarationSubmissionValidator.validate(summarySubmissionFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      return declarationDetails(model, summarySubmissionFormData);
    }

    String transactionId =
        amendmentService.submitMeansReassessment(
            user, amendment, getLatestMeansAssessment(activeCase, user));

    session.removeAttribute(SUBMISSION_RESULT);
    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);
    // The quick edit type is not persisted against the TDS draft, so the post-submission cleanup
    // takes it from the session to know that only the means was submitted.
    session.setAttribute(
        SUBMISSION_QUICK_EDIT_TYPE, QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT);

    return "redirect:/%s/%s"
        .formatted(CaseContext.AMENDMENTS.getPathValue(), SUBMISSION_SUBMIT_CASE);
  }

  private BaseApplicationDetail getOrCreateMeansReassessment(
      final ApplicationDetail ebsCase, final UserDetail user) {
    BaseApplicationDetail existingApplication = getTdsApplication(ebsCase, user);

    if (existingApplication != null) {
      return existingApplication;
    }

    amendmentService.createMeansReassessmentForCase(ebsCase, user);

    return Optional.ofNullable(getTdsApplication(ebsCase, user))
        .orElseThrow(() -> new CaabApplicationException("Failed to create means reassessment"));
  }

  private BaseApplicationDetail getTdsApplication(
      final ApplicationDetail ebsCase, final UserDetail user) {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference(ebsCase.getCaseReferenceNumber());

    ApplicationDetails applications =
        applicationService.getTdsApplications(caseSearchCriteria, user, 0, 1);

    if (applications == null || applications.getContent() == null) {
      return null;
    }

    return applications.getContent().stream().findFirst().orElse(null);
  }

  private void setReassessmentSession(
      final HttpSession session,
      final ApplicationDetail amendment,
      final BaseApplicationDetail tdsApplication) {
    ActiveCase activeCase =
        ActiveCase.builder()
            .applicationId(amendment.getId())
            .caseReferenceNumber(amendment.getCaseReferenceNumber())
            .providerId(amendment.getProviderDetails().getProvider().getId())
            .clientReferenceNumber(amendment.getClient().getReference())
            .providerCaseReferenceNumber(amendment.getProviderDetails().getProviderCaseReference())
            .build();

    session.setAttribute(APPLICATION_SUMMARY, tdsApplication);
    session.setAttribute(APPLICATION, amendment);
    session.setAttribute(APPLICATION_ID, String.valueOf(amendment.getId()));
    session.setAttribute(ACTIVE_CASE, activeCase);
  }

  private AssessmentDetail getLatestMeansAssessment(
      final ActiveCase activeCase, final UserDetail user) {
    AssessmentDetails assessmentDetails =
        assessmentService
            .getAssessments(
                List.of(AssessmentRulebase.MEANS.getName()),
                user.getProvider().getId().toString(),
                activeCase.getCaseReferenceNumber())
            .block();

    if (assessmentDetails == null || assessmentDetails.getContent() == null) {
      return null;
    }

    return getMostRecentAssessmentDetail(assessmentDetails.getContent());
  }

  private boolean canSubmit(
      final ApplicationDetail ebsCase, final ActiveCase activeCase, final UserDetail user) {
    return hasSubmitPermission(ebsCase) && isComplete(getLatestMeansAssessment(activeCase, user));
  }

  private boolean hasSubmitPermission(final ApplicationDetail ebsCase) {
    return ebsCase.getAvailableFunctions() != null
        && ebsCase.getAvailableFunctions().contains(FunctionConstants.SUBMIT_MEANS_REASSESSMENT);
  }

  private boolean isComplete(final AssessmentDetail meansAssessment) {
    return meansAssessment != null
        && AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(meansAssessment.getStatus());
  }

  private String displayStatus(final AssessmentDetail meansAssessment) {
    if (meansAssessment == null || meansAssessment.getStatus() == null) {
      return "Not started";
    }

    return AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(meansAssessment.getStatus())
        ? "Complete"
        : meansAssessment.getStatus();
  }

  private String declarationDetails(
      final Model model, final SummarySubmissionFormData summarySubmissionFormData) {
    final DeclarationLookupDetail declarations =
        lookupService.getDeclarations(DECLARATION_APPLICATION).block();
    final List<DynamicCheckbox> declarationOptions =
        submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(declarations);

    if (summarySubmissionFormData.getDeclarationOptions() == null
        || summarySubmissionFormData.getDeclarationOptions().isEmpty()) {
      summarySubmissionFormData.setDeclarationOptions(declarationOptions);
    }

    model.addAttribute("summarySubmissionFormData", summarySubmissionFormData);
    return "application/means-reassessment/declaration";
  }

  private void clearReassessmentSession(final HttpSession session) {
    session.removeAttribute(APPLICATION_SUMMARY);
    session.removeAttribute(APPLICATION_ID);
    session.removeAttribute(APPLICATION);
    session.removeAttribute(ACTIVE_CASE);
  }
}
