package uk.gov.laa.ccms.caab.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Provides error-handling capabilities for the CAAB API client interactions.
 */
@Slf4j
@Component
public class CaabApiClientErrorHandler {

  /**
   * Handles errors encountered during application creation.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Void> handleCreateApplicationError(Throwable e) {
    final String msg = "Failed to create application";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }
}
