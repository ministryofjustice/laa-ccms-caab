package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing controller tests")
class BillingControllerTest {

  @Mock BillingService billingService;

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
          .containsEntry("statementOfAccount", display);
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
}
