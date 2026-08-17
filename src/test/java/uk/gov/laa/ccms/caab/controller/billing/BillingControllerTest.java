package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.declaration.PoaDeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.PoaSummaryPdfService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing controller tests")
class BillingControllerTest {

  @Mock BillingService billingService;

  @Mock AssessmentService assessmentService;

  @Mock LookupService lookupService;

  @Mock SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;

  @Mock PoaDeclarationSubmissionValidator poaDeclarationValidator;

  @Mock PoaSummaryPdfService poaSummaryPdfService;

  @InjectMocks BillingController billingController;

  private MockMvcTester mockMvc;

  private final UserDetail user =
      new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(billingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
    // By default the case has no draft bill or payments on account.
    lenient()
        .when(billingService.getStatementOfAccountDisplay(any(), any(), any()))
        .thenReturn(new StatementOfAccountDisplay());
  }

  @Nested
  @DisplayName("GET: /case/billing")
  class CaseStatementOfAccount {

    @Test
    @DisplayName("Returns the statement of account view with the case reference number and figures")
    void returnsViewAndCaseReference() {
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("300000123");
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/case-statement-of-account")
          .model()
          .containsEntry("caseReferenceNumber", "300000123")
          .containsEntry("statementOfAccount", display)
          // The pagination links return to the bills/POA section rather than the top of the page.
          .containsEntry("paginationAnchor", "bills-and-poa");
    }

    @Test
    @DisplayName("Paginates the bills and POA table, ten rows per page, in line with other tables")
    void paginatesBillsAndPoa() {
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("300000123");
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      List<BillPoaRow> rows = new ArrayList<>();
      for (int i = 0; i < 25; i++) {
        rows.add(new BillPoaRow("POA", "Authorised", null, null, new BigDecimal("1.00"), false));
      }
      display.setBillsAndPoa(rows);
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing")
                      .param("page", "1")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .extractingByKey("billsAndPoaPage")
          .isInstanceOfSatisfying(
              Page.class,
              page -> {
                // 25 rows at the default size of 10 gives three pages; page 1 holds ten of them.
                assertThat(page.getTotalElements()).isEqualTo(25);
                assertThat(page.getTotalPages()).isEqualTo(3);
                assertThat(page.getNumber()).isEqualTo(1);
                assertThat(page.getContent()).hasSize(10);
              });
    }

    @Test
    @DisplayName("Shows all billing actions when the case carries the relevant functions")
    void showsActionsWhenFunctionsPresent() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(
                  List.of(
                      FunctionConstants.ENTER_UNDERTAKING,
                      FunctionConstants.ADD_UPDATE_BILL,
                      FunctionConstants.ADD_UPDATE_POA));

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("showEnterUndertaking", true)
          .containsEntry("showCreateBill", true)
          .containsEntry("showCreatePoa", true);
    }

