package uk.gov.laa.ccms.caab.controller.billing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.validators.billing.BillingUndertakingValidator;
import uk.gov.laa.ccms.caab.bean.validators.declaration.PoaDeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.config.StringToBillingContextConverter;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.PaymentOnAccountDetails;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.BillingService;
import uk.gov.laa.ccms.caab.service.BillingSummaryPdfService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.StatementOfAccountBills;
import uk.gov.laa.ccms.data.model.StatementOfAccountCostLimitation;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetail;
import uk.gov.laa.ccms.data.model.StatementOfAccountDetails;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoice;
import uk.gov.laa.ccms.data.model.StatementOfAccountInvoiceList;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.InvoiceDataResponse;
import uk.gov.laa.ccms.soa.gateway.model.OpaAttribute;
import uk.gov.laa.ccms.soa.gateway.model.OpaEntity;
import uk.gov.laa.ccms.soa.gateway.model.OpaInstance;

@ExtendWith(MockitoExtension.class)
class SubmittedBillingDataApiIntegrationTest extends AbstractIntegrationTest {

  private static final String CASE_REF = "300000123";

  @RegisterExtension
  static WireMockExtension data =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.ebs-api.port", data::getPort);
  }

  @Autowired private EbsApiClient ebsApiClient;
  @Mock private CaabApiClient caab;
  @Mock private SoaApiClient soa;
  @Mock private LookupService lookups;
  @Mock private AssessmentService assessments;
  @Mock private SoaApplicationMapper mapper;
  @Mock private ClientService clients;
  @Mock private AmendmentService amendments;
  @Mock private SubmissionSummaryDisplayMapper submissionMapper;
  @Mock private PoaDeclarationSubmissionValidator declarations;
  @Mock private BillingUndertakingValidator undertakings;

  private MockMvc mvc;
  private MockHttpSession session;
  private Set<String> requestedAssessmentPaths;

  @BeforeEach
  void setup() throws Exception {
    requestedAssessmentPaths = new HashSet<>();
    lenient().when(caab.getBill(any(), any())).thenReturn(Mono.empty());
    lenient()
        .when(caab.getPaymentsOnAccount(any(), any()))
        .thenReturn(Mono.just(new PaymentOnAccountDetails()));
    // The old source can complete; wrong-source failures must be assertions, not null publishers.
    lenient()
        .when(soa.getInvoiceData(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(legacyInvoice()));
    BillingService billing =
        new BillingService(ebsApiClient, caab, lookups, soa, mapper, assessments, clients);
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    BillingController controller =
        new BillingController(
            billing,
            amendments,
            assessments,
            lookups,
            submissionMapper,
            declarations,
            new BillingSummaryPdfService(engine),
            undertakings);
    DefaultFormattingConversionService conversion = new DefaultFormattingConversionService();
    conversion.addConverter(new StringToBillingContextConverter());
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setConversionService(conversion)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    session = new MockHttpSession();
    session.setAttribute(
        CASE,
        new ApplicationDetail()
            .caseReferenceNumber(CASE_REF)
            .status(new StringDisplayValue().displayValue("Current case status"))
            .providerDetails(
                new ApplicationProviderDetails()
                    .provider(new IntDisplayValue().id(10).displayValue("Current Firm")))
            .availableFunctions(
                List.of(FunctionConstants.ADD_UPDATE_BILL, FunctionConstants.ADD_UPDATE_POA)));
    session.setAttribute(
        USER_DETAILS,
        new UserDetail()
            .loginId("synthetic-user")
            .userType("EXTERNAL")
            .provider(new BaseProvider().id(10).name("Current Firm")));
    statement("Solicitor Final", "Submitted");
    stub(invoice(555, "Solicitor Final", "100.00", "20.00"));
    stub(invoice(777, "Solicitor Final", "200.00", "40.00"));
  }

  @AfterEach
  void noWritesOrAssessmentLifecycle() {
    assertThat(mockingDetails(caab).getInvocations())
        .allMatch(i -> Set.of("getBill", "getPaymentsOnAccount").contains(i.getMethod().getName()));
    verifyNoInteractions(assessments, mapper, clients, amendments);
    assertThat(data.findAll(anyRequestedFor(anyUrl())))
        .as("only statement and selected-invoice GET requests are allowed")
        .allSatisfy(
            request -> {
              assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
              assertThat(
                      request.getUrl().matches("/statementofaccount(?:\\?.*)?")
                          || requestedAssessmentPaths.contains(request.getUrl()))
                  .as("unexpected Data API request %s", request.getUrl())
                  .isTrue();
            });
  }

  private String endpoint(long id) {
    return "/cases/" + CASE_REF + "/invoices/" + id + "/assessment";
  }

  private void stub(JSONObject body) {
    List<Long> attributeIds = new ArrayList<>();
    for (Object item : body.getJSONArray("instances")) {
      for (Object raw : ((JSONObject) item).getJSONArray("attributes")) {
        attributeIds.add(((JSONObject) raw).getLong("id"));
      }
    }
    assertThat(attributeIds).as("synthetic assessment attribute IDs").doesNotHaveDuplicates();
    data.stubFor(
        get(urlPathEqualTo(endpoint(body.getLong("billingIncidentId"))))
            .willReturn(okJson(body.toString())));
  }

  private void statement(String type, String status) throws Exception {
    StatementOfAccountDetails body = new StatementOfAccountDetails();
    for (int firm : List.of(10, 20)) {
      body.addContentItem(
          new StatementOfAccountDetail()
              .entityType("Provider")
              .firmId((long) firm)
              .firmName(firm == 10 ? "Current Firm" : "Previous Firm")
              .costLimitation(
                  new StatementOfAccountCostLimitation()
                      .certificateAmount(new BigDecimal("2000.00"))
                      .remainingAmount(new BigDecimal("1500.00")))
              .bills(new StatementOfAccountBills().amountAuthorised(new BigDecimal("500.00")))
              .invoiceList(
                  new StatementOfAccountInvoiceList()
                      .addInvoiceItem(
                          new StatementOfAccountInvoice()
                              .billingIncidentId(firm == 10 ? 555L : 777L)
                              .invoiceType(type)
                              .invoiceStatus(status)
                              .invoiceAmount(new BigDecimal(firm == 10 ? "120.00" : "240.00")))));
    }
    data.stubFor(
        get(urlPathEqualTo("/statementofaccount"))
            .willReturn(okJson(new ObjectMapper().writeValueAsString(body))));
  }

  private MvcResult request(String context, String suffix, String id) throws Exception {
    requestedAssessmentPaths.add(
        "/cases/"
            + ((ApplicationDetail) session.getAttribute(CASE)).getCaseReferenceNumber()
            + "/invoices/"
            + id
            + "/assessment");
    return mvc.perform(
            MockMvcRequestBuilders.get("/case/billing/" + context + suffix)
                .param("billing-id", id)
                .session(session))
        .andReturn();
  }

  private void selectedRead(long... ids) {
    List<String> paths = new ArrayList<>();
    for (long id : ids) {
      paths.add(endpoint(id));
      data.verify(
          1,
          getRequestedFor(urlPathEqualTo(endpoint(id)))
              .withHeader("Authorization", matching(".+")));
    }
    assertThat(data.findAll(getRequestedFor(urlPathMatching("/cases/.*/invoices/.*/assessment"))))
        .extracting(request -> request.getUrl())
        .containsExactlyInAnyOrderElementsOf(paths);
    verifyNoInteractions(soa);
  }

  private void unavailable(MvcResult result, String context, String status) {
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getViewName())
        .isEqualTo("application/billing/" + context + "-details");
    assertThat(result.getModelAndView().getModel())
        .containsEntry("readOnly", true)
        .containsEntry("detailsUnavailable", true)
        .containsEntry("assessmentStatus", status);
    assertThat(result.getResponse().getContentType()).isNotEqualTo("application/pdf");
  }

  private String pdf(MvcResult result, String context, String net, String gross, String hours)
      throws Exception {
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
    assertThat(result.getResponse().getHeader("Content-Disposition"))
        .startsWith("inline;")
        .contains(context + "-summary.pdf");
    PdfReader reader = new PdfReader(result.getResponse().getContentAsByteArray());
    try {
      PdfTextExtractor extractor = new PdfTextExtractor(reader);
      StringBuilder text = new StringBuilder();
      for (int page = 1; page <= reader.getNumberOfPages(); page++) {
        text.append(extractor.getTextFromPage(page));
      }
      String compact = text.toString().replaceAll("\\s+", "");
      assertThat(compact)
          .contains(context.equals("poa") ? "POAREPORT" : "BILLREPORT")
          .contains("TotalNetClaim(\u00a3)" + net)
          .contains("TotalClaim(incVAT&Uplift\u00a3)" + gross)
          .contains("TotalHoursMinutesClaimed" + hours)
          .contains("CurrentFirm", "Currentcasestatus", "2,000.00", "1,500.00", "500.00");
      return compact;
    } finally {
      reader.close();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "bill, Solicitor Final, Submitted",
    "bill, Counsel Final, Under Assessment",
    "bill, Solicitor Interim, Assessed",
    "bill, Solicitor Final, Paid",
    "bill, Solicitor Adjustment, Zero Paid",
    "bill, Solicitor Appeal, QC and Approval",
    "poa, Solicitor POA, Submitted",
    "poa, Counsel POA, Authorised",
    "poa, Solicitor POA, Rejected",
    "poa, Solicitor POA, Paid (Unrecouped)"
  })
  void selectedDetailsUseTheNewSourceWithoutDrafts(String context, String type, String status)
      throws Exception {
    statement(type, status);
    stub(invoice(555, type, "100.00", "20.00"));
    MvcResult result = request(context, "", "555");
    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getModel())
        .containsEntry("readOnly", true)
        .containsEntry("assessmentStatus", status)
        .containsEntry("detailsUnavailable", false)
        .containsEntry("billingId", "555");
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  void selectedPdfsUseDistinctInvoicesInOneSessionIncludingThePreviousFirm(String context)
      throws Exception {
    String type = context.equals("poa") ? "Counsel POA" : "Solicitor Final";
    statement(type, "Assessed");
    stub(invoice(555, type, "100.00", "20.00"));
    stub(invoice(777, type, "200.00", "40.00"));
    String first = pdf(request(context, "/summary", "555"), context, "100.00", "120.00", "00:00");
    String second = pdf(request(context, "/summary", "777"), context, "200.00", "240.00", "00:00");
    assertThat(first).contains("01-JAN-2026").doesNotContain("999.00", "240.00");
    assertThat(second).doesNotContain("999.00", "120.00");
    selectedRead(555, 777);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/summary"})
  void missingHistoricalDataRetainsTheKnownStatus(String suffix) throws Exception {
    data.stubFor(get(urlPathEqualTo(endpoint(555))).willReturn(aResponse().withStatus(404)));
    unavailable(request("bill", suffix, "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(ints = {403, 500})
  void upstreamErrorsAreNotUnavailable(int status) throws Exception {
    data.stubFor(get(urlPathEqualTo(endpoint(555))).willReturn(aResponse().withStatus(status)));
    MvcResult result = request("bill", "/summary", "555");
    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getViewName()).isEqualTo("error");
    assertThat(result.getModelAndView().getModel())
        .containsKey("error")
        .doesNotContainKey("detailsUnavailable");
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"malformed-json", "connection-reset"})
  void decodingAndTransportFailuresRemainErrors(String failure) throws Exception {
    var response = aResponse();
    if (failure.equals("malformed-json")) {
      response.withHeader("Content-Type", "application/json").withBody("{broken");
    } else {
      response.withFault(Fault.CONNECTION_RESET_BY_PEER);
    }
    data.stubFor(get(urlPathEqualTo(endpoint(555))).willReturn(response));
    MvcResult result = request("bill", "/summary", "555");
    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getViewName()).isEqualTo("error");
    verifyNoInteractions(soa);
  }

  @ParameterizedTest
  @ValueSource(strings = {"caseReference", "billingIncidentId"})
  void inconsistentResponseIdentityCannotRenderAnotherInvoice(String field) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    body.put(field, field.equals("caseReference") ? "300000999" : 777);
    data.stubFor(get(urlPathEqualTo(endpoint(555))).willReturn(okJson(body.toString())));
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "0", "-1", "abc", "9223372036854775808", "888"})
  void invalidOrUnknownSelectionDoesNotReadAnAssessment(String id) throws Exception {
    unavailable(request("bill", "/summary", id), "bill", null);
    data.verify(0, getRequestedFor(urlPathMatching("/cases/.*/invoices/.*")));
    verifyNoInteractions(soa);
  }

  @Test
  void wrongKindCannotReadAnAssessment() throws Exception {
    statement("Solicitor POA", "Submitted");
    unavailable(request("bill", "/summary", "555"), "bill", null);
    data.verify(0, getRequestedFor(urlPathEqualTo(endpoint(555))));
    verifyNoInteractions(soa);
  }

  @ParameterizedTest
  @ValueSource(strings = {"other-case", "other-provider"})
  void statementScopeCannotBeBypassedByAnInvoiceIdentifier(String scenario) throws Exception {
    var statementRequest = get(urlPathEqualTo("/statementofaccount"));
    var expectedRequest = getRequestedFor(urlPathEqualTo("/statementofaccount"));
    if (scenario.equals("other-case")) {
      ((ApplicationDetail) session.getAttribute(CASE)).setCaseReferenceNumber("300000999");
      statementRequest.withQueryParam("case-reference-number", equalTo("300000999"));
      expectedRequest.withQueryParam("case-reference-number", equalTo("300000999"));
    } else {
      ((UserDetail) session.getAttribute(USER_DETAILS))
          .setProvider(new BaseProvider().id(77).name("Outside Firm"));
      statementRequest.withQueryParam("billing-provider-party-id", equalTo("77"));
      expectedRequest.withQueryParam("billing-provider-party-id", equalTo("77"));
    }
    data.stubFor(statementRequest.willReturn(okJson("{\"content\":[]}")));
    unavailable(request("bill", "/summary", "555"), "bill", null);
    data.verify(1, expectedRequest);
    data.verify(0, getRequestedFor(urlPathMatching("/cases/.*/invoices/.*")));
    verifyNoInteractions(soa);
  }

  @Test
  void deniedPermissionPrecedesAllReads() throws Exception {
    ((ApplicationDetail) session.getAttribute(CASE)).setAvailableFunctions(List.of());
    assertThat(request("bill", "/summary", "555").getResponse().getRedirectedUrl())
        .isEqualTo("/case/billing");
    data.verify(0, getRequestedFor(urlPathMatching(".*")));
    verifyNoInteractions(soa, caab);
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-instances", "no-global", "no-line", "missing-date", "missing-net"})
  void incompleteRetainedAssessmentsAreNotRenderedAsPartialPdfs(String scenario) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    switch (scenario) {
      case "no-instances" -> body.put("instances", new JSONArray());
      case "no-global" -> body.getJSONArray("instances").remove(0);
      case "no-line" -> body.getJSONArray("instances").remove(1);
      case "missing-date" -> remove(line(body), "DISB_DATE");
      case "missing-net" -> remove(line(body), "DISB_NET_CLAIM_INC_UPLFT");
      default -> throw new AssertionError("Unknown fixture " + scenario);
    }
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({
    "bill,CARE_BILL_LINE",
    "bill,FAS_H_BILL_LINE",
    "poa,POAOPC_BILL_LINE",
    "poa,POADIS_BILL_LINE"
  })
  void feeSchemePrefixesRetainTimeItemsAndUplift(String context, String entityType)
      throws Exception {
    String type = context.equals("poa") ? "Solicitor POA" : "Solicitor Final";
    statement(type, "Submitted");
    JSONObject body = invoice(555, type, "82.50", "16.50");
    JSONObject claim = line(body);
    claim.put("entityName", entityType);
    put(claim, "DISB_ITEM", "3");
    put(claim, "DISB_RATE", "25.00");
    put(claim, "DISB_UPLIFT", "10.00");
    put(claim, "DISB_TIME", "01:00:00");
    String prefix = entityType.substring(0, entityType.length() - "_BILL_LINE".length());
    for (Object raw : claim.getJSONArray("attributes")) {
      JSONObject attribute = (JSONObject) raw;
      attribute.put("name", attribute.getString("name").replace("DISB_", prefix + "_"));
    }
    stub(body);
    String text = pdf(request(context, "/summary", "555"), context, "82.50", "99.00", "01:00");
    assertThat(text).contains("01-JAN-2026Disbursement01:00325.0010.0082.5020.0099.00");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({"01:05,01:05", "01:05:00,01:05", "70:08:00,70:08"})
  void retainedClaimedTimeHasAnAccurateFooter(String stored, String expected) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    put(line(body), "DISB_TIME", stored);
    stub(body);
    pdf(request("bill", "/summary", "555"), "bill", "100.00", "120.00", expected);
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"01:60", "01:00:01", "-1:00", "invalid"})
  void invalidOrLossyClaimedTimeIsUnavailable(String stored) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    put(line(body), "DISB_TIME", stored);
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({
    "100.00,100.00,0.00,true",
    "100.00,100.01,0.00,false",
    "100.00,100.00,1.00,false",
    "0.00,0.00,0.00,false",
    "100.00,100.00,NULL,false"
  })
  void zeroVatInferenceRequiresExactPositiveEvidence(
      String net, String gross, String vatClaim, boolean usable) throws Exception {
    String submittedVat = new BigDecimal(gross).subtract(new BigDecimal(net)).toPlainString();
    JSONObject body = invoice(555, "Solicitor Final", net, submittedVat);
    remove(line(body), "DISB_VAT");
    put(line(body), "DISB_TOTAL_CLAIM", gross);
    if (!vatClaim.equals("NULL")) put(line(body), "DISB_VAT_CLAIM", vatClaim);
    stub(body);
    MvcResult result = request("bill", "/summary", "555");
    if (usable) pdf(result, "bill", "100.00", "100.00", "00:00");
    else unavailable(result, "bill", "Submitted");
    selectedRead(555);
  }

  @Test
  void invalidExplicitVatIsNotReplacedByZeroInference() throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "0.00");
    put(line(body), "DISB_VAT", "unknown");
    put(line(body), "DISB_VAT_CLAIM", "0");
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({
    "COURT_ASSESS_BILL,Yes,Yes",
    "COURT_ASSESSED_BILL,true,Yes",
    "GB_CLAIM_ASSESSED_BY_COURT,true,Yes",
    "GB_CLAIM_ASSESSED_BY_COURT,false,No"
  })
  void courtAliasesDisplayYesOrNo(String alias, String value, String expected) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    put(global(body), alias, value);
    stub(body);
    assertThat(pdf(request("bill", "/summary", "555"), "bill", "100.00", "120.00", "00:00"))
        .contains("CourtAssessedBill" + expected);
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "conflict"})
  void unreliableCourtFlagsAreUnavailable(String scenario) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    put(global(body), "COURT_ASSESS_BILL", scenario.equals("invalid") ? "unknown" : "No");
    if (scenario.equals("conflict")) put(global(body), "GB_CLAIM_ASSESSED_BY_COURT", "true");
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @Test
  void assessedOnlyAdjustmentDoesNotHideOrChangeTheSubmittedClaim() throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    JSONObject assessed = instance(2000, "OVERALL_ADJ_BILL_LINE", "assessment-adjustment");
    put(assessed, "ASSESSED_LINE_INCL_VAT", "-12.00");
    body.getJSONArray("instances").put(assessed);
    stub(body);
    pdf(request("bill", "/summary", "555"), "bill", "100.00", "120.00", "00:00");
    selectedRead(555);
  }

  @Test
  void nilLikeMissingClaimFieldsRemainUnavailable() throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "0.00", "0.00");
    remove(line(body), "DISB_DATE");
    remove(line(body), "DISB_NET_CLAIM_INC_UPLFT");
    remove(line(body), "DISB_TOTAL_CLAIM");
    put(line(body), "DISB_NET_VALUE", "0.00");
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @Test
  void completeDatedZeroClaimsAreNotMistakenForMissingData() throws Exception {
    stub(invoice(555, "Solicitor Final", "0.00", "0.00"));
    pdf(request("bill", "/summary", "555"), "bill", "0.00", "0.00", "00:00");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({"BILL_SUBMITTED_TOTAL_AMT,100.01", "BILL_SUBMITTED_TOTAL_VAT,20.01"})
  void mismatchingSubmittedTotalsAreUnavailable(String name, String value) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    put(global(body), name, value);
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @ParameterizedTest
  @ValueSource(strings = {"responseValue", "responseText", "extendedValue"})
  void responseValuePrecedenceAndFallbackReachThePdf(String source) throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    JSONObject description = put(global(body), "BILL_DESCRIPTION", null);
    description.put(source, "Chosen description");
    if (source.equals("responseValue")) description.put("responseText", "Wrong description");
    if (!source.equals("extendedValue")) {
      description.put("extendedValue", "Wrong extended description");
    }
    stub(body);
    assertThat(pdf(request("bill", "/summary", "555"), "bill", "100.00", "120.00", "00:00"))
        .contains("Chosendescription")
        .doesNotContain("Wrongdescription", "Wrongextendeddescription");
    selectedRead(555);
  }

  @ParameterizedTest
  @CsvSource({"BILL_SUBMITTED_TOTAL_AMT,100.00", "DISB_DATE,01-01-2026"})
  void invalidMandatoryValuesCannotUseValidLowerPrecedenceFallbacks(String name, String fallback)
      throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "100.00", "20.00");
    JSONObject target = name.startsWith("BILL_") ? global(body) : line(body);
    put(target, name, "invalid").put("responseText", fallback).put("extendedValue", fallback);
    stub(body);
    unavailable(request("bill", "/summary", "555"), "bill", "Submitted");
    selectedRead(555);
  }

  @Test
  void multiPageRepeatedLabelsRetainEveryLineAndReconciledFooter() throws Exception {
    JSONObject body = invoice(555, "Solicitor Final", "169.00", "0.00");
    body.getJSONArray("instances").remove(1);
    for (int number = 0; number < 169; number++) {
      JSONObject claim = claim(1002 + number, "1.00", "1.00");
      put(claim, "DISB_VAT", "0");
      put(claim, "DISB_TIME", "01:00:00");
      body.getJSONArray("instances").put(claim);
    }
    stub(body);
    MvcResult result = request("bill", "/summary", "555");
    String text = pdf(result, "bill", "169.00", "169.00", "169:00");
    assertThat(text.split("01-JAN-2026", -1).length - 1).isEqualTo(169);
    PdfReader reader = new PdfReader(result.getResponse().getContentAsByteArray());
    try {
      assertThat(reader.getNumberOfPages()).isGreaterThan(1);
    } finally {
      reader.close();
    }
    selectedRead(555);
  }

  private JSONObject invoice(long id, String type, String net, String vat) {
    JSONObject global = instance(1001, "global", "global");
    put(global, "BILL_TYPE", type);
    put(global, "BILL_SUBMITTED_TOTAL_AMT", net);
    put(global, "BILL_SUBMITTED_TOTAL_VAT", vat);
    String gross = new BigDecimal(net).add(new BigDecimal(vat)).toPlainString();
    JSONObject claim = claim(1002, net, gross);
    if (new BigDecimal(vat).signum() == 0) put(claim, "DISB_VAT", "0");
    return new JSONObject()
        .put("caseReference", CASE_REF)
        .put("billingIncidentId", id)
        .put("assessmentId", id == 555 ? 501 : 502)
        .put("billingProviderId", id == 555 ? "10" : "20")
        .put("instances", new JSONArray().put(global).put(claim));
  }

  private JSONObject claim(long id, String net, String gross) {
    JSONObject line = instance(id, "DISB_BILL_LINE", "repeated-label");
    put(line, "DISB_DATE", "01-01-2026");
    put(line, "DISB_COST_TYPE", "Disbursement");
    put(line, "DISB_NET_CLAIM_INC_UPLFT", net);
    put(line, "DISB_VAT", "20");
    put(line, "DISB_TOTAL_CLAIM", gross);
    return line;
  }

  private JSONObject instance(long id, String type, String label) {
    return new JSONObject()
        .put("id", id)
        .put("entityName", type)
        .put("label", label)
        .put("attributes", new JSONArray());
  }

  private JSONObject global(JSONObject body) {
    return body.getJSONArray("instances").getJSONObject(0);
  }

  private JSONObject line(JSONObject body) {
    return body.getJSONArray("instances").getJSONObject(1);
  }

  private JSONObject put(JSONObject instance, String name, String value) {
    JSONArray attributes = instance.getJSONArray("attributes");
    long nextId = instance.getLong("id") * 100 + 1;
    for (Object item : attributes) {
      JSONObject attribute = (JSONObject) item;
      nextId = Math.max(nextId, attribute.getLong("id") + 1);
      if (attribute.getString("name").equals(name)) {
        attribute.put("responseValue", value == null ? JSONObject.NULL : value);
        return attribute;
      }
    }
    JSONObject attribute =
        new JSONObject()
            .put("id", nextId)
            .put("name", name)
            .put("responseType", "text")
            .put("responseValue", value == null ? JSONObject.NULL : value)
            .put("userEntered", !name.contains("TOTAL") && !name.contains("CLAIM"));
    attributes.put(attribute);
    return attribute;
  }

  private void remove(JSONObject instance, String name) {
    JSONArray attributes = instance.getJSONArray("attributes");
    for (int index = attributes.length() - 1; index >= 0; index--) {
      if (attributes.getJSONObject(index).getString("name").equals(name)) attributes.remove(index);
    }
  }

  private InvoiceDataResponse legacyInvoice() {
    List<OpaEntity> entities = new ArrayList<>();
    for (Object item :
        invoice(555, "Solicitor Final", "999.00", "0.00").getJSONArray("instances")) {
      JSONObject instance = (JSONObject) item;
      List<OpaAttribute> attributes = new ArrayList<>();
      for (Object raw : instance.getJSONArray("attributes")) {
        JSONObject attribute = (JSONObject) raw;
        attributes.add(
            new OpaAttribute()
                .attribute(attribute.getString("name"))
                .responseType("text")
                .responseValue(attribute.getString("responseValue")));
      }
      entities.add(
          new OpaEntity()
              .entityName(instance.getString("entityName"))
              .instances(
                  List.of(
                      new OpaInstance()
                          .instanceLabel(instance.getString("label"))
                          .attributes(attributes))));
    }
    return new InvoiceDataResponse().opaResponse(entities);
  }
}
