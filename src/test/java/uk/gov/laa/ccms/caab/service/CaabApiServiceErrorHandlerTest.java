package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CaabApiServiceErrorHandlerTest {

    @InjectMocks
    private CaabApiServiceErrorHandler caabApiServiceErrorHandler;

    @BeforeEach
    public void setUp() {
        caabApiServiceErrorHandler = new CaabApiServiceErrorHandler();
    }

    @Test
    public void testHandleCreateApplicationError() {
        Throwable throwable = new RuntimeException("Error");

        Mono<Void> result = caabApiServiceErrorHandler.handleCreateApplicationError(throwable);

        StepVerifier.create(result)
                .verifyErrorMatches(e -> e instanceof CaabApiServiceException
                        && e.getMessage().equals("Failed to create application")
                        && e.getCause() == throwable);
    }
}