    @Test
    @DisplayName("Hides create bill and POA when a draft already exists, even with the functions")
    void hidesCreateWhenDraftExists() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(
                  List.of(FunctionConstants.ADD_UPDATE_BILL, FunctionConstants.ADD_UPDATE_POA));
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      display.setDraftBillExists(true);
      display.setDraftPoaExists(true);
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("showCreateBill", false)
          .containsEntry("showCreatePoa", false);
    }

    @Test
    @DisplayName("Hides billing actions when the case does not carry the relevant functions")
    void hidesActionsWhenFunctionsAbsent() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("showEnterUndertaking", false)
          .containsEntry("showCreateBill", false)
          .containsEntry("showCreatePoa", false);
    }

    @Test
    @DisplayName("Handles a case with no available functions without error")
    void handlesNullAvailableFunctions() {
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("300000123");

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("showEnterUndertaking", false)
          .containsEntry("showCreateBill", false)
          .containsEntry("showCreatePoa", false);
    }

    @Test
    @DisplayName("Offers the draft POA's edit and delete links only with the POA function")
    void showsPoaMaintenanceOnlyWithFunction() {
      ApplicationDetail withFunction =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_POA));
      ApplicationDetail withoutFunction =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing")
                      .sessionAttr(CASE, withFunction)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("canMaintainPoa", true);

      assertThat(
              mockMvc.perform(
                  get("/case/billing")
                      .sessionAttr(CASE, withoutFunction)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("canMaintainPoa", false);
    }
  }

  @Nested
  @DisplayName("GET: /case/billing/poa")
  class CreatePoa {

    private ApplicationDetail caseWithPoaFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_POA));
    }

    @Test
    @DisplayName("Creates the draft POA if absent and shows the POA details screen")
    void createsDraftAndShowsScreen() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/poa-details")
          .model()
          // Nothing started yet, so no summary link is offered.
          .containsEntry("assessmentStatus", "Not started")
          .containsEntry("assessmentComplete", false)
          .containsEntry("viewPoaSummary", false);

      verify(billingService).createDraftPaymentOnAccountIfAbsent("300000123", "10", user);
    }

    @Test
    @DisplayName("Offers the POA summary once the assessment is complete")
    void offersSummaryWhenComplete() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.POA.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus()))));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("assessmentComplete", true)
          .containsEntry("viewPoaSummary", true);
    }

    @Test
    @DisplayName("Redirects back without creating a draft when the case lacks the POA function")
    void redirectsWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }
  }

  @Nested
  @DisplayName("/case/billing/poa/remove")
  class RemovePoa {

    private ApplicationDetail caseWithPoaFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_POA));
    }

    @Test
    @DisplayName("GET shows the delete payment on account confirmation screen")
    void showsConfirmation() {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/remove")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/poa-remove");
    }

    @Test
    @DisplayName(
        "POST deletes the draft POA and its assessment data, then returns to the statement")
    void deletesPoaAndAssessments() {
      when(assessmentService.deleteAssessments(any(), any(), any(), any()))
          .thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/poa/remove")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verify(billingService).deleteDraftPaymentsOnAccount("300000123", "10", user);
      // The pre-population assessment goes too, so a later POA starts fresh rather than resuming
      // the deleted interview (old PUI removeOpaSessionsForAssessmentIncludingPrepop).
      verify(assessmentService)
          .deleteAssessments(
              eq(user),
              eq(List.of("poaAssessment", "poaAssessment_PREPOP")),
              eq("300000123"),
              any());
    }

    @Test
    @DisplayName("POST does nothing when the case does not carry the POA function")
    void doesNothingWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  post("/case/billing/poa/remove")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
      verifyNoInteractions(assessmentService);
    }
  }

  @Nested
  @DisplayName("/case/billing/poa/declaration")
  class PoaDeclaration {

    private ApplicationDetail caseWithPoaFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_POA));
    }

    private void completeAssessment() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.POA.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus())
                              .entityTypes(new ArrayList<>()))));
    }

    @Test
    @DisplayName("GET shows the declaration screen with the looked-up declaration options")
    void showsDeclaration() {
      completeAssessment();
      when(lookupService.getDeclarations(eq("BILL"), any()))
          .thenReturn(Mono.just(new DeclarationLookupDetail()));
      when(submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(any()))
          .thenReturn(List.of(new DynamicCheckbox()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/declaration")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/poa-declaration")
          .model()
          .containsKey("summarySubmissionFormData");
    }

    @Test
    @DisplayName("GET redirects when the assessment is not complete")
    void redirectsWhenNotComplete() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/declaration")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }

    @Test
    @DisplayName("POST submits the POA, clears the assessments and redirects to the confirmation")
    void submitsAndRedirects() {
      completeAssessment();
      when(billingService.submitPaymentOnAccount(eq("300000123"), eq("10"), any(), any()))
          .thenReturn("INV-9");
      when(assessmentService.deleteAssessments(any(), any(), eq("300000123"), any()))
          .thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/poa/declaration")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing/poa/confirmation");

      verify(billingService).submitPaymentOnAccount(eq("300000123"), eq("10"), any(), eq(user));
      // The POA OPA sessions, including the pre-population, are removed after submission.
      verify(assessmentService)
          .deleteAssessments(
              eq(user),
              eq(
                  List.of(
                      AssessmentRulebase.POA.getName(),
                      AssessmentRulebase.POA.getPrePopAssessmentName())),
              eq("300000123"),
              eq(null));
    }

    @Test
    @DisplayName("POST re-renders the declaration when it is not acknowledged")
    void reRendersOnValidationError() {
      completeAssessment();
      when(lookupService.getDeclarations(eq("BILL"), any()))
          .thenReturn(Mono.just(new DeclarationLookupDetail()));
      when(submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(any()))
          .thenReturn(List.of(new DynamicCheckbox()));
      doAnswer(
              invocation -> {
                final Errors errors = invocation.getArgument(1);
                errors.reject("declaration.required", "You must acknowledge the declaration.");
                return null;
              })
          .when(poaDeclarationValidator)
          .validate(any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/poa/declaration")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/poa-declaration");

      verifyNoInteractions(billingService);
    }

    @Test
    @DisplayName("Confirmation shows the submission reference from the session")
    void confirmationShowsReference() {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/confirmation")
                      .sessionAttr(SUBMISSION_TRANSACTION_ID, "INV-9")))
          .hasStatusOk()
          .hasViewName("application/billing/poa-confirmation")
          .model()
          .containsEntry("transactionId", "INV-9");
    }

    @Test
    @DisplayName("Confirmation redirects when there is no submission in the session")
    void confirmationRedirectsWhenNoSubmission() {
      assertThat(mockMvc.perform(get("/case/billing/poa/confirmation")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");
    }
  }

  @Nested
  @DisplayName("GET: /case/billing/poa/summary")
  class PoaSummary {

    private ApplicationDetail caseWithPoaFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_POA));
    }

    @Test
    @DisplayName("Streams the POA summary PDF when the assessment is complete")
    void streamsPdf() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.POA.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus())
                              .entityTypes(new ArrayList<>()))));
      when(billingService.getAllocatedCostLimit(any(), any())).thenReturn(new BigDecimal("100.00"));
      when(poaSummaryPdfService.generatePoaSummary(any(), any(), any(), any(), any()))
          .thenReturn("%PDF-1.4 stub".getBytes());

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/summary")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasContentType(MediaType.APPLICATION_PDF);

      verify(poaSummaryPdfService).generatePoaSummary(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Redirects when the assessment is not complete")
    void redirectsWhenNotComplete() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/summary")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(poaSummaryPdfService);
    }
  }
}
