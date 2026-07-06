package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseStatementOfAccount;
import uk.gov.laa.ccms.soa.gateway.model.SoaBillSummary;
import uk.gov.laa.ccms.soa.gateway.model.SoaInvoice;
import uk.gov.laa.ccms.soa.gateway.model.SoaStatement;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing service tests")
class BillingServiceTest {

  private static final String CASE_REF = "300000123456";

  @Mock SoaApiClient soaApiClient;

  @InjectMocks BillingService billingService;

  private SoaStatement statement(
      String entityType, String firmId, String firmName, BigDecimal billsAuthorised) {
    return new SoaStatement()
        .entityType(entityType)
        .firmId(firmId)
        .firmName(firmName)
        .bills(new SoaBillSummary().amountAuthorized(billsAuthorised));
  }

  @Test
  @DisplayName("Buckets statements into provider, counsel and prior solicitor columns")
  void bucketsStatements() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    CaseStatementOfAccount soa =
        new CaseStatementOfAccount()
            .addStatementsItem(
                statement("PROVIDER", "10", "Current Firm", new BigDecimal("100"))
                    .addInvoicesItem(
                        new SoaInvoice()
                            .invoiceType("Bill")
                            .invoiceStatus("Authorised")
                            .invoiceAmount(new BigDecimal("100")))
                    .addInvoicesItem(new SoaInvoice().invoiceType("Bill").invoiceStatus("Draft")))
            .addStatementsItem(statement("COUNSEL", "55", "Counsel A", new BigDecimal("20")))
            .addStatementsItem(statement("PROVIDER", "99", "Prior Sol", new BigDecimal("30")));

    when(soaApiClient.getCaseStatementOfAccount(CASE_REF, "user1", "EXTERNAL"))
        .thenReturn(Mono.just(soa));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isUserBelongsToCurrentProvider()).isTrue();
    assertThat(display.getProviderFirmName()).isEqualTo("Current Firm");
    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("100");
    assertThat(display.getCounsel().getBillsAuthorised()).isEqualByComparingTo("20");
    assertThat(display.getPriorSolicitor().getBillsAuthorised()).isEqualByComparingTo("30");
    // The EBS draft invoice is stripped; only the authorised bill remains.
    assertThat(display.getInvoices()).hasSize(1);
    assertThat(display.getInvoices().get(0).getInvoiceStatus()).isEqualTo("Authorised");
  }

  @Test
  @DisplayName("When user does not belong to the case provider, only their statement is shown")
  void userNotOnCaseProvider() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(77));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    CaseStatementOfAccount soa =
        new CaseStatementOfAccount()
            .addStatementsItem(statement("PROVIDER", "77", "Other Firm", new BigDecimal("50")));

    when(soaApiClient.getCaseStatementOfAccount(CASE_REF, "user1", "EXTERNAL"))
        .thenReturn(Mono.just(soa));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isUserBelongsToCurrentProvider()).isFalse();
    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("50");
    assertThat(display.getCounsel().getBillsAuthorised()).isNull();
    assertThat(display.getPriorSolicitor().getBillsAuthorised()).isNull();
  }

  @Test
  @DisplayName("Returns an empty display when the SOA response is null")
  void nullResponse() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber(CASE_REF);

    when(soaApiClient.getCaseStatementOfAccount(CASE_REF, "user1", "EXTERNAL"))
        .thenReturn(Mono.empty());

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getProvider()).isNull();
    assertThat(display.getInvoices()).isNull();
  }
}
