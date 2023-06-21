package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.UserResponse;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        String expectedUri = "/users/{loginId}/notifications/summary";

        NotificationSummary mockSummary = new NotificationSummary()
                .notifications(10)
                .standardActions(5)
                .overdueActions(2);

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", "EXTERNAL")).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.just(mockSummary));

        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId);

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
        String expectedUri = "/users/{loginId}/notifications/summary";

        when(soaGatewayWebClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header("SoaGateway-User-Role", "EXTERNAL")).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(soaGatewayServiceErrorHandler.handleNotificationSummaryError(eq(loginId), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId);

        StepVerifier.create(summaryMono)
                .verifyComplete();
    }

}