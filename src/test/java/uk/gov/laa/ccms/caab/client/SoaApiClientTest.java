package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.CoverSheet;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;
import uk.gov.laa.ccms.soa.gateway.model.TransactionStatus;
import uk.gov.laa.ccms.soa.gateway.model.UserOptions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class SoaApiClientTest {
  @Mock
  private WebClient soaApiWebClientMock;
  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock
  private WebClient.ResponseSpec responseMock;

  @Mock
  private WebClient.RequestBodySpec requestBodyMock;
  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriMock;

  @Mock
  private SoaApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks
  private SoaApiClient soaApiClient;


  @Test
  void getContractDetails_returnsData() {
    Integer providerFirmId = 123;
    Integer officeId = 345;
    String loginId = "user1";
    String userType = "userType";

    ContractDetails contractDetails = new ContractDetails();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.just(contractDetails));

    Mono<ContractDetails> contractDetailsMono =
        soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType);

    StepVerifier.create(contractDetailsMono)
        .expectNextMatches(cd -> cd == contractDetails)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals("/contract-details?providerFirmId=123&officeId=345", actualUri.toString());
  }

  @Test
  void getContractDetails_handlesError() {
    Integer providerFirmId = 123;
    Integer officeId = 345;
    String loginId = "user1";
    String userType = "userType";

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);

    when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("Contract details"), any())).thenReturn(Mono.empty());

    Mono<ContractDetails> contractDetailsMono =
        soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType);

    StepVerifier.create(contractDetailsMono)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals("/contract-details?providerFirmId=123&officeId=345", actualUri.toString());
  }

  @Test
  void getClients_ReturnsClientDetails_Successful() {
    String expectedUri = "/clients?first-name=John&surname=Doe&date-of-birth=2000-01-01&page=0&size=10";

    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    String loginId = "user1";
    String userType = "userType";
    String firstName = "John";
    String lastName = "Doe";

    int page = 0;
    int size = 10;

    clientSearchCriteria.setForename(firstName);
    clientSearchCriteria.setSurname(lastName);
    clientSearchCriteria.setDobDay("1");
    clientSearchCriteria.setDobMonth("1");
    clientSearchCriteria.setDobYear("2000");

    ClientDetails mockClientDetails = new ClientDetails();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientDetails.class)).thenReturn(Mono.just(mockClientDetails));

    Mono<ClientDetails> clientDetailsMono =
        soaApiClient.getClients(clientSearchCriteria, loginId, userType, page, size);

    StepVerifier.create(clientDetailsMono)
        .expectNextMatches(clientDetails -> clientDetails == mockClientDetails)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(ClientDetails.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getCase_ReturnsCaseDetail_Successful() {
    String caseReferenceNumber = "123";
    String loginId = "user1";
    String userType = "userType";

    String expectedUri = String.format("/cases/%s", caseReferenceNumber);

    CaseDetail mockCaseDetail = new CaseDetail();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseDetail.class)).thenReturn(Mono.just(mockCaseDetail));

    Mono<CaseDetail> caseDetailMono =
        soaApiClient.getCase(caseReferenceNumber, loginId, userType);

    StepVerifier.create(caseDetailMono)
        .expectNextMatches(caseDetail -> caseDetail == mockCaseDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(CaseDetail.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getClient_returnsClientDetails_Successful() {
    String clientReferenceNumber = "CLIENT123";
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/{clientReferenceNumber}";

    ClientDetail mockClientDetail = new ClientDetail();

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, clientReferenceNumber)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientDetail.class)).thenReturn(Mono.just(mockClientDetail));

    Mono<ClientDetail> clientDetailMono =
        soaApiClient.getClient(clientReferenceNumber, loginId, userType);

    StepVerifier.create(clientDetailMono)
        .expectNextMatches(clientDetail -> clientDetail == mockClientDetail)
        .verifyComplete();
  }

  @Test
  void getClient_handlesError() {
    String clientReferenceNumber = "CLIENT123";
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/{clientReferenceNumber}";

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, clientReferenceNumber)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("client"), eq("reference number"), eq(clientReferenceNumber))).thenReturn(Mono.empty());

    Mono<ClientDetail> clientDetailMono =
        soaApiClient.getClient(clientReferenceNumber, loginId, userType);

    StepVerifier.create(clientDetailMono)
        .verifyComplete();
  }

  @Test
  void getClientStatus_Successful() {
    String transactionId = "123";
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/status/{transactionId}";

    TransactionStatus mockTransactionStatus = new TransactionStatus();

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(Mono.just(mockTransactionStatus));

    Mono<TransactionStatus> transactionStatusMono = soaApiClient.getClientStatus(transactionId, loginId, userType);

    StepVerifier.create(transactionStatusMono)
        .expectNextMatches(transactionStatus -> transactionStatus == mockTransactionStatus)
        .verifyComplete();
  }

  @Test
  void getClientStatus_Error() {
    String transactionId = "123";
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/status/{transactionId}";

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("client transaction status"), eq("transaction id"), eq(transactionId)))
        .thenReturn(Mono.empty());

    Mono<TransactionStatus> transactionStatusMono = soaApiClient.getClientStatus(transactionId, loginId, userType);

    StepVerifier.create(transactionStatusMono)
        .verifyComplete();
  }

  @Test
  void postClient_Successful() {

    ClientDetailDetails clientDetails = new ClientDetailDetails();
    clientDetails.setName(new NameDetail().fullName("John Doe"));
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients";

    ClientTransactionResponse mockClientCreated = new ClientTransactionResponse();
    mockClientCreated.setTransactionId("123");

    when(soaApiWebClientMock.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ClientDetailDetails.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockClientCreated));

    Mono<ClientTransactionResponse> clientCreatedMono = soaApiClient.postClient(clientDetails, loginId, userType);

    StepVerifier.create(clientCreatedMono)
        .expectNext(mockClientCreated)
        .verifyComplete();
  }

  @Test
  void postClient_Error() {
    ClientDetailDetails clientDetails = new ClientDetailDetails();
    clientDetails.setName(new NameDetail().fullName("John Doe"));
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients";

    when(soaApiWebClientMock.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ClientDetailDetails.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiCreateError(any(), eq("Client"))).thenReturn(Mono.empty());

    Mono<ClientTransactionResponse> clientCreatedMono = soaApiClient.postClient(clientDetails, loginId, userType);

    StepVerifier.create(clientCreatedMono)
        .verifyComplete();
  }

  @Test
  void putClient_Successful() {

    ClientDetailDetails clientDetails = new ClientDetailDetails();
    clientDetails.setName(new NameDetail().fullName("John Doe"));
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/{clientReferenceNumber}";
    String clientReferenceNumber = "1234";

    ClientTransactionResponse mockClientUpdated = new ClientTransactionResponse();
    mockClientUpdated.setTransactionId("123");

    when(soaApiWebClientMock.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, clientReferenceNumber)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ClientDetailDetails.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockClientUpdated));

    Mono<ClientTransactionResponse> clientUpdatedMono = soaApiClient.putClient(
        clientReferenceNumber, clientDetails, loginId, userType);

    StepVerifier.create(clientUpdatedMono)
        .expectNext(mockClientUpdated)
        .verifyComplete();
  }

  @Test
  void putClient_Error() {

    ClientDetailDetails clientDetails = new ClientDetailDetails();
    clientDetails.setName(new NameDetail().fullName("John Doe"));
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/clients/{clientReferenceNumber}";
    String clientReferenceNumber = "1234";

    ClientTransactionResponse mockClientUpdated = new ClientTransactionResponse();
    mockClientUpdated.setTransactionId("123");

    when(soaApiWebClientMock.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, clientReferenceNumber)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ClientDetailDetails.class))).thenReturn(requestHeadersMock);

    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiUpdateError(
        any(), eq("Client"), eq("reference number"), eq(clientReferenceNumber)))
        .thenReturn(Mono.empty());

    Mono<ClientTransactionResponse> clientUpdatedMono = soaApiClient.putClient(
        clientReferenceNumber, clientDetails, loginId, userType);

    StepVerifier.create(clientUpdatedMono)
        .verifyComplete();
  }

  @Test
  void getOrganisations_Successful() {
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();
    searchCriteria.setName("thename");
    searchCriteria.setType("thetype");
    searchCriteria.setCity("thecity");
    searchCriteria.setPostcode("thepostcode");

    String expectedUri = String.format(
        "/organisations?name=%s&type=%s&city=%s&postcode=%s&page=%s&size=%s",
        searchCriteria.getName(),
        searchCriteria.getType(),
        searchCriteria.getCity(),
        searchCriteria.getPostcode(),
        page,
        size);

    OrganisationDetails mockOrganisationDetails = new OrganisationDetails();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrganisationDetails.class)).thenReturn(
        Mono.just(mockOrganisationDetails));

    Mono<OrganisationDetails> organisationDetailsMono =
        soaApiClient.getOrganisations(searchCriteria, loginId, userType, page, size);

    StepVerifier.create(organisationDetailsMono)
        .expectNextMatches(orgDetails -> orgDetails == mockOrganisationDetails)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(OrganisationDetails.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getOrganisations_handlesError() {
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrganisationDetails.class)).thenReturn(Mono.error(
        new WebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("Organisations"), any())).thenReturn(Mono.empty());

    Mono<OrganisationDetails> organisationDetailsMono =
        soaApiClient.getOrganisations(searchCriteria, loginId, userType, page, size);

    StepVerifier.create(organisationDetailsMono)
        .verifyComplete();
  }

  @Test
  void getOrganisation_Successful() {
    String loginId = "user1";
    String userType = "userType";
    String orgId = "123";

    String expectedUri = String.format("/organisations/%s", orgId);

    OrganisationDetail mockOrganisationDetail = new OrganisationDetail();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrganisationDetail.class)).thenReturn(
        Mono.just(mockOrganisationDetail));

    Mono<OrganisationDetail> organisationDetailMono =
        soaApiClient.getOrganisation(orgId, loginId, userType);

    StepVerifier.create(organisationDetailMono)
        .expectNextMatches(orgDetail -> orgDetail == mockOrganisationDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(OrganisationDetail.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getOrganisation_handlesError() {
    String loginId = "user1";
    String userType = "userType";
    String orgId = "123";

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrganisationDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("Organisation"), eq("id"), eq(orgId))).thenReturn(Mono.empty());

    Mono<OrganisationDetail> organisationDetailMono =
        soaApiClient.getOrganisation(orgId, loginId, userType);

    StepVerifier.create(organisationDetailMono)
        .verifyComplete();
  }

  @Test
  void downloadCoverSheet_successful() {
    String loginId = "user1";
    String userType = "userType";
    String attachmentId = "12345";

    String expectedUri = "/documents/12345/cover-sheet";

    CoverSheet mockCoverSheet = new CoverSheet();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CoverSheet.class)).thenReturn(
        Mono.just(mockCoverSheet));

    Mono<CoverSheet> coverSheetMono =
        soaApiClient.downloadCoverSheet(attachmentId, loginId, userType);

    StepVerifier.create(coverSheetMono)
        .expectNextMatches(coverSheet -> coverSheet == mockCoverSheet)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(CoverSheet.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void downloadDocument_successful() {
    String loginId = "user1";
    String userType = "userType";
    String attachmentId = "12345";

    String expectedUri = "/documents/12345";

    Document mockDocument = new Document();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Document.class)).thenReturn(
        Mono.just(mockDocument));

    Mono<Document> documentMono =
        soaApiClient.downloadDocument(attachmentId, loginId, userType);

    StepVerifier.create(documentMono)
        .expectNextMatches(document -> document == mockDocument)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(Document.class);

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void registerDocument_Successful() {
    Document document = new Document();
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/documents";

    ClientTransactionResponse mockDocumentRegistered = new ClientTransactionResponse();
    mockDocumentRegistered.setTransactionId("123");

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(Document.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockDocumentRegistered));

    Mono<ClientTransactionResponse> documentRegisteredMono =
        soaApiClient.registerDocument(document,
        loginId,
        userType);

    StepVerifier.create(documentRegisteredMono)
        .expectNext(mockDocumentRegistered)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void uploadDocument_Successful() {
    Document document = new Document();
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/documents?notification-reference=12345&case-reference-number=98765";

    ClientTransactionResponse mockDocumentRegistered = new ClientTransactionResponse();
    mockDocumentRegistered.setTransactionId("123");

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(Document.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockDocumentRegistered));

    Mono<ClientTransactionResponse> documentRegisteredMono =
        soaApiClient.uploadDocument(document,
            "12345",
            "98765",
            loginId,
            userType);

    StepVerifier.create(documentRegisteredMono)
        .expectNext(mockDocumentRegistered)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void updateDocument_Successful() {
    Document document = new Document();
    document.setDocumentId("123");
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/documents/123?notification-reference=12345";

    ClientTransactionResponse mockDocumentUploaded = new ClientTransactionResponse();
    mockDocumentUploaded.setTransactionId("123");

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(Document.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockDocumentUploaded));

    Mono<ClientTransactionResponse> documentUploadedMono =
        soaApiClient.updateDocument(document,
            "12345",
            null,
            loginId,
            userType);

    StepVerifier.create(documentUploadedMono)
        .expectNext(mockDocumentUploaded)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void updateUser_Successful() {

    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/users/options";
    String providerFirmId = "1234";
    UserOptions userOptions = new UserOptions()
        .userLoginId(loginId)
        .providerFirmId(providerFirmId);

    ClientTransactionResponse mockUserUpdated = new ClientTransactionResponse();
    mockUserUpdated.setTransactionId("123");

    when(soaApiWebClientMock.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(UserOptions.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientTransactionResponse.class)).thenReturn(Mono.just(mockUserUpdated));

    Mono<ClientTransactionResponse> userUpdatedMono = soaApiClient.updateUserOptions(
        userOptions, loginId, userType);

    StepVerifier.create(userUpdatedMono)
        .expectNext(mockUserUpdated)
        .verifyComplete();
  }

}
