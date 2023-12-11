package uk.gov.laa.ccms.caab.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;

@ExtendWith(MockitoExtension.class)
class CaabApiClientErrorHandlerTest {

  @InjectMocks
  private CaabApiClientErrorHandler caabApiClientErrorHandler;

  @Test
  public void testHandleCreateApplicationError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<String> result = caabApiClientErrorHandler.handleCreateApplicationError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to create application")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleGetApplicationError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<ApplicationDetail> result = caabApiClientErrorHandler.handleGetApplicationError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to retrieve application")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleGetApplicationTypeError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<ApplicationType> result = caabApiClientErrorHandler.handleGetApplicationTypeError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to retrieve application type")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleGetProviderDetailsError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<ApplicationProviderDetails> result = caabApiClientErrorHandler.handleGetProviderDetailsError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to retrieve provider details")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandlePatchApplicationError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<Void> result = caabApiClientErrorHandler.handlePatchApplicationError(throwable, "type");

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof CaabApiClientException
            && e.getMessage().equals("Failed to patch application - type")
            && e.getCause() == throwable);
  }
}