package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
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
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupValueDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountCostLimitation;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing controller tests")
class BillingControllerTest {

  @Mock BillingService billingService;

  @Mock BillingUndertakingValidator billingUndertakingValidator;

  @Mock AmendmentService amendmentService;

  @Mock AssessmentService assessmentService;

  @Mock LookupService lookupService;

  @Mock SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;

  @Mock PoaDeclarationSubmissionValidator poaDeclarationValidator;

  @Mock BillingSummaryPdfService billingSummaryPdfService;

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
    @DisplayName("Explains the withheld create and copy actions while a draft is in progress")
    void explainsWhyActionsAreWithheldWhileDraftExists() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      display.setDraftBillExists(true);
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("draftInProgress", true);
    }

    @Test
    @DisplayName("A draft POA alone also explains the withheld actions, as it withholds them too")
    void explainsWhenOnlyADraftPoaExists() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      display.setDraftPoaExists(true);
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("draftInProgress", true);
    }

    @Test
    @DisplayName("Carries no draft explanation when nothing is in progress")
    void noExplanationWithoutADraft() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));

      assertThat(
              mockMvc.perform(
                  get("/case/billing").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("draftInProgress", false);
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

    @Test
    @DisplayName("Offers the draft bill's edit and delete links only with the bill function")
    void showsBillMaintenanceOnlyWithFunction() {
      ApplicationDetail withFunction =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
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
          .containsEntry("canMaintainBill", true);

      assertThat(
              mockMvc.perform(
                  get("/case/billing")
                      .sessionAttr(CASE, withoutFunction)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("canMaintainBill", false);
    }
  }

  @Nested
  @DisplayName("GET: /case/billing/undertaking")
  class EnterUndertaking {

    @Test
    @DisplayName("Returns undertaking view, clears cached range and initialises form data")
    void returnsUndertakingViewAndClearsRange() {
      final var result =
          mockMvc.perform(
              get("/case/billing/undertaking")
                  .sessionAttr("undertakingMinimum", new BigDecimal("100"))
                  .sessionAttr("undertakingMaximum", new BigDecimal("900")));

      assertThat(result)
          .hasStatusOk()
          .hasViewName("application/billing/enter-undertaking")
          .model()
          .hasEntrySatisfying(
              "undertakingFormData",
              value -> assertThat(value).isInstanceOf(UndertakingFormData.class));

      assertThat(result)
          .request()
          .sessionAttributes()
          .doesNotContainKeys("undertakingMinimum", "undertakingMaximum");
    }
  }

  @Nested
  @DisplayName("POST: /case/billing/undertaking")
  class SaveUndertaking {

    @Test
    @DisplayName("Submits undertaking and redirects to amendment submission")
    void submitsUndertakingAndRedirects() {
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("300000123");
      when(amendmentService.submitQuickAmendmentUndertaking(any(), eq("300000123"), eq(user)))
          .thenReturn("TRANS123");

      assertThat(
              mockMvc.perform(
                  post("/case/billing/undertaking")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr("undertakingMinimum", new BigDecimal("100.00"))
                      .sessionAttr("undertakingMaximum", new BigDecimal("900.00"))
                      .sessionAttr(SUBMISSION_RESULT, "old-result")
                      .param("undertakingAmount", "250.00")
                      .param("acceptedTerms", "true")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/amendments/submit-case")
          .request()
          .sessionAttributes()
          .containsEntry(SUBMISSION_TRANSACTION_ID, "TRANS123")
          .doesNotContainKeys(SUBMISSION_RESULT, "undertakingMinimum", "undertakingMaximum");

      verify(amendmentService)
          .submitQuickAmendmentUndertaking(
              argThat(
                  formData ->
                      formData != null
                          && "250.00".equals(formData.getUndertakingAmount())
                          && formData.isAcceptedTerms()
                          && formData.getUndertakingMaximumAmount() != null
                          && formData
                                  .getUndertakingMaximumAmount()
                                  .compareTo(new BigDecimal("900.00"))
                              == 0),
              eq("300000123"),
              eq(user));
    }

    @Test
    @DisplayName("Returns undertaking view with cached range when validation fails")
    void returnsViewWithCachedRangeWhenValidationFails() {
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("300000123");
      StatementOfAccountDetail statement = new StatementOfAccountDetail();
      statement.setBills(new StatementOfAccountBills().totalAmount(new BigDecimal("300.00")));
      statement.setCostLimitation(
          new StatementOfAccountCostLimitation().remainingAmount(new BigDecimal("900.00")));

      when(billingService.getCurrentProviderStatement(eq("300000123"), any(), any()))
          .thenReturn(statement);
      doAnswer(
              invocation -> {
                Errors errors = invocation.getArgument(1);
                errors.rejectValue("undertakingAmount", "billing.undertakingAmount.required");
                return null;
              })
          .when(billingUndertakingValidator)
          .validate(any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/undertaking")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("undertakingAmount", "")))
          .hasStatusOk()
          .hasViewName("application/billing/enter-undertaking")
          .request()
          .sessionAttributes()
          .containsEntry("undertakingMinimum", new BigDecimal("300.00"))
          .containsEntry("undertakingMaximum", new BigDecimal("900.00"));

      verify(amendmentService, never()).submitQuickAmendmentUndertaking(any(), any(), any());
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
  @DisplayName("GET: /case/billing/bill")
  class CreateBill {

    private ApplicationDetail caseWithBillFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
    }

    @Test
    @DisplayName("Creates the draft bill if absent and shows the bill details screen")
    void createsDraftAndShowsScreen() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-details")
          .model()
          // Nothing started yet, so no bill summary link is offered.
          .containsEntry("assessmentStatus", "Not started")
          .containsEntry("assessmentComplete", false)
          .containsEntry("printDraftBill", false);

      verify(billingService).createDraftBillIfAbsent("300000123", "10", user);
    }

    @Test
    @DisplayName("Offers the bill summary once the assessment is complete")
    void offersSummaryWhenComplete() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.BILLING.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus()))));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .containsEntry("assessmentComplete", true)
          .containsEntry("printDraftBill", true);
    }

    @Test
    @DisplayName("Looks the status up against the billing rulebase, not the POA one")
    void readsBillingRulebase() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      mockMvc.perform(
          get("/case/billing/bill")
              .sessionAttr(CASE, caseWithBillFunction())
              .sessionAttr(USER_DETAILS, user));

      verify(assessmentService)
          .getAssessments(
              eq(List.of(AssessmentRulebase.BILLING.getName())), eq("10"), eq("300000123"));
    }

    @Test
    @DisplayName("Redirects back without creating a draft when the case lacks the bill function")
    void redirectsWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }
  }

  @Nested
  @DisplayName("GET: /case/billing/bill/copy")
  class CopyBill {

    private ApplicationDetail caseWithBillFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
    }

    @Test
    @DisplayName("Copies the rejected bill and opens the bill details screen")
    void copiesAndOpensBillDetails() {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/copy")
                      .param("billing-id", "555")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing/bill");

      verify(billingService).copyBill("300000123", "10", "555", user);
    }

    @Test
    @DisplayName("Refuses the copy when no billing incident id is supplied")
    void refusesWithoutBillingId() {
      // A row EBS gave no id for renders the link with an empty billing-id, and the URL can be
      // reached directly; either way there is nothing to ask EBS for.
      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/copy")
                      .param("billing-id", "")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verify(billingService, never()).copyBill(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Refuses the copy while a draft bill is already in progress")
    void refusesWhileDraftExists() {
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      display.setDraftBillExists(true);
      when(billingService.getStatementOfAccountDisplay(eq("300000123"), any(), any()))
          .thenReturn(display);

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/copy")
                      .param("billing-id", "555")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      // A case carries at most one draft bill, so the URL cannot be used to get a second.
      verify(billingService, never()).copyBill(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Refuses the copy when the case does not carry the bill function")
    void refusesWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/copy")
                      .param("billing-id", "555")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }
  }

  @Nested
  @DisplayName("/case/billing/bill/remove")
  class RemoveBill {

    private ApplicationDetail caseWithBillFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
    }

    @Test
    @DisplayName("GET shows the delete bill confirmation screen")
    void showsConfirmation() {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/remove")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-remove");
    }

    @Test
    @DisplayName("POST deletes the draft bill and its assessment data, then returns")
    void deletesBillAndAssessments() {
      when(assessmentService.deleteAssessments(any(), any(), any(), any()))
          .thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/remove")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verify(billingService).deleteDraftBill("300000123", "10", user);
      // The legacy RemoveBill clears the billing sessions including the pre-population, so a later
      // bill starts from scratch.
      verify(assessmentService)
          .deleteAssessments(
              eq(user),
              eq(
                  List.of(
                      AssessmentRulebase.BILLING.getName(),
                      AssessmentRulebase.BILLING.getPrePopAssessmentName())),
              eq("300000123"),
              isNull());
    }

    @Test
    @DisplayName("Redirects away without deleting when the case lacks the bill function")
    void redirectsWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/remove")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }
  }

  @Nested
  @DisplayName("/case/billing/bill/declaration")
  class SubmitBill {

    private ApplicationDetail caseWithBillFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
    }

    private void billingAssessment(final String status, final AssessmentEntityDetail global) {
      final AssessmentDetail assessment =
          new AssessmentDetail().name(AssessmentRulebase.BILLING.getName()).status(status);
      if (global != null) {
        assessment.entityTypes(
            List.of(
                new AssessmentEntityTypeDetail()
                    .name(AssessmentEntityType.GLOBAL.getType())
                    .entities(List.of(global))));
      }
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails().addContentItem(assessment)));
    }

    /** Reference data has a declaration for this bill type, so the screen has one to show. */
    private void declarationConfigured() {
      when(lookupService.getDeclarations(any(), any()))
          .thenReturn(
              Mono.just(
                  new DeclarationLookupDetail()
                      .addContentItem(new DeclarationLookupValueDetail().text("I agree"))));
      when(submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(any()))
          .thenReturn(List.of(new DynamicCheckbox()));
    }

    private AssessmentEntityDetail global(final String name, final String value) {
      return new AssessmentEntityDetail()
          .attributes(List.of(new AssessmentAttributeDetail().name(name).value(value)));
    }

    @Test
    @DisplayName("Blocks submission and explains why when the bill details are incomplete")
    void blocksIncompleteBill() {
      billingAssessment("INCOMPLETE", null);

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          // The legacy PUI returns the user to the bill details screen carrying the reason.
          .hasViewName("application/billing/bill-details")
          .model()
          .containsEntry("submissionError", "billing.bill.error.notComplete");
    }

    @Test
    @DisplayName("Blocks submission of a court-assessed claim the court has not yet assessed")
    void blocksClaimAwaitingCourtAssessment() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "false"));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-details")
          .model()
          .containsEntry("submissionError", "billing.bill.error.notAssessed");
    }

    @Test
    @DisplayName("Shows the declaration for a complete, court-assessed bill")
    void showsDeclaration() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      declarationConfigured();

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-declaration");
    }

    @Test
    @DisplayName("A bill that never went to court is treated as assessed, as the legacy PUI does")
    void treatsAbsentCourtAnswerAsAssessed() {
      billingAssessment(AssessmentStatus.COMPLETE.getStatus(), null);
      declarationConfigured();

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-declaration");
    }

    @Test
    @DisplayName("Keys the declaration on the assessment's bill type")
    void keysDeclarationOnBillType() {
      billingAssessment(AssessmentStatus.COMPLETE.getStatus(), global("BILL_TYPE", "CLAIM"));
      declarationConfigured();

      mockMvc.perform(
          get("/case/billing/bill/declaration")
              .sessionAttr(CASE, caseWithBillFunction())
              .sessionAttr(USER_DETAILS, user));

      verify(lookupService).getDeclarations("BILL", "CLAIM");
    }

    @Test
    @DisplayName("POST submits the bill, clears the billing assessments and confirms")
    void submitsBill() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      when(billingService.submitBill(eq("300000123"), eq("10"), any(), eq(user)))
          .thenReturn("INV-9");
      when(assessmentService.deleteAssessments(any(), any(), any(), any()))
          .thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/declaration")
                      .param("declarationOptions[0].fieldValueDisplayValue", "I agree")
                      .param("declarationOptions[0].checked", "true")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing/bill/confirmation");

      verify(billingService).submitBill(eq("300000123"), eq("10"), any(), eq(user));
      verify(assessmentService)
          .deleteAssessments(
              eq(user),
              eq(
                  List.of(
                      AssessmentRulebase.BILLING.getName(),
                      AssessmentRulebase.BILLING.getPrePopAssessmentName())),
              eq("300000123"),
              isNull());
    }

    @Test
    @DisplayName("Submit skips the declaration and sends the bill when none is configured")
    void skipsEmptyDeclaration() {
      // Given - reference data holds no declaration for this bill type
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      when(lookupService.getDeclarations(any(), any()))
          .thenReturn(Mono.just(new DeclarationLookupDetail()));
      when(billingService.submitBill(eq("300000123"), eq("10"), any(), eq(user)))
          .thenReturn("INV-9");
      when(assessmentService.deleteAssessments(any(), any(), any(), any()))
          .thenReturn(Mono.empty());

      // When
      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/submit")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing/bill/confirmation");

      // Then - straight to EBS, exactly as old PUI does when SHOW_DECLARATION is false
      verify(billingService).submitBill(eq("300000123"), eq("10"), any(), eq(user));
    }

    @Test
    @DisplayName("Submit shows the declaration, and sends nothing, when one is configured")
    void showsDeclarationBeforeSubmitting() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      declarationConfigured();

      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/submit")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-declaration");

      verify(billingService, never()).submitBill(any(), any(), any(), any());
    }

    @Test
    @DisplayName("The declaration screen redirects to the bill when none is configured")
    void declarationRedirectsWhenEmpty() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      when(lookupService.getDeclarations(any(), any()))
          .thenReturn(Mono.just(new DeclarationLookupDetail()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing/bill");
    }

    @Test
    @DisplayName("POST does not submit the bill again when the declaration is submitted twice")
    void doesNotSubmitBillTwice() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      when(billingService.submitBill(eq("300000123"), eq("10"), any(), eq(user)))
          .thenReturn("INV-9");
      when(assessmentService.deleteAssessments(any(), any(), any(), any()))
          .thenReturn(Mono.empty());

      final MockHttpSession session = new MockHttpSession();
      session.setAttribute(CASE, caseWithBillFunction());
      session.setAttribute(USER_DETAILS, user);

      for (int attempt = 0; attempt < 2; attempt++) {
        assertThat(
                mockMvc.perform(
                    post("/case/billing/bill/declaration")
                        .param("declarationOptions[0].fieldValueDisplayValue", "I agree")
                        .param("declarationOptions[0].checked", "true")
                        .session(session)))
            .hasStatus3xxRedirection()
            .hasRedirectedUrl("/case/billing/bill/confirmation");
      }

      // One bill, however many times the declaration was posted.
      verify(billingService, times(1)).submitBill(eq("300000123"), eq("10"), any(), eq(user));
    }

    @Test
    @DisplayName("POST re-shows the declaration without submitting when it is not fully accepted")
    void rejectsUnacceptedDeclaration() {
      billingAssessment(
          AssessmentStatus.COMPLETE.getStatus(), global("COURT_ASSESSED_BILL", "true"));
      declarationConfigured();
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
                  post("/case/billing/bill/declaration")
                      .param("declarationOptions[0].fieldValueDisplayValue", "I agree")
                      .param("declarationOptions[0].checked", "false")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-declaration");

      verify(billingService, never()).submitBill(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST cannot be used to submit around the final validation")
    void postRerunsFinalValidation() {
      billingAssessment("INCOMPLETE", null);

      assertThat(
              mockMvc.perform(
                  post("/case/billing/bill/declaration")
                      .param("declarationOptions[0].fieldValueDisplayValue", "I agree")
                      .param("declarationOptions[0].checked", "true")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/billing/bill-details");

      verify(billingService, never()).submitBill(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Redirects away when the case does not carry the bill function")
    void redirectsWhenNotAuthorised() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .caseReferenceNumber("300000123")
              .availableFunctions(List.of(FunctionConstants.BILLING));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/declaration")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingService);
    }

    @Test
    @DisplayName("The confirmation shows the submission reference and then clears it")
    void showsConfirmation() {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/confirmation")
                      .sessionAttr(SUBMISSION_TRANSACTION_ID, "INV-9")))
          .hasStatusOk()
          .hasViewName("application/billing/bill-confirmation")
          .model()
          .containsEntry("transactionId", "INV-9");
    }

    @Test
    @DisplayName("The confirmation redirects away when there is no submission to confirm")
    void redirectsWithoutASubmission() {
      assertThat(mockMvc.perform(get("/case/billing/bill/confirmation")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");
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
      when(billingSummaryPdfService.generatePoaSummary(any(), any(), any(), any(), any()))
          .thenReturn("%PDF-1.4 stub".getBytes());

      assertThat(
              mockMvc.perform(
                  get("/case/billing/poa/summary")
                      .sessionAttr(CASE, caseWithPoaFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasContentType(MediaType.APPLICATION_PDF);

      verify(billingSummaryPdfService).generatePoaSummary(any(), any(), any(), any(), any());
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

      verifyNoInteractions(billingSummaryPdfService);
    }
  }

  @Nested
  @DisplayName("GET: /case/billing/bill/summary")
  class BillSummary {

    private ApplicationDetail caseWithBillFunction() {
      return new ApplicationDetail()
          .caseReferenceNumber("300000123")
          .availableFunctions(List.of(FunctionConstants.ADD_UPDATE_BILL));
    }

    @Test
    @DisplayName("Streams the bill summary PDF when the assessment is complete")
    void streamsPdf() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.BILLING.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus())
                              .entityTypes(new ArrayList<>()))));
      when(billingService.getAllocatedCostLimit(any(), any())).thenReturn(new BigDecimal("100.00"));
      when(billingSummaryPdfService.generateBillSummary(any(), any(), any(), any(), any()))
          .thenReturn("%PDF-1.4 stub".getBytes());

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/summary")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasContentType(MediaType.APPLICATION_PDF);

      verify(billingSummaryPdfService).generateBillSummary(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Redirects when the assessment is not complete")
    void redirectsWhenNotComplete() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(Mono.just(new AssessmentDetails()));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/summary")
                      .sessionAttr(CASE, caseWithBillFunction())
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingSummaryPdfService);
    }

    @Test
    @DisplayName("Redirects when the case does not allow a bill to be maintained")
    void redirectsWhenBillNotAllowed() {
      when(assessmentService.getAssessments(any(), any(), any()))
          .thenReturn(
              Mono.just(
                  new AssessmentDetails()
                      .addContentItem(
                          new AssessmentDetail()
                              .name(AssessmentRulebase.BILLING.getName())
                              .status(AssessmentStatus.COMPLETE.getStatus())
                              .entityTypes(new ArrayList<>()))));

      assertThat(
              mockMvc.perform(
                  get("/case/billing/bill/summary")
                      .sessionAttr(CASE, new ApplicationDetail().caseReferenceNumber("300000123"))
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");

      verifyNoInteractions(billingSummaryPdfService);
    }
  }
}
