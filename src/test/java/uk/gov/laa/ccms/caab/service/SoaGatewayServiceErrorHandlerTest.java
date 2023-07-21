package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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

    @Test
    public void testHandleContractDetailsError() {
        Integer providerFirmId = 123;
        Integer officeId = 4567;
        Throwable throwable = new RuntimeException("Error");

        Mono<ContractDetails> result = soaGatewayServiceErrorHandler.handleContractDetailsError(providerFirmId, officeId, throwable);

        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    public void testHandleClientDetailsError() {
        ClientSearchDetails clientSearchDetails = new ClientSearchDetails();
        clientSearchDetails.setForename("John");
        clientSearchDetails.setSurname("Doe");
        clientSearchDetails.setDobYear("1990");
        clientSearchDetails.setDobMonth("02");
        clientSearchDetails.setDobDay("01");
        clientSearchDetails.setUniqueIdentifierType(1);
        clientSearchDetails.setUniqueIdentifierValue("ABC123");

        Throwable throwable = new RuntimeException("Error");

        Mono<ClientDetails> result = soaGatewayServiceErrorHandler.handleClientDetailsError(clientSearchDetails, throwable);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

}