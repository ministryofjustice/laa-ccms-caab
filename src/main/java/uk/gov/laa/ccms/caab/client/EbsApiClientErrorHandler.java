package uk.gov.laa.ccms.caab.client;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/** Provides error-handling capabilities for the EBS API client interactions. */
@Component
@Slf4j
public class EbsApiClientErrorHandler extends AbstractApiClientErrorHandler {

  /** Classifies expected lookup failures; callers decide whether the read is optional. */
  public static boolean isRecoverableInvoiceAssessmentFailure(final EbsApiClientException error) {
    if (error.getHttpStatus() != null) {
      return !error.hasHttpStatus(HttpStatus.UNAUTHORIZED)
          && !error.hasHttpStatus(HttpStatus.FORBIDDEN);
    }
    return error.getCause() instanceof WebClientRequestException
        || error.getCause() instanceof DecodingException
        || error.getCause() instanceof IOException
        || error.getCause() instanceof DataBufferLimitException
        || error.getCause() instanceof UnsupportedMediaTypeException;
  }

  /** Retains failure classification without logging assessment payloads or case identifiers. */
  public <T> Mono<T> handleInvoiceAssessmentRetrieveError(final Throwable error) {
    final String message = "Failed to retrieve retained invoice assessment";
    if (error instanceof WebClientResponseException responseException) {
      HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
      if (status == null && responseException.getStatusCode().is5xxServerError()) {
        status = HttpStatus.BAD_GATEWAY;
      } else if (status == null && responseException.getStatusCode().is4xxClientError()) {
        status = HttpStatus.BAD_REQUEST;
      }
      log.error("{} (HTTP {})", message, responseException.getStatusCode().value());
      return Mono.error(new InvoiceAssessmentReadException(message, status));
    }
    log.error(message);
    return Mono.error(new InvoiceAssessmentReadException(message, error));
  }

  /**
   * Handles targeted user lookup failures without exposing the login ID from the request or error.
   *
   * @param error the lookup failure
   * @param <T> the expected response type
   * @return an error with safe context and HTTP status when available
   */
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
