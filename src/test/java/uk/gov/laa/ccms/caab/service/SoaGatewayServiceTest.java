package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class SoaGatewayServiceTest {

    @Mock
    private WebClient soaGatewayWebClientMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @InjectMocks
    private SoaGatewayService soaGatewayService;

    @Mock
    private SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler;

    @Test
    void getNotificationsSummary_returnData() {

        String loginId = "user1";
        String userType = "userType";
        String expectedUri = "/users/{loginId}/notifications/summary";

        NotificationSummary mockSummary = new NotificationSummary()
                .notifications(10)
                .standardActions(5)
                .overdueActions(2);

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.just(mockSummary));

        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId, userType);

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

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(soaGatewayServiceErrorHandler.handleNotificationSummaryError(eq(loginId), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId, userType);

        StepVerifier.create(summaryMono)
                .verifyComplete();
    }

    @Test
    void getCategoryOfLawCodes_returnData() {

        Integer providerFirmId = 123;
        Integer officeId = 345;
        String loginId = "user1";
        String userType = "userType";

        ContractDetails contractDetails = new ContractDetails()
            .addContractsItem(
                createContractDetail("CAT1", true, true))
            .addContractsItem(
                createContractDetail("CAT2", true, true));

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.just(contractDetails));

        List<String> response = soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, Boolean.TRUE);

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("CAT1", response.get(0));
        assertEquals("CAT2", response.get(1));

        assertEquals("/contract-details?providerFirmId=123&officeId=345", actualUri.toString());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "null, true, true, CAT2, true, true, true",
        "CAT1, null, true, CAT2, true, true, true",
        "CAT1, false, null, CAT2, false, true, false",
        "CAT1, false, false, CAT2, false, true, false"}, nullValues={"null"})
    void getCategoryOfLawCodes_filtersCorrectly(String cat1, Boolean newMatters1, Boolean remAuth1,
        String cat2, Boolean newMatters2, Boolean remAuth2, Boolean initialApp) {

        Integer providerFirmId = 123;
        Integer officeId = 345;
        String loginId = "user1";
        String userType = "userType";

        ContractDetails contractDetails = new ContractDetails()
            .addContractsItem(
                createContractDetail(cat1, newMatters1, remAuth1))
            .addContractsItem(
                createContractDetail(cat2, newMatters2, remAuth2));

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.just(contractDetails));

        List<String> response = soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, initialApp);

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(cat2, response.get(0));

        assertEquals("/contract-details?providerFirmId=123&officeId=345", actualUri.toString());
    }

    @Test
    void getCategoryOfLawCodes_handlesError() {
        Integer providerFirmId = 123;
        Integer officeId = 345;
        String loginId = "user1";
        String userType = "userType";

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);

        when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(soaGatewayServiceErrorHandler.handleContractDetailsError(eq(providerFirmId), eq(officeId), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, Boolean.TRUE);

        verify(soaGatewayServiceErrorHandler).handleContractDetailsError(eq(providerFirmId), eq(officeId), any(WebClientResponseException.class));

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

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(ClientDetails.class)).thenReturn(Mono.just(mockClientDetails));

        Mono<ClientDetails> clientDetailsMono = soaGatewayService.getClients(clientSearchCriteria, loginId, userType, page, size);

        StepVerifier.create(clientDetailsMono)
                .expectNextMatches(clientDetails -> clientDetails == mockClientDetails)
                .verifyComplete();

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        verify(soaGatewayWebClientMock).get();
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

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CaseDetails.class)).thenReturn(Mono.just(mockCaseDetails));

        Mono<CaseDetails> caseDetailsMono = soaGatewayService.getCases(copyCaseSearchCriteria, loginId, userType, page, size);

        StepVerifier.create(caseDetailsMono)
            .expectNextMatches(clientDetails -> clientDetails == mockCaseDetails)
            .verifyComplete();

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        verify(soaGatewayWebClientMock).get();
        verify(requestHeadersUriMock).uri(uriCaptor.capture());
        verify(requestHeadersMock).header("SoaGateway-User-Login-Id", loginId);
        verify(requestHeadersMock).header("SoaGateway-User-Role", userType);
        verify(requestHeadersMock).retrieve();
        verify(responseMock).bodyToMono(CaseDetails.class);

        assertEquals(expectedUri, actualUri.toString());
    }

    private static ContractDetail createContractDetail(String cat, Boolean createNewMatters, Boolean remainderAuth) {
        return new ContractDetail()
            .categoryofLaw(cat)
            .subCategory("SUBCAT1")
            .createNewMatters(createNewMatters)
            .remainderAuthorisation(remainderAuth)
            .contractualDevolvedPowers("CATDEVPOW")
            .authorisationType("AUTHTYPE1");
    }
}