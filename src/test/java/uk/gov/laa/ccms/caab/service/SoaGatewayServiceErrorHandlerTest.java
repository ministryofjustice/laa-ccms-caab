package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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
        ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
        clientSearchCriteria.setForename("John");
        clientSearchCriteria.setSurname("Doe");
        clientSearchCriteria.setDobYear("1990");
        clientSearchCriteria.setDobMonth("02");
        clientSearchCriteria.setDobDay("01");
        clientSearchCriteria.setUniqueIdentifierType(1);
        clientSearchCriteria.setUniqueIdentifierValue("ABC123");

        Throwable throwable = new RuntimeException("Error");

        Mono<ClientDetails> result = soaGatewayServiceErrorHandler.handleClientDetailsError(clientSearchCriteria, throwable);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

}