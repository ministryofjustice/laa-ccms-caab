package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.UserResponse;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class SoaGatewayServiceErrorHandlerTest {

    @InjectMocks
    private SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler;

    @Mock
    private Logger loggerMock;

    @BeforeEach
    public void setUp() {
        soaGatewayServiceErrorHandler = new SoaGatewayServiceErrorHandler();
    }

    @Test
    public void testHandleNotificationSummaryError() {
        String loginId = "testLoginId";
        Throwable throwable = new RuntimeException("Error");

        Mono<NotificationSummary> result = soaGatewayServiceErrorHandler.handleNotificationSummaryError(loginId, throwable);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

}