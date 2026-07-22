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
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing service tests")
class BillingServiceTest {

  private static final String CASE_REF = "300000123456";

  @Mock EbsApiClient ebsApiClient;

  @InjectMocks BillingService billingService;

  private StatementOfAccountDetail statement(
      String entityType, Long firmId, BigDecimal billsAuthorised) {
    return new StatementOfAccountDetail()
        .entityType(entityType)
        .firmId(firmId)
        .bills(new StatementOfAccountBills().amountAuthorised(billsAuthorised));
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
                new ApplicationProviderDetails()
                    .provider(new IntDisplayValue().id(10).displayValue("Current Firm")));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .addInvoicesItem(
                        new StatementOfAccountInvoice()
                            .invoiceType("Bill")
                            .invoiceStatus("Authorised")
                            .invoiceAmount(new BigDecimal("100")))
                    .addInvoicesItem(
                        new StatementOfAccountInvoice().invoiceType("Bill").invoiceStatus("Draft")))
            .addContentItem(statement("Counsel", 55L, new BigDecimal("20")))
            .addContentItem(statement("Provider", 99L, new BigDecimal("30")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isUserBelongsToCurrentProvider()).isTrue();
    assertThat(display.getProviderFirmName()).isEqualTo("Current Firm");
    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("100");
    assertThat(display.getCounsel().getBillsAuthorised()).isEqualByComparingTo("20");
    assertThat(display.getPriorSolicitor().getBillsAuthorised()).isEqualByComparingTo("30");
    // EBS has no case-wide totals block, so the total column is the sum of the three columns.
    assertThat(display.getTotal().getBillsAuthorised()).isEqualByComparingTo("150");
    // The EBS draft invoice is stripped; only the authorised bill remains.
    assertThat(display.getInvoices()).hasSize(1);
    assertThat(display.getInvoices().get(0).getInvoiceStatus()).isEqualTo("Authorised");
  }

  @Test
  @DisplayName("When user does not belong to the case provider, only their statement is requested")
  void userNotOnCaseProvider() {
    UserDetail user =
        new UserDetail()
            .loginId("user1")
            .userType("EXTERNAL")
            .provider(new BaseProvider().id(77).name("Other Firm"));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Provider", 77L, new BigDecimal("50")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, 77L)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isUserBelongsToCurrentProvider()).isFalse();
    assertThat(display.getProviderFirmName()).isEqualTo("Other Firm");
    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("50");
    assertThat(display.getCounsel().getBillsAuthorised()).isNull();
    assertThat(display.getPriorSolicitor().getBillsAuthorised()).isNull();
    assertThat(display.getTotal().getBillsAuthorised()).isEqualByComparingTo("50");
  }

  @Test
  @DisplayName("Returns an empty display when the statement of account response is null")
  void nullResponse() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber(CASE_REF);

    when(ebsApiClient.getStatementOfAccount(CASE_REF, 10L)).thenReturn(Mono.empty());

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getProvider()).isNull();
    assertThat(display.getInvoices()).isNull();
  }

  @Test
  @DisplayName("Shows zeros, not blanks, when the case has no statement for the provider")
  void noProviderStatement() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Counsel", 55L, new BigDecimal("20")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("0");
    assertThat(display.getProvider().getUndertaking()).isEqualByComparingTo("0");
    // A firm with no statement contributes nothing, so the counsel column stays blank.
    assertThat(display.getPriorSolicitor().getBillsAuthorised()).isNull();
  }

  @Test
  @DisplayName("Lists the provider's invoices ahead of the other firms'")
  void providerInvoicesFirst() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(
                statement("Counsel", 55L, new BigDecimal("20"))
                    .addInvoicesItem(new StatementOfAccountInvoice().invoiceType("Counsel Bill")))
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .addInvoicesItem(new StatementOfAccountInvoice().invoiceType("Bill")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getInvoices())
        .extracting(StatementOfAccountInvoice::getInvoiceType)
        .containsExactly("Bill", "Counsel Bill");
  }

  @Test
  @DisplayName("An absent undertaking shows as zero, as the legacy PUI does")
  void absentUndertakingShowsAsZero() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    // EBS returns no undertaking and no cost limitation for this firm.
    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Provider", 10L, new BigDecimal("100")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getProvider().getUndertaking()).isEqualByComparingTo("0");
    assertThat(display.getProvider().getPoaRecouped()).isEqualByComparingTo("0");
    // The cost limitation block is optional on a statement, so it stays blank.
    assertThat(display.getProvider().getCertificateCostLimitation()).isNull();
  }
}
