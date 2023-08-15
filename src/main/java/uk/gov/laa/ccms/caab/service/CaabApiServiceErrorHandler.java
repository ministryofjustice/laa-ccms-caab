package uk.gov.laa.ccms.caab.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Provides error-handling capabilities for the CAAB API service interactions.
 */
@Slf4j
@Component
public class CaabApiServiceErrorHandler {

  /**
   * Handles errors encountered during application creation.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Void> handleCreateApplicationError(Throwable e) {
    final String msg = "Failed to create application";
    log.error(msg, e);
    return Mono.error(new CaabApiServiceException(msg, e));
  }
}
