package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.Bills;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetails;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoiceList;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing service tests")
class BillingServiceTest {

  private static final String CASE_REF = "300000123456";

  @Mock EbsApiClient ebsApiClient;

  @Mock CaabApiClient caabApiClient;

  @InjectMocks BillingService billingService;

  @BeforeEach
  void noDraftsByDefault() {
    // Unless a test says otherwise, the provider has no draft bill or payments on account.
    lenient().when(caabApiClient.getBill(any(), any())).thenReturn(Mono.empty());
    lenient()
        .when(caabApiClient.getPaymentsOnAccount(any(), any()))
        .thenReturn(Mono.just(new PaymentOnAccountDetails()));
  }

  private StatementOfAccountDetail statement(
      String entityType, Long firmId, BigDecimal billsAuthorised) {
    return new StatementOfAccountDetail()
        .entityType(entityType)
        .firmId(firmId)
        .bills(new StatementOfAccountBills().amountAuthorised(billsAuthorised));
  }

  private StatementOfAccountInvoiceList invoiceList(final StatementOfAccountInvoice... invoices) {
    final StatementOfAccountInvoiceList list = new StatementOfAccountInvoiceList();
    for (final StatementOfAccountInvoice invoice : invoices) {
      list.addInvoiceItem(invoice);
    }
    return list;
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
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Authorised")
                                .invoiceAmount(new BigDecimal("100")),
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Draft"))))
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
    assertThat(display.getBillsAndPoa()).hasSize(1);
    assertThat(display.getBillsAndPoa().get(0).status()).isEqualTo("Authorised");
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
    assertThat(display.getBillsAndPoa()).isNull();
  }

  @Test
  @DisplayName(
      "Fetches and shows nothing when the user has no provider and is not the case provider")
  void userWithoutProvider() {
    // With no provider there is nothing to scope the query to. Querying unrestricted would return
    // every firm's statement and invoices, so no query is made and nothing is shown — another
    // firm's billing data must never be exposed.
    UserDetail user = new UserDetail().loginId("user1").userType("EXTERNAL");
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isUserBelongsToCurrentProvider()).isFalse();
    assertThat(display.getProvider()).isNull();
    assertThat(display.getBillsAndPoa()).isNull();
    verifyNoInteractions(ebsApiClient);
    verifyNoInteractions(caabApiClient);
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
    // No prior solicitor statements were summed, so the prior solicitor column stays blank.
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
                    .invoiceList(
                        invoiceList(new StatementOfAccountInvoice().invoiceType("Counsel Bill"))))
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(invoiceList(new StatementOfAccountInvoice().invoiceType("Bill"))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type)
        .containsExactly("Bill", "Counsel Bill");
  }

  @Test
  @DisplayName("Groups the provider's drafts with its own invoices, ahead of other firms'")
  void draftsGroupedWithProviderBlock() {
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
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Counsel Bill")
                                .invoiceStatus("Authorised"))))
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Authorised"))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    when(caabApiClient.getBill(CASE_REF, "10"))
        .thenReturn(Mono.just(new Bills().amount(new BigDecimal("300"))));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // Provider's submitted invoice, then the provider's draft, then the counsel firm's invoice.
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type, BillPoaRow::status)
        .containsExactly(
            tuple("Bill", "Authorised"),
            tuple("Bill", "Draft"),
            tuple("Counsel Bill", "Authorised"));
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

  @Test
  @DisplayName("Derives the counsel cost ceiling and remaining from the case's counsel cost limits")
  void derivesCounselCostCeiling() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)))
            .costs(
                new CostStructureDetail()
                    .costEntries(
                        List.of(
                            // Two counsel limits are summed (casing is ignored)...
                            new CostEntryDetail()
                                .costCategory("Counsel")
                                .requestedCosts(new BigDecimal("1000"))
                                .amountBilled(new BigDecimal("400")),
                            new CostEntryDetail()
                                .costCategory("COUNSEL")
                                .requestedCosts(new BigDecimal("500"))
                                .amountBilled(BigDecimal.ZERO),
                            // ...and a non-counsel limit is excluded.
                            new CostEntryDetail()
                                .costCategory("PROFIT")
                                .requestedCosts(new BigDecimal("9999"))
                                .amountBilled(BigDecimal.ZERO))));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Provider", 10L, new BigDecimal("100")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // Ceiling = 1000 + 500; remaining = (1000 - 400) + (500 - 0).
    assertThat(display.getCounselCostCeiling()).isEqualByComparingTo("1500");
    assertThat(display.getCounselCostCeilingRemaining()).isEqualByComparingTo("1100");
  }

  @Test
  @DisplayName("Counsel cost ceiling is zero when the case carries no counsel cost limits")
  void counselCostCeilingZeroWhenNoCounselCosts() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Provider", 10L, new BigDecimal("100")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getCounselCostCeiling()).isEqualByComparingTo("0");
    assertThat(display.getCounselCostCeilingRemaining()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Adds the provider's draft bill and draft payments on account as draft rows")
  void addsDraftBillAndPoaRows() {
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
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Authorised"))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    // Draft POA of net 200 at 17.5% VAT grosses up to 235.00; draft bill of 300.
    when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
        .thenReturn(
            Mono.just(
                new PaymentOnAccountDetails()
                    .addContentItem(
                        new PaymentOnAccountDetail()
                            .actualNetCost(new BigDecimal("200"))
                            .vatRate("17.5"))));
    when(caabApiClient.getBill(CASE_REF, "10"))
        .thenReturn(Mono.just(new Bills().amount(new BigDecimal("300"))));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isDraftPoaExists()).isTrue();
    assertThat(display.isDraftBillExists()).isTrue();
    // The submitted invoice heads the table, then the draft POA, then the draft bill.
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type, BillPoaRow::status)
        .containsExactly(
            tuple("Bill", "Authorised"), tuple("POA", "Draft"), tuple("Bill", "Draft"));
    assertThat(display.getBillsAndPoa().get(1).amount()).isEqualByComparingTo("235.00");
    assertThat(display.getBillsAndPoa().get(2).amount()).isEqualByComparingTo("300");
  }

  @Test
  @DisplayName("A draft fetch failure does not break the statement of account")
  void draftFetchFailureIsTolerated() {
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
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Authorised"))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    // The draft store is unavailable (e.g. a 500 from the CAAB API).
    when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
        .thenReturn(Mono.error(new RuntimeException("boom")));
    when(caabApiClient.getBill(CASE_REF, "10"))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // The EBS statement still renders; the drafts are simply absent.
    assertThat(display.getProvider().getBillsAuthorised()).isEqualByComparingTo("100");
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::status)
        .containsExactly("Authorised");
    assertThat(display.isDraftPoaExists()).isFalse();
    assertThat(display.isDraftBillExists()).isFalse();
  }

  @Test
  @DisplayName("A draft bill with no amount yet is still shown and hides Create Bill")
  void draftBillWithoutAmountStillShown() {
    // An in-progress draft may not have an amount entered yet. The CAAB API returns it (a 404 would
    // mean no draft), so it must be shown with a blank amount, mirroring the legacy PUI.
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(statement("Provider", 10L, new BigDecimal("100")));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    when(caabApiClient.getBill(CASE_REF, "10")).thenReturn(Mono.just(new Bills()));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isDraftBillExists()).isTrue();
    assertThat(display.getBillsAndPoa())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.type()).isEqualTo("Bill");
              assertThat(row.status()).isEqualTo("Draft");
              assertThat(row.amount()).isNull();
            });
  }
}
