package uk.gov.laa.ccms.caab.controller.billing;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.DECLARATION_BILL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_NOT_STARTED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.UNDERTAKING_MAXIMUM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.UNDERTAKING_MINIMUM;
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
import java.util.function.BiFunction;
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
import org.springframework.web.util.WebUtils;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.billing.BillingUndertakingValidator;
import uk.gov.laa.ccms.caab.bean.validators.declaration.PoaDeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.service.BillingSummaryPdfService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling requests related to case billing. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class BillingController {

  private static final String STATUTORY_CHARGE_MANUAL_URL =
      "https://assets.publishing.service.gov.uk/media/6a4756b1d200ca05e289e412/The_Statutory_Charge_Manual_July_2026.pdf";

  private final BillingService billingService;
  private final AmendmentService amendmentService;
  private final AssessmentService assessmentService;
  private final LookupService lookupService;
  private final SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  private final PoaDeclarationSubmissionValidator poaDeclarationValidator;
  private final BillingSummaryPdfService billingSummaryPdfService;
  private final BillingUndertakingValidator billingUndertakingValidator;

  private static final String CASE_STATEMENT_OF_ACCOUNT_URL = "redirect:/case/billing";
  private static final String OPA_BILL_TYPE_ATTRIBUTE = "BILL_TYPE";
  private static final String OPA_COURT_ASSESSED_BILL_ATTRIBUTE = "COURT_ASSESSED_BILL";
  private static final String BILL_DECLARATION_VIEW = "application/billing/bill-declaration";
  private static final String POA_DECLARATION_VIEW = "application/billing/poa-declaration";

  /** Session attribute holding the billing submission this session has already sent. */
  private static final String BILLING_SUBMISSION_SENT = "billingSubmissionSent";

  private static final String BILL_SUBMISSION = "bill";
  private static final String POA_SUBMISSION = "poa";

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
    // Likewise for the draft bill's edit and delete links.
    model.addAttribute("canMaintainBill", canMaintainBill(ebsCase));
    // A draft withholds the create action for its own type, and a draft bill also withholds the
    // copy action against a rejected bill, since copying creates a draft bill. Explain that rather
    // than leaving the actions silently absent - the legacy PUI carries a note to the same purpose
    // as static intro text on this screen.
    model.addAttribute(
        "draftInProgress",
        statementOfAccount.isDraftBillExists() || statementOfAccount.isDraftPoaExists());

    return "application/billing/case-statement-of-account";
  }

  /**
   * Displays the enter undertaking screen and clears any previously cached undertaking range values
   * from the session.
   *
   * @param model The model used to pass form data to the view.
   * @param session The current HTTP session.
   * @return The enter undertaking view.
   */
  @GetMapping("/case/billing/undertaking")
  public String enterUndertaking(final Model model, final HttpSession session) {
    session.removeAttribute(UNDERTAKING_MINIMUM);
    session.removeAttribute(UNDERTAKING_MAXIMUM);
    model.addAttribute("statutoryChargeManualUrl", STATUTORY_CHARGE_MANUAL_URL);
    model.addAttribute("undertakingFormData", new UndertakingFormData());
    return "application/billing/enter-undertaking";
  }

  /**
   * Validates and submits an undertaking amount as a quick amendment.
   *
   * <p>The valid undertaking range is derived from the current provider statement and cached in the
   * session for redisplay when validation fails.
   *
   * @param ebsCase The case details from EBS.
   * @param user The logged-in user.
   * @param undertakingFormData The undertaking form payload.
   * @param bindingResult Validation errors for the form.
   * @param model The model used to pass form data back to the view.
   * @param session The current HTTP session.
   * @return The enter undertaking view when validation fails; otherwise a redirect to submit the
   *     amendment.
   */
  @PostMapping("/case/billing/undertaking")
  public String submitUndertaking(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("undertakingFormData") final UndertakingFormData undertakingFormData,
      final BindingResult bindingResult,
      final Model model,
      HttpSession session) {

    BigDecimal undertakingMinimum = (BigDecimal) session.getAttribute(UNDERTAKING_MINIMUM);
    BigDecimal undertakingMaximum = (BigDecimal) session.getAttribute(UNDERTAKING_MAXIMUM);

    if (undertakingMinimum == null || undertakingMaximum == null) {
      final StatementOfAccountDetail statementOfAccount =
          billingService.getCurrentProviderStatement(
              ebsCase.getCaseReferenceNumber(), ebsCase, user);
      if (statementOfAccount != null) {
        if (statementOfAccount.getBills() != null) {
          undertakingMinimum = statementOfAccount.getBills().getTotalAmount();
        }
        if (statementOfAccount.getCostLimitation() != null) {
          undertakingMaximum = statementOfAccount.getCostLimitation().getRemainingAmount();
        }
      }

      undertakingMinimum = undertakingMinimum == null ? BigDecimal.ZERO : undertakingMinimum;
      undertakingMaximum = undertakingMaximum == null ? BigDecimal.ZERO : undertakingMaximum;
    }

    undertakingFormData.setUndertakingMinimumAmount(undertakingMinimum);
    undertakingFormData.setUndertakingMaximumAmount(undertakingMaximum);

    // validate
    billingUndertakingValidator.validate(undertakingFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      session.setAttribute(UNDERTAKING_MINIMUM, undertakingMinimum);
      session.setAttribute(UNDERTAKING_MAXIMUM, undertakingMaximum);
      model.addAttribute("statutoryChargeManualUrl", STATUTORY_CHARGE_MANUAL_URL);
      model.addAttribute("undertakingFormData", undertakingFormData);
      return "application/billing/enter-undertaking";
    }

    final String transactionId =
        amendmentService.submitQuickAmendmentUndertaking(
            undertakingFormData, ebsCase.getCaseReferenceNumber(), user);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);
    session.removeAttribute(SUBMISSION_RESULT);
    session.removeAttribute(UNDERTAKING_MINIMUM);
    session.removeAttribute(UNDERTAKING_MAXIMUM);
    model.addAttribute("undertakingFormData", new UndertakingFormData());
    return "redirect:/amendments/submit-case";
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

    final AssessmentDetail poaAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.POA);
    final boolean assessmentComplete = isComplete(poaAssessment);

    model.addAttribute("assessmentStatus", displayStatus(poaAssessment));
    model.addAttribute("assessmentComplete", assessmentComplete);
    // The legacy PUI only reveals the "POA summary" link, and the Action column holding it, once
    // the assessment is complete (PreparePOADetails' viewPOASummary flag).
    model.addAttribute("viewPoaSummary", assessmentComplete);

    return "application/billing/poa-details";
  }

  /**
   * Displays the "Create a Bill - Bill details" screen, the entry point to the bill billing
   * journey.
   *
   * <p>This ports the legacy PUI {@code CCMS_CB03}: entering the screen creates the draft bill if
   * the provider does not already have one ({@code AddBill}), and the screen then shows the status
   * of the billing assessment ({@code PrepareBillClaimSummary}). Selecting "Bill details" starts
   * the OPA interview; once it is complete a "Bill summary" link is offered, exactly as the legacy
   * screen did.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param model the model used to pass data to the view.
   * @return the bill details view.
   */
  @GetMapping("/case/billing/bill")
  public String createBill(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    billingService.createDraftBillIfAbsent(
        ebsCase.getCaseReferenceNumber(), providerId(user), user);

    final AssessmentDetail billingAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.BILLING);
    final boolean assessmentComplete = isComplete(billingAssessment);

    model.addAttribute("assessmentStatus", displayStatus(billingAssessment));
    model.addAttribute("assessmentComplete", assessmentComplete);
    // The legacy PUI only reveals the "Bill summary" link, and the Action column holding it, once
    // the assessment is complete (PrepareBillClaimSummary's printDraftBill flag).
    model.addAttribute("printDraftBill", assessmentComplete);

    return "application/billing/bill-details";
  }

  /**
   * Copies a rejected bill onto a new draft and opens the bill details screen for it.
   *
   * <p>This ports the legacy PUI {@code CopyBill}. A case carries at most one draft bill, and
   * copying creates one, so the copy is refused while a draft is already in progress - the same
   * rule that withholds the action in the bills/POA table, applied here so it cannot be worked
   * around by coming straight to this URL.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param billingId the billing incident id of the bill to copy.
   * @return a redirect to the bill details screen, or back to the statement when it cannot be
   *     copied.
   */
  @GetMapping("/case/billing/bill/copy")
  public String copyBill(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam("billing-id") final String billingId) {

    // Nothing can be fetched from EBS without an invoice to address, so refuse rather than asking
    // it for a blank one. The action is already withheld from rows carrying no billing incident
    // id; this covers the URL being reached directly.
    if (!canMaintainBill(ebsCase) || billingId == null || billingId.isBlank()) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final StatementOfAccountDisplay statementOfAccount =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);
    if (statementOfAccount.isDraftBillExists()) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    billingService.copyBill(ebsCase.getCaseReferenceNumber(), providerId(user), billingId, user);

    return "redirect:/case/billing/bill";
  }

  /**
   * Displays the "Delete bill" confirmation screen.
   *
   * @param ebsCase the case details from EBS.
   * @return the delete bill confirmation view.
   */
  @GetMapping("/case/billing/bill/remove")
  public String removeBillConfirmation(@SessionAttribute(CASE) final ApplicationDetail ebsCase) {
    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    return "application/billing/bill-remove";
  }

  /**
   * Deletes the provider's draft bill and the billing assessment data that went with it, then
   * returns to the case statement of account.
   *
   * <p>This ports the legacy PUI {@code RemoveBill}, which deletes the bill held for the case and
   * provider and removes the billing OPA sessions, including the pre-population session, so a later
   * bill starts from scratch rather than resuming the deleted one.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @return a redirect to the case statement of account.
   */
  @PostMapping("/case/billing/bill/remove")
  public String removeBill(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    billingService.deleteDraftBill(ebsCase.getCaseReferenceNumber(), providerId(user), user);

    deleteBillingAssessments(ebsCase, user);

    return CASE_STATEMENT_OF_ACCOUNT_URL;
  }

  private void deleteBillingAssessments(final ApplicationDetail ebsCase, final UserDetail user) {
    assessmentService
        .deleteAssessments(
            user,
            List.of(
                AssessmentRulebase.BILLING.getName(),
                AssessmentRulebase.BILLING.getPrePopAssessmentName()),
            ebsCase.getCaseReferenceNumber(),
            null)
        .block();
  }

  /**
   * Displays the bill declaration, the step between the bill details screen and submission.
   *
   * <p>This ports the legacy PUI {@code CCMS_CB03} submit chain. {@code PerformFinalValForBill}
   * runs first and, on failure, returns the user to the bill details screen carrying the reason
   * rather than letting the submission proceed. The declaration statements themselves are keyed on
   * the assessment's bill type, and the user must accept them all to submit.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param model the model used to pass data to the view.
   * @return the bill declaration view, or a redirect when the bill cannot be submitted.
   */
  @GetMapping("/case/billing/bill/declaration")
  public String billDeclaration(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final Model model) {

    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final AssessmentDetail billingAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.BILLING);

    final String validationError = billFinalValidationError(billingAssessment);
    if (validationError != null) {
      return billDetailsWithError(model, billingAssessment, validationError);
    }

    final List<DynamicCheckbox> options = declarationOptions(billingAssessment);
    if (options.isEmpty()) {
      // Nothing to acknowledge, so there is no declaration to show. Reached directly rather than
      // through Submit, so send the user back to the bill instead of submitting it.
      return "redirect:/case/billing/bill";
    }

    return declarationDetails(model, summarySubmissionFormData, options, BILL_DECLARATION_VIEW);
  }

  /**
   * Handles Submit on the bill details screen, showing the declaration when there is one to
   * acknowledge and submitting the bill straight away when there is not.
   *
   * <p>This is the legacy PUI's submit chain: {@code PerformFinalValForBill} runs first, then
   * {@code RetrieveDeclarationText} decides whether a declaration exists for this bill type. When
   * none is configured it sets {@code SHOW_DECLARATION} to false and the chain goes directly to
   * {@code PerformSubmission}, rather than showing an empty declaration screen.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param model the model used to pass data to the view.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return the declaration view, or a redirect to the confirmation when there is no declaration.
   */
  @PostMapping("/case/billing/bill/submit")
  public String billSubmit(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final Model model,
      final HttpSession session) {

    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final AssessmentDetail billingAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.BILLING);

    final String validationError = billFinalValidationError(billingAssessment);
    if (validationError != null) {
      return billDetailsWithError(model, billingAssessment, validationError);
    }

    final List<DynamicCheckbox> options = declarationOptions(billingAssessment);
    if (!options.isEmpty()) {
      return declarationDetails(model, summarySubmissionFormData, options, BILL_DECLARATION_VIEW);
    }

    return submitBillAndConfirm(ebsCase, user, billingAssessment, session);
  }

  /**
   * Submits the provider's draft bill to EBS once the declaration is acknowledged, and shows the
   * submission confirmation.
   *
   * <p>This ports the legacy PUI bill submit chain ({@code PerformFinalValForBill} to {@code
   * PerformSubmission}): the final validation is re-run so the declaration cannot be posted around
   * it, the declaration must be accepted in full, and the draft bill and its completed assessment
   * are then sent to EBS. The billing OPA sessions (including the pre-population) are removed
   * afterwards so a later bill starts afresh, as the legacy post-submission cleanup does.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param bindingResult the validation result for the declaration.
   * @param model the model used to pass data to the view.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return a redirect to the confirmation, or the declaration view when validation fails.
   */
  @PostMapping("/case/billing/bill/declaration")
  public String billDeclarationPost(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    if (!canMaintainBill(ebsCase)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final AssessmentDetail billingAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.BILLING);

    final String validationError = billFinalValidationError(billingAssessment);
    if (validationError != null) {
      return billDetailsWithError(model, billingAssessment, validationError);
    }

    // The bill and POA declarations share the legacy rule that every statement must be accepted.
    poaDeclarationValidator.validate(summarySubmissionFormData, bindingResult);
    if (bindingResult.hasErrors()) {
      return declarationDetails(
          model, summarySubmissionFormData, billingAssessment, BILL_DECLARATION_VIEW);
    }

    return submitBillAndConfirm(ebsCase, user, billingAssessment, session);
  }

  /**
   * Displays the bill submission confirmation, showing the reference EBS returned for the
   * submission.
   *
   * @param transactionId the submission reference carried from the submit step.
   * @param session the HTTP session, which the reference is cleared from.
   * @param model the model used to pass data to the view.
   * @return the bill confirmation view, or a redirect when there is no submission to confirm.
   */
  @GetMapping("/case/billing/bill/confirmation")
  public String billConfirmation(
      @SessionAttribute(name = SUBMISSION_TRANSACTION_ID, required = false)
          final String transactionId,
      final HttpSession session,
      final Model model) {

    if (transactionId == null) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    session.removeAttribute(SUBMISSION_TRANSACTION_ID);
    releaseSubmission(session);
    model.addAttribute("transactionId", transactionId);
    return "application/billing/bill-confirmation";
  }

  /**
   * Re-renders the bill details screen carrying a final validation failure, which is where the
   * legacy PUI leaves the user when the bill cannot be submitted.
   */
  private String billDetailsWithError(
      final Model model, final AssessmentDetail billingAssessment, final String errorMessageCode) {

    final boolean assessmentComplete = isComplete(billingAssessment);
    model.addAttribute("assessmentStatus", displayStatus(billingAssessment));
    model.addAttribute("assessmentComplete", assessmentComplete);
    model.addAttribute("printDraftBill", assessmentComplete);
    model.addAttribute("submissionError", errorMessageCode);

    return "application/billing/bill-details";
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

    final AssessmentDetail poaAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.POA);
    // A POA can only be submitted when the case allows it and the assessment is complete, the same
    // two conditions the details screen uses to reveal Submit. Guard the declaration too, so it
    // cannot be reached directly for a POA that is not ready.
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final List<DynamicCheckbox> options = declarationOptions(poaAssessment);
    if (options.isEmpty()) {
      // Nothing to acknowledge - as for the bill, do not show an empty declaration.
      return "redirect:/case/billing/poa";
    }

    return declarationDetails(model, summarySubmissionFormData, options, POA_DECLARATION_VIEW);
  }

  /**
   * Handles Submit on the POA details screen, showing the declaration when there is one to
   * acknowledge and submitting the payment on account straight away when there is not. See {@link
   * #billSubmit} for the legacy behaviour this follows.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param summarySubmissionFormData the declaration form data.
   * @param model the model used to pass data to the view.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return the declaration view, or a redirect to the confirmation when there is no declaration.
   */
  @PostMapping("/case/billing/poa/submit")
  public String poaSubmit(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("summarySubmissionFormData")
          final SummarySubmissionFormData summarySubmissionFormData,
      final Model model,
      final HttpSession session) {

    final AssessmentDetail poaAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.POA);
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    final List<DynamicCheckbox> options = declarationOptions(poaAssessment);
    if (!options.isEmpty()) {
      return declarationDetails(model, summarySubmissionFormData, options, POA_DECLARATION_VIEW);
    }

    return submitPoaAndConfirm(ebsCase, user, poaAssessment, session);
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

    final AssessmentDetail poaAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.POA);
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return CASE_STATEMENT_OF_ACCOUNT_URL;
    }

    poaDeclarationValidator.validate(summarySubmissionFormData, bindingResult);
    if (bindingResult.hasErrors()) {
      return poaDeclarationDetails(model, summarySubmissionFormData, poaAssessment);
    }

    return submitPoaAndConfirm(ebsCase, user, poaAssessment, session);
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
    releaseSubmission(session);
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

    final AssessmentDetail poaAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.POA);
    if (!canMaintainPoa(ebsCase) || !isComplete(poaAssessment)) {
      return summaryUnavailable();
    }

    return summaryPdf(
        "poa-summary.pdf",
        (statement, allocatedCostLimit) ->
            billingSummaryPdfService.generatePoaSummary(
                ebsCase, providerName(user), poaAssessment, statement, allocatedCostLimit),
        ebsCase,
        user);
  }

  /**
   * Streams the bill summary report as a PDF, generated from the completed billing assessment. This
   * ports the legacy PUI {@code GetBillSummary}, which is its {@code GetPoaSummary} bar the report
   * title and the assessment it reads, so both run through the same generator here too.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @return the PDF response, or a redirect when the bill summary cannot be shown.
   */
  @GetMapping("/case/billing/bill/summary")
  public ResponseEntity<byte[]> billSummary(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    final AssessmentDetail billingAssessment =
        getLatestAssessment(ebsCase, user, AssessmentRulebase.BILLING);
    if (!canMaintainBill(ebsCase) || !isComplete(billingAssessment)) {
      return summaryUnavailable();
    }

    return summaryPdf(
        "bill-summary.pdf",
        (statement, allocatedCostLimit) ->
            billingSummaryPdfService.generateBillSummary(
                ebsCase, providerName(user), billingAssessment, statement, allocatedCostLimit),
        ebsCase,
        user);
  }

  /**
   * Gathers the cost figures both summary reports need, renders one, and serves it inline so the
   * summary link opens it in a new tab.
   */
  private ResponseEntity<byte[]> summaryPdf(
      final String filename,
      final BiFunction<StatementOfAccountDisplay, BigDecimal, byte[]> render,
      final ApplicationDetail ebsCase,
      final UserDetail user) {

    final StatementOfAccountDisplay statement =
        billingService.getStatementOfAccountDisplay(
            ebsCase.getCaseReferenceNumber(), ebsCase, user);
    final BigDecimal allocatedCostLimit = billingService.getAllocatedCostLimit(statement, ebsCase);

    final String contentDisposition =
        ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(render.apply(statement, allocatedCostLimit));
  }

  private ResponseEntity<byte[]> summaryUnavailable() {
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/case/billing")).build();
  }

  private String poaDeclarationDetails(
      final Model model,
      final SummarySubmissionFormData summarySubmissionFormData,
      final AssessmentDetail poaAssessment) {

    return declarationDetails(
        model, summarySubmissionFormData, poaAssessment, POA_DECLARATION_VIEW);
  }

  /**
   * Builds the declaration screen for a bill or POA submission. The legacy PUI keys both from the
   * same table, on the assessment's {@code BILL_TYPE}, differing only in which assessment it reads
   * ({@code RetrieveDeclarationText} / {@code DeclarationHelper.getDeclarationTextsForBills}).
   */
  private String declarationDetails(
      final Model model,
      final SummarySubmissionFormData summarySubmissionFormData,
      final AssessmentDetail assessment,
      final String view) {

    return declarationDetails(
        model, summarySubmissionFormData, declarationOptions(assessment), view);
  }

  /**
   * Looks up the declaration statements the provider must acknowledge for an assessment's bill
   * type. An empty list means none is configured, which the legacy PUI treats as "do not show the
   * declaration" rather than as an empty screen.
   */
  private List<DynamicCheckbox> declarationOptions(final AssessmentDetail assessment) {
    final DeclarationLookupDetail declarations =
        lookupService.getDeclarations(DECLARATION_BILL, billType(assessment)).block();
    final List<DynamicCheckbox> options =
        submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(declarations);
    return options == null ? List.of() : options;
  }

  /** Renders a declaration screen from statements that have already been looked up. */
  private String declarationDetails(
      final Model model,
      final SummarySubmissionFormData summarySubmissionFormData,
      final List<DynamicCheckbox> declarationOptions,
      final String view) {

    if (summarySubmissionFormData.getDeclarationOptions() == null
        || summarySubmissionFormData.getDeclarationOptions().isEmpty()) {
      summarySubmissionFormData.setDeclarationOptions(declarationOptions);
    }
    model.addAttribute("summarySubmissionFormData", summarySubmissionFormData);
    return view;
  }

  /**
   * The bill type qualifies the declaration lookup. It is produced by the assessment and held on
   * the global entity, mirroring the legacy PUI, which reads the {@code BILL_TYPE} attribute from
   * the assessment session to key the declaration retrieval.
   */
  private String billType(final AssessmentDetail assessment) {
    return getAssessmentEntitiesForEntityType(assessment, AssessmentEntityType.GLOBAL).stream()
        .map(entity -> getAssessmentAttribute(entity, OPA_BILL_TYPE_ATTRIBUTE))
        .filter(attribute -> attribute != null && attribute.getValue() != null)
        .map(AssessmentAttributeDetail::getValue)
        .findFirst()
        .orElse(null);
  }

  /**
   * Runs the legacy PUI's final validation for a bill ({@code PerformFinalValForBill}) and returns
   * the message code of the first failure, or {@code null} when the bill may be submitted.
   *
   * <p>Two conditions block submission: the bill details must be complete, and a claim the provider
   * has said needs court assessment must actually have been assessed. The court answer is absent
   * for bills that never went near a court, which the legacy treats as assessed rather than as a
   * failure.
   */
  private String billFinalValidationError(final AssessmentDetail billingAssessment) {
    if (!isComplete(billingAssessment)) {
      return "billing.bill.error.notComplete";
    }

    return isCourtAssessed(billingAssessment) ? null : "billing.bill.error.notAssessed";
  }

  private boolean isCourtAssessed(final AssessmentDetail billingAssessment) {
    return getAssessmentEntitiesForEntityType(billingAssessment, AssessmentEntityType.GLOBAL)
        .stream()
        .map(entity -> getAssessmentAttribute(entity, OPA_COURT_ASSESSED_BILL_ATTRIBUTE))
        .filter(attribute -> attribute != null && attribute.getValue() != null)
        .map(AssessmentAttributeDetail::getValue)
        .findFirst()
        .map(Boolean::parseBoolean)
        .orElse(true);
  }

  private Set<String> availableFunctions(final ApplicationDetail ebsCase) {
    return ebsCase.getAvailableFunctions() == null
        ? Collections.emptySet()
        : Set.copyOf(ebsCase.getAvailableFunctions());
  }

  private boolean canMaintainPoa(final ApplicationDetail ebsCase) {
    return availableFunctions(ebsCase).contains(FunctionConstants.ADD_UPDATE_POA);
  }

  private boolean canMaintainBill(final ApplicationDetail ebsCase) {
    return availableFunctions(ebsCase).contains(FunctionConstants.ADD_UPDATE_BILL);
  }

  private String providerId(final UserDetail user) {
    return String.valueOf(user.getProvider().getId());
  }

  private String providerName(final UserDetail user) {
    return user.getProvider() == null ? null : user.getProvider().getName();
  }

  private AssessmentDetail getLatestAssessment(
      final ApplicationDetail ebsCase, final UserDetail user, final AssessmentRulebase rulebase) {
    final AssessmentDetails assessmentDetails =
        assessmentService
            .getAssessments(
                List.of(rulebase.getName()), providerId(user), ebsCase.getCaseReferenceNumber())
            .block();

    if (assessmentDetails == null || assessmentDetails.getContent() == null) {
      return null;
    }

    return getMostRecentAssessmentDetail(assessmentDetails.getContent());
  }

  private boolean isComplete(final AssessmentDetail assessment) {
    return assessment != null
        && AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(assessment.getStatus());
  }

  private String displayStatus(final AssessmentDetail assessment) {
    if (assessment == null || assessment.getStatus() == null) {
      return SECTION_STATUS_NOT_STARTED;
    }

    return AssessmentStatus.COMPLETE.getStatus().equalsIgnoreCase(assessment.getStatus())
        ? SECTION_STATUS_COMPLETE
        : assessment.getStatus();
  }

  /**
   * Sends the draft bill to EBS and hands the user to the confirmation. Shared by the declaration
   * POST and by Submit on the bill details screen, which skips the declaration when none is
   * configured for the bill type.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param billingAssessment the completed billing assessment.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return a redirect to the bill confirmation.
   */
  private String submitBillAndConfirm(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final AssessmentDetail billingAssessment,
      final HttpSession session) {

    if (!claimSubmission(session, BILL_SUBMISSION)) {
      // A second submit, which a double-clicked button sends before the first has finished. Both
      // would read the same draft bill before either removed it, so EBS would take two identical
      // bills. Send the user to the confirmation instead of submitting again.
      return "redirect:/case/billing/bill/confirmation";
    }

    final String transactionId;
    try {
      transactionId =
          billingService.submitBill(
              ebsCase.getCaseReferenceNumber(), providerId(user), billingAssessment, user);
    } catch (final RuntimeException e) {
      // Nothing reached EBS, so let the user try again in this session.
      releaseSubmission(session);
      throw e;
    }

    deleteBillingAssessments(ebsCase, user);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);

    return "redirect:/case/billing/bill/confirmation";
  }

  /**
   * Sends the draft payment on account to EBS and hands the user to the confirmation. Shared by the
   * declaration POST and by Submit on the POA details screen.
   *
   * @param ebsCase the case details from EBS.
   * @param user the logged-in user.
   * @param poaAssessment the completed POA assessment.
   * @param session the HTTP session, used to carry the submission reference to the confirmation.
   * @return a redirect to the POA confirmation.
   */
  private String submitPoaAndConfirm(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final AssessmentDetail poaAssessment,
      final HttpSession session) {

    if (!claimSubmission(session, POA_SUBMISSION)) {
      // As for the bill above - a repeat submit must not reach EBS a second time.
      return "redirect:/case/billing/poa/confirmation";
    }

    final String transactionId;
    try {
      transactionId =
          billingService.submitPaymentOnAccount(
              ebsCase.getCaseReferenceNumber(), providerId(user), poaAssessment, user);
    } catch (final RuntimeException e) {
      // Nothing reached EBS, so let the user try again in this session.
      releaseSubmission(session);
      throw e;
    }

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
   * Claims the single submission a declaration screen is allowed to make, returning {@code false}
   * when this session has already made it.
   *
   * <p>A double-clicked submit button sends two requests that run at the same time. Without this,
   * both read the same draft before either removes it and EBS accepts both, which is how one bill
   * was submitted twice for the same amount. The check and the claim are done under the session
   * mutex so two concurrent requests cannot both win it.
   *
   * @param session the current session.
   * @param submission which declaration is being submitted.
   * @return {@code true} if this request may submit, {@code false} if it is a repeat.
   */
  private boolean claimSubmission(final HttpSession session, final String submission) {
    synchronized (WebUtils.getSessionMutex(session)) {
      if (submission.equals(session.getAttribute(BILLING_SUBMISSION_SENT))) {
        return false;
      }
      session.setAttribute(BILLING_SUBMISSION_SENT, submission);
      return true;
    }
  }

  /**
   * Releases the submission claim, so the next bill or payment on account in this session can be
   * submitted. Called once the submission has been confirmed to the user, and when a submission
   * failed without reaching EBS.
   *
   * @param session the current session.
   */
  private void releaseSubmission(final HttpSession session) {
    synchronized (WebUtils.getSessionMutex(session)) {
      session.removeAttribute(BILLING_SUBMISSION_SENT);
    }
  }
}
