package uk.gov.laa.ccms.caab.controller.billing;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.DECLARATION_BILL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_NOT_STARTED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntitiesForEntityType;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getMostRecentAssessmentDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.declaration.PoaDeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.PoaSummaryPdfService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to case billing. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class BillingController {

  private final BillingService billingService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  private final PoaDeclarationSubmissionValidator poaDeclarationValidator;
  private final PoaSummaryPdfService poaSummaryPdfService;

  private static final String CASE_STATEMENT_OF_ACCOUNT_URL = "redirect:/case/billing";
  private static final String OPA_BILL_TYPE_ATTRIBUTE = "BILL_TYPE";

  /**
   * Displays the case statement of account (billing) screen. The available billing actions are
   * shown based on the functions the case carries, and the statement of account figures and
   * bills/POA invoices are retrieved from EBS, mirroring the legacy PUI behaviour. The bills/POA
   * table can be long, so it is paginated in line with the other PUI tables.
   *
   * @param ebsCase The case details from EBS.
   * @param user The logged-in user.
   * @param page The requested bills/POA page (zero-based).
   * @param size The number of bills/POA rows per page.
   * @param request The current request, used to build the pagination links.
   * @param model The model used to pass data to the view.
   * @return The case statement of account view.
   */
  @GetMapping("/case/billing")
  public String caseStatementOfAccount(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      final HttpServletRequest request,
      final Model model) {

    final Set<String> availableFunctions = availableFunctions(ebsCase);

    final StatementOfAccountDisplay statementOfAccount =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);

    final List<BillPoaRow> billsAndPoa =
        statementOfAccount.getBillsAndPoa() == null
            ? List.of()
            : statementOfAccount.getBillsAndPoa();

    model.addAttribute("caseReferenceNumber", ebsCase.getCaseReferenceNumber());
    model.addAttribute("statementOfAccount", statementOfAccount);
    model.addAttribute(
        "billsAndPoaPage",
        PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), billsAndPoa));
    model.addAttribute("currentUrl", request.getRequestURL().toString());
    // Return the user to the bills/POA table (below the statement) after paging, rather than the
    // top of the page. The shared pagination fragment appends this as the link's anchor.
    model.addAttribute("paginationAnchor", "bills-and-poa");
    model.addAttribute(
        "showEnterUndertaking", availableFunctions.contains(FunctionConstants.ENTER_UNDERTAKING));
    // A create action is offered only when the case allows it and no draft of that type already
    // exists, since the create screen edits the existing draft rather than adding a second one.
    model.addAttribute(
        "showCreateBill",
        availableFunctions.contains(FunctionConstants.ADD_UPDATE_BILL)
            && !statementOfAccount.isDraftBillExists());
    model.addAttribute(
        "showCreatePoa",
        availableFunctions.contains(FunctionConstants.ADD_UPDATE_POA)
            && !statementOfAccount.isDraftPoaExists());
    // The draft POA's edit and delete links are only offered to a user allowed to maintain a POA,
    // mirroring the legacy PUI, which hides the actions when the function is not on the case.
    model.addAttribute("canMaintainPoa", canMaintainPoa(ebsCase));

    return "application/billing/case-statement-of-account";
  }

  /**
   * Displays the "Create a POA - POA details" screen, the entry point to the POA billing journey.
   *
   * <p>This ports the legacy PUI {@code CCMS_POA01}: entering the screen creates the draft payment
   * on account if the provider does not already have one ({@code AddPaymentOnAccount}), and the
   * screen then shows the status of the POA assessment ({@code PreparePOADetails}). Selecting "POA
   * details" starts the OPA interview; once it is complete a "POA summary" link is offered, exactly
   * as the legacy screen did.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param model the model used to pass data to the view.
   * @return the POA details view.
   */
  @GetMapping("/case/billing/poa")
  public String createPoa(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    if (!canMaintainPoa(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    billingService.createDraftPaymentOnAccountIfAbsent(
        ebsCase.getCaseReferenceNumber(), providerId(user), user);

    final AssessmentDetail poaAssessment = getLatestPoaAssessment(ebsCase, user);
    final boolean assessmentComplete = isComplete(poaAssessment);

    model.addAttribute("assessmentStatus", displayStatus(poaAssessment));
    model.addAttribute("assessmentComplete", assessmentComplete);
    // The legacy PUI only reveals the "POA summary" link, and the Action column holding it, once
    // the assessment is complete (PreparePOADetails' viewPOASummary flag).
    model.addAttribute("viewPoaSummary", assessmentComplete);

    return "application/billing/poa-details";
  }

  /**
   * Displays the "Delete payment on account" confirmation screen.
   *
   * @param ebsCase the case details from EBS.
   * @return the delete POA confirmation view.
   */
  @GetMapping("/case/billing/poa/remove")
  public String removePoaConfirmation(@SessionAttribute(CASE) final ApplicationDetail ebsCase) {
    if (!canMaintainPoa(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    return "application/billing/poa-remove";
  }

  /**
   * Deletes the provider's draft payment on account and the POA assessment data that went with it,
   * then returns to the case statement of account.
   *
   * <p>This ports the legacy PUI {@code RemovePaymentOfAccount}, which deletes the POA held for the
   * case and provider and removes the POA OPA sessions, including the pre-population session, so a
   * later POA starts from scratch rather than resuming the deleted one.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @return a redirect to the case statement of account.
   */
  @PostMapping("/case/billing/poa/remove")
  public String removePoa(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    if (!canMaintainPoa(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    billingService.deleteDraftPaymentsOnAccount(
        ebsCase.getCaseReferenceNumber(), providerId(user), user);

    assessmentService
        .deleteAssessments(
            user,
            List.of(
                AssessmentRulebase.POA.getName(), AssessmentRulebase.POA.getPrePopAssessmentName()),
            ebsCase.getCaseReferenceNumber(),
            null)
        .block();

    return CASE_STATEMENT_OF_ACCOUNT_URL;
  }

  /**
   * Displays the POA declaration screen, the step before the POA is submitted to EBS.
   *
   * <p>This ports the legacy PUI {@code RetrieveDeclarationText}: the declaration statements the
   * provider must acknowledge are looked up for the bill declaration type, qualified by the POA
   * assessment's bill type, and the user must select them to proceed to submit.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param model the model used to pass data to the view.
   * @return the POA declaration view, or a redirect when the POA cannot be submitted.
   */
  @GetMapping("/case/billing/poa/declaration")
  public String poaDeclaration(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final Model model) {

    final AssessmentDetail poaAssessment = getLatestPoaAssessment(ebsCase, user);
    // A POA can only be submitted when the case allows it and the assessment is complete, the same
    // two conditions the details screen uses to reveal Submit. Guard the declaration too, so it
    // cannot be reached directly for a POA that is not ready.
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    return poaDeclarationDetails(model, summarySubmissionFormData, poaAssessment);
  }

  /**
   * Submits the provider's draft payment on account to EBS once the declaration is acknowledged,
   * and shows the submission confirmation.
   *
   * <p>This ports the legacy PUI POA submit chain ({@code PerformFinalValForPoa} to {@code
   * PerformSubmission}): the declaration must be acknowledged, the draft POA and its completed
   * assessment are sent to EBS, and the POA OPA sessions (including the pre-population) are then
   * removed, exactly as the legacy post-submission cleanup does, so a later POA starts afresh.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param bindingResult the validation result for the declaration.
   * @param model the model used to pass data to the view.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return a redirect to the confirmation, or the declaration view when validation fails.
   */
  @PostMapping("/case/billing/poa/declaration")
  public String poaDeclarationPost(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    final AssessmentDetail poaAssessment = getLatestPoaAssessment(ebsCase, user);
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    poaDeclarationValidator.validate(summarySubmissionFormData, bindingResult);
    if (bindingResult.hasErrors()) {
      return poaDeclarationDetails(model, summarySubmissionFormData, poaAssessment);
    }

    final String transactionId =
        billingService.submitPaymentOnAccount(
            ebsCase.getCaseReferenceNumber(), providerId(user), poaAssessment, user);

    assessmentService
        .deleteAssessments(
            user,
            List.of(
                AssessmentRulebase.POA.getName(), AssessmentRulebase.POA.getPrePopAssessmentName()),
            ebsCase.getCaseReferenceNumber(),
            null)
        .block();

    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);

    return "redirect:/case/billing/poa/confirmation";
  }

  /**
   * Displays the POA submission confirmation, showing the reference EBS returned for the
   * submission.
   *
   * @param transactionId the submission reference carried from the submit step.
   * @param model the model used to pass data to the view.
   * @return the POA confirmation view, or a redirect when there is no submission to confirm.
   */
  @GetMapping("/case/billing/poa/confirmation")
  public String poaConfirmation(
      @SessionAttribute(name = SUBMISSION_TRANSACTION_ID, required = false)
          final String transactionId,
      final HttpSession session,
      final Model model) {

    if (transactionId == null) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    session.removeAttribute(SUBMISSION_TRANSACTION_ID);
    model.addAttribute("transactionId", transactionId);
    return "application/billing/poa-confirmation";
  }

  /**
   * Streams the POA summary report as a PDF, generated from the completed POA assessment. This
   * ports the legacy PUI {@code GetPoaSummary}: the report shows the claim lines the OPA interview
   * captured, and is only reachable once the assessment is complete. It is generated on demand and
   * served inline (opened in a new tab by the summary link) rather than stored, as the legacy PUI
   * did, since it is a stateless view of the current assessment.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @return the PDF response, or a redirect when the POA summary cannot be shown.
   */
  @GetMapping("/case/billing/poa/summary")
  public ResponseEntity<byte[]> poaSummary(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    final AssessmentDetail poaAssessment = getLatestPoaAssessment(ebsCase, user);
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/case/billing")).build();
    }

    final StatementOfAccountDisplay statement =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);
    final BigDecimal allocatedCostLimit = billingService.getAllocatedCostLimit(statement, ebsCase);

    final byte[] pdf =
        poaSummaryPdfService.generatePoaSummary(
            ebsCase, providerName(user), poaAssessment, statement, allocatedCostLimit);

    final String contentDisposition =
        ContentDisposition.inline()
            .filename("poa-summary.pdf", StandardCharsets.UTF_8)
            .build()
            .toString();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(pdf);
  }

  private String poaDeclarationDetails(
      final Model model,
      final SummarySubmissionFormData summarySubmissionFormData,
      final AssessmentDetail poaAssessment) {

    final DeclarationLookupDetail declarations =
        lookupService.getDeclarations(DECLARATION_BILL, poaBillType(poaAssessment)).block();
    final List<DynamicCheckbox> declarationOptions =
        submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(declarations);

    if (summarySubmissionFormData.getDeclarationOptions() == null
        || summarySubmissionFormData.getDeclarationOptions().isEmpty()) {
      summarySubmissionFormData.setDeclarationOptions(declarationOptions);
    }
    model.addAttribute("summarySubmissionFormData", summarySubmissionFormData);
    return "application/billing/poa-declaration";
  }

  /**
   * The POA bill type qualifies the declaration lookup. It is produced by the assessment and held
   * on the global entity, mirroring the legacy PUI, which reads the {@code BILL_TYPE} attribute
   * from the POA assessment session to key the declaration retrieval.
   */
  private String poaBillType(final AssessmentDetail poaAssessment) {
    return getAssessmentEntitiesForEntityType(poaAssessment, AssessmentEntityType.GLOBAL).stream()
        .map(entity -> getAssessmentAttribute(entity, OPA_BILL_TYPE_ATTRIBUTE))
        .filter(attribute -> attribute != null && attribute.getValue() != null)
        .map(AssessmentAttributeDetail::getValue)
        .findFirst()
        .orElse(null);
  }

  private Set<String> availableFunctions(final ApplicationDetail ebsCase) {
    return ebsCase.getAvailableFunctions() == null
        ? Collections.emptySet()
        : Set.copyOf(ebsCase.getAvailableFunctions());
  }

  private boolean canMaintainPoa(final ApplicationDetail ebsCase) {
    return availableFunctions(ebsCase).contains(FunctionConstants.ADD_UPDATE_POA);
  }

  private String providerId(final UserDetail user) {
    return String.valueOf(user.getProvider().getId());
  }

  private String providerName(final UserDetail user) {
    return user.getProvider() == null ? null : user.getProvider().getName();
  }

  private AssessmentDetail getLatestPoaAssessment(
      final ApplicationDetail ebsCase, final UserDetail user) {
    final AssessmentDetails assessmentDetails =
        assessmentService
            .getAssessments(
                List.of(AssessmentRulebase.POA.getName()),
                providerId(user),
                ebsCase.getCaseReferenceNumber())
            .block();

    if (assessmentDetails == null || assessmentDetails.getContent() == null) {
      return null;
    }

    return getMostRecentAssessmentDetail(assessmentDetails.getContent());
  }

  private boolean isComplete(final AssessmentDetail poaAssessment) {
    return poaAssessment != null
        && AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(poaAssessment.getStatus());
  }

  private String displayStatus(final AssessmentDetail poaAssessment) {
    if (poaAssessment == null || poaAssessment.getStatus() == null) {
      return SECTION_STATUS_NOT_STARTED;
    }

    return AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(poaAssessment.getStatus())
        ? SECTION_STATUS_COMPLETE
        : poaAssessment.getStatus();
  }
}
