package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.bean.validators.billing.BillingUndertakingValidator;
import uk.gov.laa.ccms.caab.bean.validators.declaration.PoaDeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClientException;
import uk.gov.laa.ccms.caab.config.StringToBillingContextConverter;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.Bills;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetail;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetails;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.service.BillingSummaryPdfService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoiceList;
import uk.gov.laa.ccms.data.model.TaxRateLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDataResponse;
import uk.gov.laa.ccms.soa.gateway.model.OpaAttribute;
import uk.gov.laa.ccms.soa.gateway.model.OpaEntity;
import uk.gov.laa.ccms.soa.gateway.model.OpaInstance;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submitted billing invoice selection through the real billing service")
class SubmittedBillingControllerTest {

  private static final String CASE_REF = "300000123";
  private static final byte[] PDF = "%PDF-1.4 selected invoice".getBytes(StandardCharsets.US_ASCII);

  @Mock private EbsApiClient ebsApiClient;
  @Mock private CaabApiClient caabApiClient;
  @Mock private SoaApiClient soaApiClient;
  @Mock private LookupService lookupService;
  @Mock private AssessmentService assessmentService;
  @Mock private SoaApplicationMapper soaApplicationMapper;
  @Mock private ClientService clientService;
  @Mock private AmendmentService amendmentService;
  @Mock private SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  @Mock private PoaDeclarationSubmissionValidator poaDeclarationValidator;
  @Mock private BillingSummaryPdfService pdfService;
  @Mock private BillingUndertakingValidator undertakingValidator;

  private MockMvcTester mockMvc;
  private StatementOfAccountDetails visibleStatements;
  private boolean draftsPresent = true;
  private final UserDetail user =
      new UserDetail()
          .loginId("user1")
          .userType("EXTERNAL")
          .provider(new BaseProvider().id(10).name("Current Firm"));

  @BeforeEach
  void setUp() {
    final BillingService billingService =
        new BillingService(
            ebsApiClient,
            caabApiClient,
            lookupService,
            soaApiClient,
            soaApplicationMapper,
            assessmentService,
            clientService);
    final BillingController controller =
        new BillingController(
            billingService,
            amendmentService,
            assessmentService,
            lookupService,
            submissionSummaryDisplayMapper,
            poaDeclarationValidator,
            pdfService,
            undertakingValidator);
    final DefaultFormattingConversionService conversionService =
        new DefaultFormattingConversionService();
    conversionService.addConverter(new StringToBillingContextConverter());
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(conversionService)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());

