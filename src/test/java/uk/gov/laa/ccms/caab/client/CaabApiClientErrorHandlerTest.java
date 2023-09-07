package uk.gov.laa.ccms.caab.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CaabApiClientErrorHandlerTest {

  @InjectMocks
  private CaabApiClientErrorHandler caabApiClientErrorHandler;

  @Test
  public void testHandleCreateApplicationError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<Void> result = caabApiClientErrorHandler.handleCreateApplicationError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to create application")
            && e.getCause() == throwable);
  }
}