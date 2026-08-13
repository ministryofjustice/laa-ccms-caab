package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.validation.Errors;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;
import uk.gov.laa.ccms.caab.bean.validators.billing.BillingUndertakingValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
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

  @InjectMocks BillingController billingController;

  private MockMvcTester mockMvc;

  private final UserDetail user = new UserDetail().loginId("user1").userType("EXTERNAL");

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
              "undertakingFormData", value -> assertThat(value).isInstanceOf(UndertakingFormData.class));

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
      when(amendmentService.submitQuickAmendmentUndertaking(
              any(), eq("300000123"), eq(user)))
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
          .doesNotContainKeys(
              SUBMISSION_RESULT, "undertakingMinimum", "undertakingMaximum");

      verify(amendmentService)
          .submitQuickAmendmentUndertaking(
              argThat(
                  formData ->
                      formData != null
                          && "250.00".equals(formData.getUndertakingAmount())
                          && formData.isAcceptedTerms()
                          && formData.getUndertakingMaximumAmount() != null
                          && formData.getUndertakingMaximumAmount()
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

      verify(amendmentService, never())
          .submitQuickAmendmentUndertaking(any(), any(), any());
    }
  }
}
