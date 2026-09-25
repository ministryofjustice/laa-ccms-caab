package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/** Provides error-handling capabilities for the EBS API client interactions. */
@Component
@Slf4j
public class EbsApiClientErrorHandler extends AbstractApiClientErrorHandler {

  public <T> Mono<T> handleTargetedUserRetrieveError(Throwable error) {
    String message = "Failed to retrieve provider user by login ID";
    if (error instanceof WebClientResponseException responseException) {
      HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
      log.error("{} (HTTP {})", message, responseException.getStatusCode().value());
      return Mono.error(new EbsApiClientException(message, status));
    }
    log.error(message);
    return Mono.error(new EbsApiClientException(message));
  }

  @Override
  public EbsApiClientException createException(final String message, final Throwable cause) {
    return new EbsApiClientException(message, cause);
  }

  @Override
  public EbsApiClientException createException(String message, HttpStatus httpStatus) {
    return new EbsApiClientException(message, httpStatus);
  }
}
