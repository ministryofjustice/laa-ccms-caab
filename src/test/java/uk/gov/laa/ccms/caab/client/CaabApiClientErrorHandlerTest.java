package uk.gov.laa.ccms.caab.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

class CaabApiClientErrorHandlerTest {

  private final CaabApiClientErrorHandler errorHandler = new CaabApiClientErrorHandler();

  @Test
  void handleApiCreateError_preservesConflictStatusForWebClientResponseException() {
    final WebClientResponseException exception =
        WebClientResponseException.create(
            HttpStatus.CONFLICT.value(), "Conflict", null, new byte[0], null);

    StepVerifier.create(errorHandler.handleApiCreateError(exception, "case outcome"))
        .expectErrorSatisfies(
            error -> {
              assertThat(error).isInstanceOf(CaabApiClientException.class);
              assertThat(((CaabApiClientException) error).hasHttpStatus(HttpStatus.CONFLICT))
                  .isTrue();
            })
        .verify();
  }

  @Test
  void handleApiCreateError_keepsCauseOnlyBehaviourForNonWebClientErrors() {
    final RuntimeException exception = new RuntimeException("boom");

    StepVerifier.create(errorHandler.handleApiCreateError(exception, "case outcome"))
        .expectErrorSatisfies(
            error -> {
              assertThat(error).isInstanceOf(CaabApiClientException.class);
              assertThat(((CaabApiClientException) error).hasHttpStatus(HttpStatus.CONFLICT))
                  .isFalse();
            })
        .verify();
  }
}
