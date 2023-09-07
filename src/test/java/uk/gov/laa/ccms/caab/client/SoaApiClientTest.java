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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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
  private SoaApiClientErrorHandler soaApiClientErrorHandler;

  @InjectMocks
  private SoaApiClient soaApiClient;

  @Test
  void getNotificationsSummary_returnData() {

    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/users/{loginId}/notifications/summary";

    NotificationSummary mockSummary = new NotificationSummary()
        .notifications(10)
        .standardActions(5)
        .overdueActions(2);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.just(mockSummary));

    Mono<NotificationSummary> summaryMono =
        soaApiClient.getNotificationsSummary(loginId, userType);

    StepVerifier.create(summaryMono)
        .expectNextMatches(summary ->
            summary.getNotifications() == 10 &&
                summary.getStandardActions() == 5 &&
                summary.getOverdueActions() == 2)
        .verifyComplete();
  }

  @Test
  void getNotificationsSummary_notFound() {
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/users/{loginId}/notifications/summary";

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(soaApiClientErrorHandler.handleNotificationSummaryError(eq(loginId),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<NotificationSummary> summaryMono =
        soaApiClient.getNotificationsSummary(loginId, userType);

    StepVerifier.create(summaryMono)
        .verifyComplete();
  }

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

    when(soaApiClientErrorHandler.handleContractDetailsError(eq(providerFirmId), eq(officeId),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<ContractDetails> contractDetailsMono =
        soaApiClient.getContractDetails(providerFirmId, officeId, loginId, userType);

    StepVerifier.create(contractDetailsMono)
        .verifyComplete();

    verify(soaApiClientErrorHandler).handleContractDetailsError(eq(providerFirmId),
        eq(officeId), any(WebClientResponseException.class));

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals("/contract-details?providerFirmId=123&officeId=345", actualUri.toString());
  }

  @Test
  void getClients_ReturnsClientDetails_Successful() {
    String expectedUri = "/clients?first-name=John&surname=Doe&page=0&size=10";

    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    String loginId = "user1";
    String userType = "userType";
    String firstName = "John";
    String lastName = "Doe";

    int page = 0;
    int size = 10;

    clientSearchCriteria.setForename(firstName);
    clientSearchCriteria.setSurname(lastName);

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
  void getCases_ReturnsCaseDetails_Successful() {

    CopyCaseSearchCriteria copyCaseSearchCriteria = new CopyCaseSearchCriteria();
    copyCaseSearchCriteria.setCaseReference("123");
    copyCaseSearchCriteria.setProviderCaseReference("456");
    copyCaseSearchCriteria.setActualStatus("appl");
    copyCaseSearchCriteria.setFeeEarnerId(789);
    copyCaseSearchCriteria.setOfficeId(999);
    copyCaseSearchCriteria.setClientSurname("asurname");
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    String expectedUri = String.format("/cases?case-reference-number=%s&" +
            "provider-case-reference=%s&" +
            "case-status=%s&" +
            "fee-earner-id=%s&" +
            "office-id=%s&" +
            "client-surname=%s&" +
            "page=%s&" +
            "size=%s",
        copyCaseSearchCriteria.getCaseReference(),
        copyCaseSearchCriteria.getProviderCaseReference(),
        copyCaseSearchCriteria.getActualStatus(),
        copyCaseSearchCriteria.getFeeEarnerId(),
        copyCaseSearchCriteria.getOfficeId(),
        copyCaseSearchCriteria.getClientSurname(),
        page,
        size);

    CaseDetails mockCaseDetails = new CaseDetails();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseDetails.class)).thenReturn(Mono.just(mockCaseDetails));

    Mono<CaseDetails> caseDetailsMono =
        soaApiClient.getCases(copyCaseSearchCriteria, loginId, userType, page, size);

    StepVerifier.create(caseDetailsMono)
        .expectNextMatches(clientDetails -> clientDetails == mockCaseDetails)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    verify(soaApiWebClientMock).get();
    verify(requestHeadersUriMock).uri(uriCaptor.capture());
    verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
    verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
    verify(requestHeadersMock).retrieve();
    verify(responseMock).bodyToMono(CaseDetails.class);

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

    when(soaApiClientErrorHandler.handleClientDetailError(eq(clientReferenceNumber),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<ClientDetail> clientDetailMono =
        soaApiClient.getClient(clientReferenceNumber, loginId, userType);

    StepVerifier.create(clientDetailMono)
        .verifyComplete();
  }

  @Test
  void getCaseReference_returnsCaseReferenceSummary_Successful() {
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/case-reference";

    CaseReferenceSummary mockCaseReferenceSummary = new CaseReferenceSummary();

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseReferenceSummary.class)).thenReturn(
        Mono.just(mockCaseReferenceSummary));

    Mono<CaseReferenceSummary> caseReferenceSummaryMono =
        soaApiClient.getCaseReference(loginId, userType);

    StepVerifier.create(caseReferenceSummaryMono)
        .expectNextMatches(summary -> summary == mockCaseReferenceSummary)
        .verifyComplete();
  }

  @Test
  void getCaseReference_handlesError() {
    String loginId = "user1";
    String userType = "userType";
    String expectedUri = "/case-reference";

    when(soaApiWebClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(
        requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseReferenceSummary.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(soaApiClientErrorHandler.handleCaseReferenceError(
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<CaseReferenceSummary> caseReferenceSummaryMono =
        soaApiClient.getCaseReference(loginId, userType);

    StepVerifier.create(caseReferenceSummaryMono)
        .verifyComplete();
  }
}