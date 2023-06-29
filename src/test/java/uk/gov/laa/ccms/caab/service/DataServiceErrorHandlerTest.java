package uk.gov.laa.ccms.caab.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.UserResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataServiceErrorHandlerTest {

    @InjectMocks
    private DataServiceErrorHandler dataServiceErrorHandler;

    @BeforeEach
    public void setUp() {
        dataServiceErrorHandler = new DataServiceErrorHandler();
    }

    @Test
    public void testHandleUserError() {
        Throwable throwable = new RuntimeException("Error");

        Mono<UserResponse> result = dataServiceErrorHandler.handleUserError("testLoginId", throwable);

        StepVerifier.create(result)
                .verifyErrorMatches(e -> e instanceof DataServiceException
                        && e.getMessage().equals("Failed to retrieve User with loginId: testLoginId")
                        && e.getCause() == throwable);
    }
}
