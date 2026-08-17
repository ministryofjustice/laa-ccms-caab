package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
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
import uk.gov.laa.ccms.data.model.TaxRateLookupDetail;
import uk.gov.laa.ccms.data.model.TaxRateLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing service tests")
class BillingServiceTest {

  private static final String CASE_REF = "300000123456";

  @Mock EbsApiClient ebsApiClient;

  @Mock CaabApiClient caabApiClient;

  @Mock LookupService lookupService;

  @Mock SoaApiClient soaApiClient;

  @Mock uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper soaApplicationMapper;

  @InjectMocks BillingService billingService;

  @BeforeEach
  void noDraftsByDefault() {
    // Unless a test says otherwise, the provider has no draft bill or payments on account.
    lenient().when(caabApiClient.getBill(any(), any())).thenReturn(Mono.empty());
    lenient()
        .when(caabApiClient.getPaymentsOnAccount(any(), any()))
        .thenReturn(Mono.just(new PaymentOnAccountDetails()));
    // Tax rates are only fetched when a draft POA exists; default to none.
    lenient().when(lookupService.getTaxRates()).thenReturn(Mono.just(new TaxRateLookupDetail()));
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
  @DisplayName("Orders submitted invoices by date submitted (newest first), tie-broken by id")
  void ordersSubmittedInvoicesByDateDescending() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    // GMT dates, so the legacy display shift does not affect the ordering under test.
    StatementOfAccountDetails response =
        new StatementOfAccountDetails()
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .billingIncidentId(9L)
                                .invoiceType("Older")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2020, 1, 15, 0, 0)),
                            new StatementOfAccountInvoice()
                                .billingIncidentId(2L)
                                .invoiceType("NewerB")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2021, 2, 10, 0, 0)),
                            new StatementOfAccountInvoice()
                                .billingIncidentId(1L)
                                .invoiceType("NewerA")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2021, 2, 10, 0, 0)))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // The 2021 rows come first; the equal-dated pair is ordered by ascending billing incident id.
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type)
        .containsExactly("NewerA", "NewerB", "Older");
  }

  @Test
  @DisplayName("Renders EBS dates in UTC for legacy parity, shifting BST midnight back a day")
  void shiftsBstDatesForLegacyParity() {
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
                statement("Provider", 10L, null)
                    .invoiceList(
                        invoiceList(
                            // British Summer Time midnight -> shows the previous day.
                            new StatementOfAccountInvoice()
                                .invoiceType("POA")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2018, 6, 5, 0, 0)),
                            // GMT midnight -> unchanged.
                            new StatementOfAccountInvoice()
                                .invoiceType("POA")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2017, 11, 20, 0, 0)),
                            // British Summer Time but not midnight -> unchanged date.
                            new StatementOfAccountInvoice()
                                .invoiceType("POA")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2018, 6, 8, 14, 30)))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // Ordered newest first by (raw) date submitted, each shown with the legacy UTC shift applied.
    assertThat(display.getBillsAndPoa())
        .extracting(row -> row.dateSubmitted().toLocalDate().toString())
        .containsExactly("2018-06-08", "2018-06-04", "2017-11-20");
  }

  @Test
  @DisplayName("Pins the provider's drafts to the top, ahead of every firm's submitted invoices")
  void draftsPinnedFirst() {
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
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2023, 1, 10, 0, 0)))))
            .addContentItem(
                statement("Provider", 10L, new BigDecimal("100"))
                    .invoiceList(
                        invoiceList(
                            new StatementOfAccountInvoice()
                                .invoiceType("Bill")
                                .invoiceStatus("Authorised")
                                .dateSubmitted(LocalDateTime.of(2022, 1, 10, 0, 0)))));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    when(caabApiClient.getBill(CASE_REF, "10"))
        .thenReturn(Mono.just(new Bills().amount(new BigDecimal("300"))));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    // The draft heads the table; the submitted invoices follow, newest first regardless of firm.
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type, BillPoaRow::status)
        .containsExactly(
            tuple("Bill", "Draft"),
            tuple("Counsel Bill", "Authorised"),
            tuple("Bill", "Authorised"));
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
    // Draft POA of net 200 whose VAT code "STD" resolves to 20% grosses up to 240.00; draft bill
    // 300.
    when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
        .thenReturn(
            Mono.just(
                new PaymentOnAccountDetails()
                    .addContentItem(
                        new PaymentOnAccountDetail()
                            .actualNetCost(new BigDecimal("200"))
                            .vatRate("STD"))));
    when(caabApiClient.getBill(CASE_REF, "10"))
        .thenReturn(Mono.just(new Bills().amount(new BigDecimal("300"))));
    when(lookupService.getTaxRates())
        .thenReturn(
            Mono.just(
                new TaxRateLookupDetail()
                    .addContentItem(new TaxRateLookupValueDetail().code("STD").taxRate("20"))));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.isDraftPoaExists()).isTrue();
    assertThat(display.isDraftBillExists()).isTrue();
    // The drafts head the table (POA then bill), then the submitted invoice.
    assertThat(display.getBillsAndPoa())
        .extracting(BillPoaRow::type, BillPoaRow::status)
        .containsExactly(
            tuple("POA", "Draft"), tuple("Bill", "Draft"), tuple("Bill", "Authorised"));
    assertThat(display.getBillsAndPoa().get(0).amount()).isEqualByComparingTo("240.00");
    assertThat(display.getBillsAndPoa().get(1).amount()).isEqualByComparingTo("300");
  }

  @Test
  @DisplayName("An unknown VAT code contributes no VAT; a load failure leaves the POA net only")
  void poaVatFallbacks() {
    UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));
    ApplicationDetail ebsCase =
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .providerDetails(
                new ApplicationProviderDetails().provider(new IntDisplayValue().id(10)));

    StatementOfAccountDetails response =
        new StatementOfAccountDetails().addContentItem(statement("Provider", 10L, null));

    when(ebsApiClient.getStatementOfAccount(CASE_REF, null)).thenReturn(Mono.just(response));
    when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
        .thenReturn(
            Mono.just(
                new PaymentOnAccountDetails()
                    .addContentItem(
                        new PaymentOnAccountDetail()
                            .actualNetCost(new BigDecimal("200"))
                            .vatRate("UNKNOWN"))));
    // The tax-rate lookup succeeds but has no matching code, so no VAT is applied.
    when(lookupService.getTaxRates())
        .thenReturn(
            Mono.just(
                new TaxRateLookupDetail()
                    .addContentItem(new TaxRateLookupValueDetail().code("STD").taxRate("20"))));

    StatementOfAccountDisplay display =
        billingService.getStatementOfAccountDisplay(CASE_REF, ebsCase, user);

    assertThat(display.getBillsAndPoa())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.type()).isEqualTo("POA");
              assertThat(row.amount()).isEqualByComparingTo("200.00");
            });
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

  @Nested
  @DisplayName("Draft payment on account maintenance")
  class DraftPaymentOnAccount {

    private final UserDetail user =
        new UserDetail().loginId("user1").userType("EXTERNAL").provider(new BaseProvider().id(10));

    @Test
    @DisplayName("Creates a draft POA carrying only the case and provider when there is none")
    void createsDraftWhenAbsent() {
      when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
          .thenReturn(Mono.just(new PaymentOnAccountDetails()))
          .thenReturn(
              Mono.just(
                  new PaymentOnAccountDetails()
                      .addContentItem(new PaymentOnAccountDetail().id(7))));
      when(caabApiClient.createPaymentOnAccount(any(), eq("user1"))).thenReturn(Mono.just("7"));

      PaymentOnAccountDetail created =
          billingService.createDraftPaymentOnAccountIfAbsent(CASE_REF, "10", user);

      assertThat(created.getId()).isEqualTo(7);

      ArgumentCaptor<PaymentOnAccountDetail> captor =
          ArgumentCaptor.forClass(PaymentOnAccountDetail.class);
      verify(caabApiClient).createPaymentOnAccount(captor.capture(), eq("user1"));
      // Every other field is filled in by the OPA interview, as the legacy PUI does.
      assertThat(captor.getValue().getLscCaseReference()).isEqualTo(CASE_REF);
      assertThat(captor.getValue().getProviderId()).isEqualTo("10");
      assertThat(captor.getValue().getActualNetCost()).isNull();
    }

    @Test
    @DisplayName("Re-entering the POA screen edits the existing draft rather than adding another")
    void isIdempotentWhenDraftExists() {
      PaymentOnAccountDetail existing = new PaymentOnAccountDetail().id(3);
      when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
          .thenReturn(Mono.just(new PaymentOnAccountDetails().addContentItem(existing)));

      assertThat(billingService.createDraftPaymentOnAccountIfAbsent(CASE_REF, "10", user))
          .isSameAs(existing);

      verify(caabApiClient, never()).createPaymentOnAccount(any(), any());
    }

    @Test
    @DisplayName("Deletes every draft POA the provider holds for the case")
    void deletesDrafts() {
      when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
          .thenReturn(
              Mono.just(
                  new PaymentOnAccountDetails()
                      .addContentItem(new PaymentOnAccountDetail().id(3))
                      .addContentItem(new PaymentOnAccountDetail().id(4))));
      when(caabApiClient.removePaymentOnAccount(any(), any())).thenReturn(Mono.empty());

      billingService.deleteDraftPaymentsOnAccount(CASE_REF, "10", user);

      verify(caabApiClient).removePaymentOnAccount(3L, "user1");
      verify(caabApiClient).removePaymentOnAccount(4L, "user1");
    }
  }

  @Nested
  @DisplayName("Allocated cost limit")
  class AllocatedCostLimit {

    @Test
    @DisplayName("Uses the provider statement's certificate cost limitation when there is one")
    void usesProviderCertificateCostLimitation() {
      StatementOfAccountDisplay display = new StatementOfAccountDisplay();
      SoaFigureColumn provider = new SoaFigureColumn();
      provider.setCertificateCostLimitation(new BigDecimal("2500.00"));
      display.setProvider(provider);

      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .costs(new CostStructureDetail().grantedCostLimitation(new BigDecimal("999.00")));

      assertThat(billingService.getAllocatedCostLimit(display, ebsCase))
          .isEqualByComparingTo("2500.00");
    }

    @Test
    @DisplayName("Falls back to the case's granted cost limitation when EBS holds no statement")
    void fallsBackToGrantedCostLimitation() {
      ApplicationDetail ebsCase =
          new ApplicationDetail()
              .costs(new CostStructureDetail().grantedCostLimitation(new BigDecimal("999.00")));

      assertThat(billingService.getAllocatedCostLimit(new StatementOfAccountDisplay(), ebsCase))
          .isEqualByComparingTo("999.00");
    }

    @Test
    @DisplayName("Is zero when neither a statement nor a granted cost limitation is available")
    void isZeroWhenNothingAvailable() {
      assertThat(
              billingService.getAllocatedCostLimit(
                  new StatementOfAccountDisplay(), new ApplicationDetail()))
          .isEqualByComparingTo("0");
    }
  }

  @Nested
  @DisplayName("Submit payment on account")
  class SubmitPaymentOnAccount {

    private UserDetail user() {
      return new UserDetail()
          .loginId("user1")
          .userType("EXTERNAL")
          .provider(new BaseProvider().id(10));
    }

    @Test
    @DisplayName("Maps the draft and assessment onto the invoice, submits it and deletes the draft")
    void submitsAndDeletesDraft() {
      final PaymentOnAccountDetail draft =
          new PaymentOnAccountDetail()
              .id(5)
              .lscCaseReference(CASE_REF)
              .providerId("10")
              .reason("Reason")
              .courtType("CT")
              .actualNetCost(new BigDecimal("100"))
              .vatRate("1")
              .notes("Notes");
      when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
          .thenReturn(Mono.just(new PaymentOnAccountDetails().addContentItem(draft)));
      when(lookupService.getTaxRates())
          .thenReturn(
              Mono.just(
                  new TaxRateLookupDetail()
                      .addContentItem(new TaxRateLookupValueDetail().code("1").taxRate("20"))));
      final AssessmentDetail assessment = new AssessmentDetail();
      when(soaApplicationMapper.mapAssessment(
              assessment, AssessmentRulebase.POA.getGoalAttributeName()))
          .thenReturn(List.of(new uk.gov.laa.ccms.soa.gateway.model.AssessmentResult()));
      when(soaApiClient.createInvoice(any(), eq("user1"), eq("EXTERNAL")))
          .thenReturn(Mono.just(new InvoiceResponse().invoiceReferenceId("INV-1")));
      when(caabApiClient.removePaymentOnAccount(eq(5L), eq("user1"))).thenReturn(Mono.empty());

      final String reference =
          billingService.submitPaymentOnAccount(CASE_REF, "10", assessment, user());

      assertThat(reference).isEqualTo("INV-1");

      final ArgumentCaptor<InvoiceDetail> invoiceCaptor =
          ArgumentCaptor.forClass(InvoiceDetail.class);
      verify(soaApiClient).createInvoice(invoiceCaptor.capture(), eq("user1"), eq("EXTERNAL"));
      final uk.gov.laa.ccms.soa.gateway.model.PaymentOnAccountDetail poa =
          invoiceCaptor.getValue().getPoa();
      assertThat(poa.getProviderId()).isEqualTo("10");
      assertThat(poa.getCaseReferenceNumber()).isEqualTo(CASE_REF);
      assertThat(poa.getReason()).isEqualTo("Reason");
      assertThat(poa.getCourtType()).isEqualTo("CT");
      assertThat(poa.getActualNetCost()).isEqualByComparingTo("100");
      assertThat(poa.getVatRate()).isEqualTo("1");
      assertThat(poa.getNotes()).isEqualTo("Notes");
      // 100 net grossed up by the 20% VAT the rate code resolves to.
      assertThat(poa.getActualTotalCost()).isEqualByComparingTo("120.00");
      // The legacy PUI never populates the calculated net cost, so it is left unset.
      assertThat(poa.getCalculatedNetCost()).isNull();
      assertThat(poa.getOpaResponse()).isNotNull();

      // The submitted draft is removed.
      verify(caabApiClient).removePaymentOnAccount(5L, "user1");
    }

    @Test
    @DisplayName("Throws when there is no draft payment on account to submit")
    void throwsWhenNoDraft() {
      when(caabApiClient.getPaymentsOnAccount(CASE_REF, "10"))
          .thenReturn(Mono.just(new PaymentOnAccountDetails()));

      assertThatThrownBy(
              () ->
                  billingService.submitPaymentOnAccount(
                      CASE_REF, "10", new AssessmentDetail(), user()))
          .isInstanceOf(CaabApplicationException.class);

      verifyNoInteractions(soaApiClient);
    }
  }
}
