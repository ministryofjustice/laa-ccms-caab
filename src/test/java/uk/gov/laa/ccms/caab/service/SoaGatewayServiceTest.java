package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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
        String expectedUri = "/contract-details?providerFirmId={providerFirmId}&officeId={officeId}";

        ContractDetails contractDetails = new ContractDetails()
            .addContractItem(
                createContractDetail("CAT1", true, true))
            .addContractItem(
                createContractDetail("CAT2", true, true));

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, providerFirmId, officeId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.just(contractDetails));

        List<String> response = soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, Boolean.TRUE);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("CAT1", response.get(0));
        assertEquals("CAT2", response.get(1));
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
        String expectedUri = "/contract-details?providerFirmId={providerFirmId}&officeId={officeId}";

        ContractDetails contractDetails = new ContractDetails()
            .addContractItem(
                createContractDetail(cat1, newMatters1, remAuth1))
            .addContractItem(
                createContractDetail(cat2, newMatters2, remAuth2));

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, providerFirmId, officeId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", userType)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(ContractDetails.class)).thenReturn(Mono.just(contractDetails));

        List<String> response = soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, initialApp);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(cat2, response.get(0));
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