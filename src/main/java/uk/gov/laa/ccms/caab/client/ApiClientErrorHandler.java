package uk.gov.laa.ccms.caab.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Provides error-handling capabilities for the API client interactions.
 */
@Slf4j
@Component
public class ApiClientErrorHandler {

  /**
   * Handles all caab api errors encountered.
   *
   * @param e the encountered error
   * @param message the message to log
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public <T> Mono<T> handleCaabApiError(
      final Throwable e,
      final String message) {
    log.error(message, e);
    return Mono.error(new CaabApiClientException(message, e));
  }

  /**
   * Handles all soa api errors encountered.
   *
   * @param e the encountered error
   * @param message the message to log
   * @return a Mono signaling the error wrapped in a {@code SoaApiClientException}
   */
  public <T> Mono<T> handleSoaApiError(
      final Throwable e,
      final String message) {
    log.error(message, e);
    return Mono.error(new SoaApiClientException(message, e));
  }

  /**
   * Handles all ebs api errors encountered.
   *
   * @param e the encountered error
   * @param message the message to log
   * @return a Mono signaling the error wrapped in a {@code EbsApiClientException}
   */
  public <T> Mono<T> handleEbsApiError(
      final Throwable e,
      final String message) {
    log.error(message, e);
    return Mono.error(new EbsApiClientException(message, e));
  }
}
