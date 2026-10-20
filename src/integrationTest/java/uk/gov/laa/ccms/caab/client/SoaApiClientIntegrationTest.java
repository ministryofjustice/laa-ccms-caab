package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.mapper.SoaApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.context.CaseMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.CoverSheet;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.UserOptions;

public class SoaApiClientIntegrationTest extends AbstractIntegrationTest {

  public static final String USER_1 = "user1";
  public static final String USER_TYPE = "userType";
  public static final String SOA_GATEWAY_USER_LOGIN_ID = "SoaGateway-User-Login-Id";
  public static final String SOA_GATEWAY_USER_ROLE = "SoaGateway-User-Role";

  @RegisterExtension
  protected static WireMockExtension wiremock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.soa-api.port", wiremock::getPort);
  }

  @Autowired private SoaApiClient soaApiClient;

  @Autowired private SoaApplicationMapper applicationMapper;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void meansReassessmentSendsAnswersAndApplicationTypeToGateway() {
    final ApplicationDetail amendment =
        new ApplicationDetail()
            .caseReferenceNumber("12345")
            .applicationType(new ApplicationType().id("SUB"))
            .quickEditType("MeansReassessment")
            .meansAssessmentAmended(true)
            .meritsAssessmentAmended(false)
            .availableFunctions(List.of("MNLA", "MNSB"));
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .status("COMPLETE")
            .addEntityTypesItem(
                new AssessmentEntityTypeDetail()
                    .name("global")
                    .addEntitiesItem(
                        new AssessmentEntityDetail()
                            .name("12345")
                            .addAttributesItem(
                                new AssessmentAttributeDetail()
                                    .name("TEST_INCOME")
                                    .type("number")
                                    .value("123.45")
                                    .prepopulated(false))));

    wiremock.stubFor(
        put("/cases?case-update-type=MeansReassessment")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(USER_1))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(USER_TYPE))
            .withRequestBody(
                matchingJsonPath(
                    "$.application_details.application_amendment_type", equalTo("SUB")))
            .withRequestBody(
                matchingJsonPath(
                    "$.application_details.means_assessments[0].assessment_details[0].entity[0].instances[0].attributes[0].response_value",
                    equalTo("123.45")))
            .withRequestBody(
                matchingJsonPath(
                    "$.application_details.means_assessments[0].assessment_details[0].entity[0].instances[0].attributes[0].user_defined_ind",
                    equalTo("true")))
            .willReturn(okJson("{\"transaction_id\":\"means-transaction\"}")));

    final var payload =
        applicationMapper.toCaseDetail(
            CaseMappingContext.builder()
                .tdsApplication(amendment)
                .meansAssessment(assessment)
                .build());
    final var response =
        soaApiClient.updateCase(USER_1, USER_TYPE, payload, "MeansReassessment").block();

    assertNotNull(response);
    assertEquals("means-transaction", response.getTransactionId());
  }

  @Test
  public void testGetContractDetails_returnData() throws Exception {
    Integer providerFirmId = 123;
    Integer officeId = 345;
    String loginId = USER_1;
    String userType = USER_TYPE;
    ContractDetails contractDetails = buildContractDetails();
    String contractDetailsJson = objectMapper.writeValueAsString(contractDetails);

    wiremock.stubFor(
        get("/contract-details?providerFirmId=%s&officeId=%s".formatted(providerFirmId, officeId))
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(contractDetailsJson)));

    Mono<ContractDetails> response =
        soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType);

    assertNotNull(response);
    assertEquals(contractDetailsJson, objectMapper.writeValueAsString(response.block()));
  }

  @Test
  public void testGetClient_returnData() throws Exception {
    String clientReferenceNumber = "clientRef1";
    String loginId = USER_1;
    String userType = USER_TYPE;
    ClientDetail clientDetail = new ClientDetail(); // Fill with appropriate data
    String clientDetailJson = objectMapper.writeValueAsString(clientDetail);

    wiremock.stubFor(
        get("/clients/%s".formatted(clientReferenceNumber))
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientDetailJson)));

    Mono<ClientDetail> clientMono =
        soaApiClient.getClient(clientReferenceNumber, loginId, userType);

    ClientDetail response = clientMono.block();

    assertEquals(clientDetail, response);
  }

  @Test
  public void testGetNotificationAttachments_returnsData() throws JsonProcessingException {
    String documentId = "documentId";
    String documentContent = "documentContent";
    String loginId = "loginId";
    String userType = "userType";

    Document notificationAttachment =
        new Document().documentId(documentId).fileData(documentContent);

    String notificationAttachmentJson = objectMapper.writeValueAsString(notificationAttachment);

    wiremock.stubFor(
        get("/documents/%s".formatted(documentId))
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(notificationAttachmentJson)));

    Document response = soaApiClient.downloadDocument(documentId, loginId, userType).block();

    assertEquals(notificationAttachment, response);
  }

  @Test
  public void testRegisterDocument_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    ClientTransactionResponse clientTransactionResponse =
        new ClientTransactionResponse().transactionId("01234").referenceNumber("56789");

    String clientTransactionResponseString =
        objectMapper.writeValueAsString(clientTransactionResponse);

    wiremock.stubFor(
        post("/documents")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientTransactionResponseString)));

    Mono<ClientTransactionResponse> clientTransactionResponseMono =
        soaApiClient.registerDocument(new Document(), loginId, userType);

    ClientTransactionResponse response = clientTransactionResponseMono.block();

    assertEquals(clientTransactionResponse, response);
  }

  @Test
  public void testUploadDocument_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    ClientTransactionResponse clientTransactionResponse =
        new ClientTransactionResponse().transactionId("01234").referenceNumber("56789");

    String clientTransactionResponseString =
        objectMapper.writeValueAsString(clientTransactionResponse);

    wiremock.stubFor(
        post("/documents?notification-reference=12345")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientTransactionResponseString)));

    Mono<ClientTransactionResponse> clientTransactionResponseMono =
        soaApiClient.uploadDocument(new Document(), "12345", null, loginId, userType);

    ClientTransactionResponse response = clientTransactionResponseMono.block();

    assertEquals(clientTransactionResponse, response);
  }

  @Test
  public void testUpdateDocument_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    ClientTransactionResponse clientTransactionResponse =
        new ClientTransactionResponse().transactionId("01234").referenceNumber("56789");

    String clientTransactionResponseString =
        objectMapper.writeValueAsString(clientTransactionResponse);

    wiremock.stubFor(
        put("/documents/56789?notification-reference=12345")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientTransactionResponseString)));

    Mono<ClientTransactionResponse> clientTransactionResponseMono =
        soaApiClient.updateDocument(
            new Document().documentId("56789"), "12345", null, loginId, userType);

    ClientTransactionResponse response = clientTransactionResponseMono.block();

    assertEquals(clientTransactionResponse, response);
  }

  @Test
  public void testDownloadCoverSheet_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    CoverSheet coverSheet =
        new CoverSheet()
            .fileData("Y29udGVudA==") // content
            .documentId("12345");

    String coverSheetResponse = objectMapper.writeValueAsString(coverSheet);

    wiremock.stubFor(
        get("/documents/12345/cover-sheet")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(coverSheetResponse)));

    Mono<CoverSheet> coverSheetMono = soaApiClient.downloadCoverSheet("12345", loginId, userType);

    CoverSheet response = coverSheetMono.block();

    assertEquals(coverSheet, response);
  }

  @Test
  public void testDownloadDocument_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    Document document =
        new Document()
            .fileData("Y29udGVudA==") // content
            .documentId("12345")
            .channel(ELECTRONIC.getCode())
            .documentType("DOC_TYPE")
            .fileExtension("ext")
            .status("status")
            .statusDescription("status description")
            .text("text");

    String documentResponse = objectMapper.writeValueAsString(document);

    wiremock.stubFor(
        get("/documents/12345")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(documentResponse)));

    Mono<Document> documentMono = soaApiClient.downloadDocument("12345", loginId, userType);

    Document response = documentMono.block();

    assertEquals(document, response);
  }

  @Test
  public void testUpdateUserOptions_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    ClientTransactionResponse clientTransactionResponse =
        new ClientTransactionResponse().transactionId("01234");

    String clientTransactionResponseString =
        objectMapper.writeValueAsString(clientTransactionResponse);

    wiremock.stubFor(
        put("/users/options")
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientTransactionResponseString)));

    Mono<ClientTransactionResponse> clientTransactionResponseMono =
        soaApiClient.updateUserOptions(new UserOptions(), loginId, userType);

    ClientTransactionResponse response = clientTransactionResponseMono.block();

    assertEquals(clientTransactionResponse, response);
  }

  @Test
  public void testUpdateNotification_returnsData() throws Exception {
    String loginId = USER_1;
    String userType = USER_TYPE;
    String notificationId = "12345";
    ClientTransactionResponse clientTransactionResponse =
        new ClientTransactionResponse().transactionId("01234");

    String clientTransactionResponseString =
        objectMapper.writeValueAsString(clientTransactionResponse);

    wiremock.stubFor(
        put("/notifications/%s".formatted(notificationId))
            .withHeader(SOA_GATEWAY_USER_LOGIN_ID, equalTo(loginId))
            .withHeader(SOA_GATEWAY_USER_ROLE, equalTo(userType))
            .willReturn(okJson(clientTransactionResponseString)));

    Mono<ClientTransactionResponse> clientTransactionResponseMono =
        soaApiClient.updateNotification(notificationId, new Notification(), loginId, userType);

    ClientTransactionResponse response = clientTransactionResponseMono.block();

    assertEquals(clientTransactionResponse, response);
  }

  private ContractDetails buildContractDetails() {
    return new ContractDetails()
        .addContractsItem(
            new ContractDetail()
                .categoryofLaw("CAT1")
                .subCategory("SUBCAT1")
                .createNewMatters(true)
                .remainderAuthorisation(true)
                .contractualDevolvedPowers("CATDEVPOW")
                .authorisationType("AUTHTYPE1"));
  }
}
