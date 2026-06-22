package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AMEND_CLIENT_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationSectionValidator;
import uk.gov.laa.ccms.caab.constants.AmendClientOrigin;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;
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
  private final ApplicationSectionValidator applicationSectionValidator;
  private final EvidenceService evidenceService;
  private final EvidenceMapper evidenceMapper;

  /**
   * Initiates the amendment creation and submission process for a specific case. This method
   * processes the provided session attributes, creates an amendment, and redirects to the summary
   * page upon successful completion.
   *
   * @param detail Session attribute containing application details, including the case reference
   *     number.
   * @param userDetails Session attribute containing user details.
   * @param applicationFormData Session attribute containing application form data used for the
   *     amendment.
   * @param httpSession The current HTTP session to manage and store session attributes.
   * @return A string representing the redirect URL to the amendments summary page.
   */
  @GetMapping("/amendments/create")
  public String startAmendment(
      @SessionAttribute(CASE) final ApplicationDetail detail,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      @SessionAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      HttpSession httpSession) {
    amendmentService.createAndSubmitAmendmentForCase(applicationFormData, detail, userDetails);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference(detail.getCaseReferenceNumber());
    BaseApplicationDetail tdsApplication =
        applicationService
            .getTdsApplications(caseSearchCriteria, userDetails, 0, 1)
            .getContent()
            .stream()
            .findFirst()
            .orElse(null);

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
    final ApplicationDetail activeCaseDetail = (ApplicationDetail) httpSession.getAttribute(CASE);
    if (activeCaseDetail != null) {
      amendment.setAvailableFunctions(activeCaseDetail.getAvailableFunctions());
    }
    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(amendmentService.getAmendmentSections(amendment, user))
            .orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve application summary"));

    final EvidenceDocumentDetails evidenceUploaded =
        evidenceService
            .getEvidenceDocumentsForCase(activeCase.getCaseReferenceNumber(), CcmsModule.AMENDMENT)
            .block();

    final List<BaseEvidenceDocumentDetail> uploadedDocuments =
        evidenceUploaded != null && evidenceUploaded.getContent() != null
            ? evidenceUploaded.getContent()
            : Collections.emptyList();

    // Populate the required evidence list in the session so the document upload screen can be
    // reached directly from the amend case summary (without first visiting the evidence section
    // page) and still display which evidence each uploaded file can cover.
    final List<EvidenceDocumentTypeLookupValueDetail> evidenceRequiredLookups =
        evidenceService
            .getDocumentsRequired(
                String.valueOf(amendment.getId()),
                activeCase.getCaseReferenceNumber(),
                activeCase.getProviderId())
            .blockOptional()
            .orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve required evidence data"));

    final List<EvidenceRequired> evidenceRequired =
        evidenceMapper.toEvidenceRequiredList(evidenceRequiredLookups, uploadedDocuments);

    activeCase.setApplicationId(amendment.getId());

    httpSession.setAttribute(AMEND_CLIENT_ORIGIN, AmendClientOrigin.AMEND_CASE);
    httpSession.setAttribute(APPLICATION, amendment);
    httpSession.setAttribute(APPLICATION_ID, amendment.getId());
    httpSession.setAttribute(ACTIVE_CASE, activeCase);
    httpSession.setAttribute(APPLICATION_COSTS, amendment.getCosts());
    httpSession.setAttribute(EVIDENCE_REQUIRED, evidenceRequired);

    model.addAttribute(APPLICATION_ID, amendment.getId());

    model.addAttribute("summary", applicationSectionDisplay);
    model.addAttribute("evidenceUploaded", uploadedDocuments);

    return "application/amendment-summary";
  }

  @PostMapping("/amendments/summary")
  public String completeAmendment() {
    return "redirect:/amendments/validate";
  }

  @GetMapping("/amendments/submitConfirmed")
  public String submitAmendmentConfirmed(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      HttpSession session) {
    ApplicationDetail amendment = applicationService.getApplication(applicationId).block();

    final String response = amendmentService.submitAmendment(amendment, user);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, response);
    session.removeAttribute(SUBMISSION_RESULT);

    return "redirect:/amendments/%s".formatted(SUBMISSION_SUBMIT_CASE);
  }
}