    // Both legacy and selected-item paths have valid responses; failures must be assertions,
    // not null reactive publishers or unused-stub errors on the as-yet-unimplemented path.
    lenient()
        .when(caabApiClient.getBill(any(), any()))
        .thenAnswer(
            invocation ->
                draftsPresent
                    ? Mono.just(new Bills().amount(new BigDecimal("999.00")))
                    : Mono.empty());
    lenient()
        .when(caabApiClient.getPaymentsOnAccount(any(), any()))
        .thenAnswer(
            invocation ->
                Mono.just(
                    new PaymentOnAccountDetails()
                        .content(
                            draftsPresent
                                ? List.of(
                                    new PaymentOnAccountDetail()
                                        .lscCaseReference(CASE_REF)
                                        .providerId("10"))
                                : List.of())));
    lenient().when(lookupService.getTaxRates()).thenReturn(Mono.just(new TaxRateLookupDetail()));
    lenient()
        .when(assessmentService.getAssessments(any(), any(), any()))
        .thenReturn(Mono.just(new AssessmentDetails().addContentItem(latestAssessment())));
    lenient()
        .when(ebsApiClient.getStatementOfAccount(anyString(), nullable(Long.class)))
        .thenAnswer(invocation -> Mono.just(visibleStatements));
    lenient()
        .when(soaApiClient.getInvoiceData(anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              final String billingId = invocation.getArgument(0);
              return switch (billingId) {
                case "555" -> Mono.just(invoiceData("120.00"));
                case "777" -> Mono.just(invoiceData("240.00"));
                default -> Mono.empty();
              };
            });
    lenient()
        .when(pdfService.generateBillSummary(any(), any(), any(), any(), any()))
        .thenReturn(PDF);
    lenient()
        .when(pdfService.generatePoaSummary(any(), any(), any(), any(), any()))
        .thenReturn(PDF);
    // Let accidental draft creation complete so the no-write assertion reports the real defect.
    lenient().when(caabApiClient.createBill(any(), any())).thenReturn(Mono.empty());
    lenient().when(caabApiClient.createPaymentOnAccount(any(), any())).thenReturn(Mono.empty());
    invoices("Bill", "Submitted");
  }

  private ApplicationDetail ebsCase() {
    return new ApplicationDetail()
        .caseReferenceNumber(CASE_REF)
        .providerDetails(
            new ApplicationProviderDetails()
                .provider(new IntDisplayValue().id(10).displayValue("Current Firm")))
        .availableFunctions(
            List.of(FunctionConstants.ADD_UPDATE_BILL, FunctionConstants.ADD_UPDATE_POA));
  }

  private void invoices(final String type, final String status) {
    visibleStatements =
        new StatementOfAccountDetails()
            .addContentItem(
                new StatementOfAccountDetail()
                    .entityType("Provider")
                    .firmId(10L)
                    .invoiceList(
                        new StatementOfAccountInvoiceList()
                            .addInvoiceItem(
                                new StatementOfAccountInvoice()
                                    .billingIncidentId(555L)
                                    .invoiceType(type)
                                    .invoiceStatus(status)
                                    .invoiceAmount(new BigDecimal("120.00")))
                            .addInvoiceItem(
                                new StatementOfAccountInvoice()
                                    .billingIncidentId(777L)
                                    .invoiceType(type)
                                    .invoiceStatus("Authorised")
                                    .invoiceAmount(new BigDecimal("240.00")))));
  }

  private AssessmentDetail latestAssessment() {
    return new AssessmentDetail()
        .status("complete")
        .addEntityTypesItem(
            new AssessmentEntityTypeDetail()
                .name("global")
                .addEntitiesItem(
                    new AssessmentEntityDetail()
                        .name(CASE_REF)
                        .addAttributesItem(
                            new AssessmentAttributeDetail()
                                .name("BILL_SUBMITTED_TOTAL_AMT")
                                .value("999.00"))));
  }

  private InvoiceDataResponse invoiceData(final String amount) {
    return new InvoiceDataResponse()
        .opaResponse(
            List.of(
                new OpaEntity()
                    .entityName("GLOBAL")
                    .instances(
                        List.of(
                            new OpaInstance()
                                .instanceLabel(CASE_REF)
                                .attributes(
                                    List.of(
                                        new OpaAttribute()
                                            .attribute("BILL_SUBMITTED_TOTAL_AMT")
                                            .responseType("number")
                                            .responseValue(amount),
                                        new OpaAttribute()
                                            .attribute("BILL_SUBMITTED_TOTAL_VAT")
                                            .responseType("number")
                                            .responseValue("0.00"))))),
                new OpaEntity()
                    .entityName("PROFIT_COST_BILL_LINE")
                    .instances(
                        List.of(
                            new OpaInstance()
                                .instanceLabel("invoice-line-" + amount)
                                .attributes(
                                    List.of(
                                        new OpaAttribute()
                                            .attribute("PROFIT_COST_TOTAL_CLAIM")
                                            .responseType("number")
                                            .responseValue(amount),
                                        new OpaAttribute()
                                            .attribute("PROFIT_COST_DATE")
                                            .responseType("date")
                                            .responseValue("01-01-2026"),
                                        new OpaAttribute()
                                            .attribute("PROFIT_COST_NET_CLAIM_INC_UPLFT")
                                            .responseType("number")
                                            .responseValue(amount),
                                        new OpaAttribute()
                                            .attribute("PROFIT_COST_VAT")
                                            .responseType("number")
                                            .responseValue("0.00")))))));
  }

  private void assertNoWrites() {
    assertThat(mockingDetails(caabApiClient).getInvocations())
        .extracting(invocation -> invocation.getMethod().getName())
        .allMatch(Set.of("getBill", "getPaymentsOnAccount")::contains);
    assertThat(mockingDetails(soaApiClient).getInvocations())
        .extracting(invocation -> invocation.getMethod().getName())
        .allMatch("getInvoiceData"::equals);
    assertThat(mockingDetails(assessmentService).getInvocations())
        .extracting(invocation -> invocation.getMethod().getName())
        .allMatch("getAssessments"::equals);
    verifyNoInteractions(clientService, soaApplicationMapper, amendmentService);
  }

  private void assertSelectedPdf(final String context, final String... amounts) {
    final ArgumentCaptor<AssessmentDetail> captor = ArgumentCaptor.forClass(AssessmentDetail.class);
    if (context.equals("bill")) {
      verify(pdfService, times(amounts.length))
          .generateBillSummary(any(), any(), captor.capture(), any(), any());
    } else {
      verify(pdfService, times(amounts.length))
          .generatePoaSummary(any(), any(), captor.capture(), any(), any());
    }
    for (int index = 0; index < amounts.length; index++) {
      final List<AssessmentAttributeDetail> attributes =
          captor.getAllValues().get(index).getEntityTypes().stream()
              .flatMap(type -> type.getEntities().stream())
              .flatMap(entity -> entity.getAttributes().stream())
              .toList();
      assertThat(attributes)
          .filteredOn(attribute -> "BILL_SUBMITTED_TOTAL_AMT".equals(attribute.getName()))
          .extracting(AssessmentAttributeDetail::getValue)
          .containsExactly(amounts[index]);
      assertThat(attributes)
          .filteredOn(attribute -> "PROFIT_COST_TOTAL_CLAIM".equals(attribute.getName()))
          .extracting(AssessmentAttributeDetail::getValue)
          .containsExactly(amounts[index]);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "bill, Bill, Submitted",
    "bill, Counsel Bill, Authorised",
    "bill, Bill, Rejected",
    "poa, POA, Submitted",
    "poa, Counsel POA, Authorised",
    "poa, POA, Rejected"
  })
  @DisplayName("Historical details retain the selected invoice status despite a newer assessment")
  void detailsSelectHistoricalInvoice(
      final String context, final String type, final String status) {
    invoices(type, status);
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context)
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/billing/" + context + "-details")
        .model()
        .containsEntry("readOnly", true)
        .containsEntry("billingId", "555")
        .containsEntry("assessmentStatus", status);
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({"bill, ''", "bill, /summary", "poa, ''", "poa, /summary"})
  @DisplayName("Invoice-data 404 renders unavailable without hiding the permitted invoice status")
  void invoiceNotFoundIsUnavailable(final String context, final String suffix) {
    invoices(context.equals("bill") ? "Bill" : "POA", "Authorised");
    lenient()
        .when(soaApiClient.getInvoiceData("555", "user1", "EXTERNAL"))
        .thenReturn(Mono.error(new SoaApiClientException("missing", HttpStatus.NOT_FOUND)));
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + suffix)
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/billing/" + context + "-details")
        .model()
        .containsEntry("readOnly", true)
        .containsEntry("detailsUnavailable", true)
        .containsEntry("assessmentStatus", "Authorised");
    verifyNoInteractions(pdfService);
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({"bill, Bill", "poa, POA"})
  @DisplayName(
      "A historical PDF uses the selected invoice's lines and totals, not the latest draft")
  void summaryUsesHistoricalInvoice(final String context, final String type) {
    invoices(type, "Submitted");
    final var result =
        mockMvc.perform(
            get("/case/billing/" + context + "/summary")
                .param("billing-id", "555")
                .sessionAttr(CASE, ebsCase())
                .sessionAttr(USER_DETAILS, user));

    assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_PDF);
    assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
        .startsWith("inline;")
        .contains(context + "-summary.pdf");
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PDF);
    verify(soaApiClient).getInvoiceData("555", "user1", "EXTERNAL");
    assertSelectedPdf(context, "120.00");
    verify(assessmentService, never()).getAssessments(any(), any(), any());
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({"bill, Bill", "poa, POA"})
  @DisplayName("Selecting two invoices in one session does not reuse stale summary data")
  void sessionDoesNotPinTheFirstInvoice(final String context, final String type) {
    invoices(type, "Submitted");
    final MockHttpSession session = new MockHttpSession();
    session.setAttribute(CASE, ebsCase());
    session.setAttribute(USER_DETAILS, user);
    for (final String billingId : List.of("555", "777")) {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/" + context + "/summary")
                      .param("billing-id", billingId)
                      .session(session)))
          .hasStatusOk()
          .hasContentType(MediaType.APPLICATION_PDF);
    }
    assertSelectedPdf(context, "120.00", "240.00");
    verify(soaApiClient).getInvoiceData("555", "user1", "EXTERNAL");
    verify(soaApiClient).getInvoiceData("777", "user1", "EXTERNAL");
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({"bill, Bill", "poa, POA"})
  @DisplayName("A submitted PDF remains available after drafts and assessments have been removed")
  void summarySurvivesSubmissionCleanup(final String context, final String type) {
    invoices(type, "Authorised");
    draftsPresent = false;
    lenient()
        .when(assessmentService.getAssessments(any(), any(), any()))
        .thenReturn(Mono.just(new AssessmentDetails()));
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + "/summary")
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasContentType(MediaType.APPLICATION_PDF);
    assertSelectedPdf(context, "120.00");
    assertNoWrites();
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("Viewing submitted details after cleanup must not create a replacement draft")
  void detailsDoNotRecreateDeletedDrafts(final String context) {
    invoices(context.equals("bill") ? "Bill" : "POA", "Submitted");
    draftsPresent = false;
    mockMvc.perform(
        get("/case/billing/" + context)
            .param("billing-id", "555")
            .sessionAttr(CASE, ebsCase())
            .sessionAttr(USER_DETAILS, user));
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({
    "bill, unknown, ''",
    "bill, unknown, /summary",
    "poa, unknown, ''",
    "poa, unknown, /summary",
    "bill, wrong-kind, ''",
    "bill, wrong-kind, /summary",
    "poa, wrong-kind, ''",
    "poa, wrong-kind, /summary",
    "bill, other-case, ''",
    "bill, other-case, /summary",
    "poa, other-case, ''",
    "poa, other-case, /summary",
    "bill, other-provider, ''",
    "bill, other-provider, /summary",
    "poa, other-provider, ''",
    "poa, other-provider, /summary"
  })
  @DisplayName("An out-of-scope invoice ID is unavailable before any unrestricted invoice read")
  void unavailableSelectionCannotReadOtherInvoice(
      final String context, final String scenario, final String suffix) {
    invoices(
        scenario.equals("wrong-kind")
            ? (context.equals("bill") ? "POA" : "Bill")
            : (context.equals("bill") ? "Bill" : "POA"),
        "Submitted");
    final ApplicationDetail selectedCase = ebsCase();
    UserDetail selectedUser = user;
    String billingId = "555";
    if (scenario.equals("unknown")) {
      billingId = "999";
    } else if (scenario.equals("other-case")) {
      selectedCase.setCaseReferenceNumber("300000999");
      visibleStatements = new StatementOfAccountDetails();
    } else if (scenario.equals("other-provider")) {
      selectedUser =
          new UserDetail()
              .loginId("other-user")
              .userType("EXTERNAL")
              .provider(new BaseProvider().id(77).name("Other Firm"));
      visibleStatements =
          new StatementOfAccountDetails()
              .addContentItem(new StatementOfAccountDetail().entityType("Provider").firmId(77L));
    }
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + suffix)
                    .param("billing-id", billingId)
                    .sessionAttr(CASE, selectedCase)
                    .sessionAttr(USER_DETAILS, selectedUser)))
        .hasStatusOk()
        .hasViewName("application/billing/" + context + "-details")
        .model()
        .containsEntry("readOnly", true)
        .containsEntry("detailsUnavailable", true)
        .doesNotContainEntry("assessmentStatus", "Submitted");
    if (scenario.equals("other-provider")) {
      verify(ebsApiClient, atLeastOnce()).getStatementOfAccount(CASE_REF, 77L);
    } else if (scenario.equals("other-case")) {
      verify(ebsApiClient, atLeastOnce()).getStatementOfAccount("300000999", null);
    }
    verifyNoInteractions(soaApiClient, pdfService);
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({
    "bill, missing, ''",
    "bill, missing, /summary",
    "poa, missing, ''",
    "poa, missing, /summary",
    "bill, no-opa, ''",
    "bill, no-opa, /summary",
    "poa, no-opa, ''",
    "poa, no-opa, /summary",
    "bill, empty-opa, ''",
    "bill, empty-opa, /summary",
    "poa, empty-opa, ''",
    "poa, empty-opa, /summary",
    "bill, missing-total, ''",
    "bill, missing-total, /summary",
    "poa, missing-total, ''",
    "poa, missing-total, /summary",
    "bill, missing-date, ''",
    "bill, missing-date, /summary",
    "poa, missing-date, ''",
    "poa, missing-date, /summary"
  })
  @DisplayName("Missing historical content is explicitly unavailable rather than a current PDF")
  void missingHistoricalContentIsUnavailable(
      final String context, final String missing, final String suffix) {
    invoices(context.equals("bill") ? "Bill" : "POA", "Authorised");
    final InvoiceDataResponse incomplete = invoiceData("120.00");
    if (missing.equals("missing-total")) {
      incomplete
          .getOpaResponse()
          .getFirst()
          .getInstances()
          .getFirst()
          .getAttributes()
          .getFirst()
          .setResponseValue(null);
    } else if (missing.equals("missing-date")) {
      incomplete
          .getOpaResponse()
          .get(1)
          .getInstances()
          .getFirst()
          .getAttributes()
          .get(1)
          .setResponseValue(null);
    }
    lenient()
        .when(soaApiClient.getInvoiceData("555", "user1", "EXTERNAL"))
        .thenReturn(
            switch (missing) {
              case "missing" -> Mono.empty();
              case "no-opa" -> Mono.just(new InvoiceDataResponse());
              case "empty-opa" -> Mono.just(new InvoiceDataResponse().opaResponse(List.of()));
              default -> Mono.just(incomplete);
            });
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + suffix)
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("application/billing/" + context + "-details")
        .model()
        .containsEntry("readOnly", true)
        .containsEntry("detailsUnavailable", true)
        .containsEntry("assessmentStatus", "Authorised");
    verifyNoInteractions(pdfService);
    assertNoWrites();
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("A real upstream failure is surfaced through the existing error handler")
  void upstreamFailureIsNotDisguisedAsMissingHistory(final String context) {
    invoices(context.equals("bill") ? "Bill" : "POA", "Submitted");
    lenient()
        .when(soaApiClient.getInvoiceData("555", "user1", "EXTERNAL"))
        .thenReturn(Mono.error(new CaabApplicationException("Invoice service unavailable")));
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + "/summary")
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasViewName("error")
        .model()
        .containsKey("error")
        .doesNotContainEntry("detailsUnavailable", true);
    verifyNoInteractions(pdfService);
    assertNoWrites();
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("Missing maintain permission prevents selected details and PDF retrieval")
  void permissionDenialPreventsReads(final String context) {
    final ApplicationDetail selectedCase = ebsCase().availableFunctions(List.of());
    for (final String suffix : List.of("", "/summary")) {
      assertThat(
              mockMvc.perform(
                  get("/case/billing/" + context + suffix)
                      .param("billing-id", "555")
                      .sessionAttr(CASE, selectedCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/billing");
    }
    verifyNoInteractions(ebsApiClient, caabApiClient, soaApiClient, pdfService);
    assertNoWrites();
  }

  @ParameterizedTest
  @CsvSource({"bill, Counsel Bill", "poa, Counsel POA"})
  @DisplayName("Current-provider users can still view other firms' invoices visible on their case")
  void currentProviderRetainsVisibleCounselInvoices(final String context, final String type) {
    invoices(type, "Authorised");
    visibleStatements.getContent().getFirst().setFirmId(55L);
    visibleStatements.getContent().getFirst().setEntityType("Counsel");
    assertThat(
            mockMvc.perform(
                get("/case/billing/" + context + "/summary")
                    .param("billing-id", "555")
                    .sessionAttr(CASE, ebsCase())
                    .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasContentType(MediaType.APPLICATION_PDF);
    verify(ebsApiClient, atLeastOnce()).getStatementOfAccount(CASE_REF, null);
    verify(soaApiClient).getInvoiceData("555", "user1", "EXTERNAL");
    assertSelectedPdf(context, "120.00");
    assertNoWrites();
  }
}
