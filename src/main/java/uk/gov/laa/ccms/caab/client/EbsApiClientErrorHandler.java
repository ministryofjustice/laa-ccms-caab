package uk.gov.laa.ccms.caab.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Provides error-handling capabilities for the EBS API client interactions. */
@Component
public class EbsApiClientErrorHandler extends AbstractApiClientErrorHandler {

  @Override
  public EbsApiClientException createException(final String message, final Throwable cause) {
    return new EbsApiClientException(message, cause);
  }

  @Override
  public EbsApiClientException createException(String message, HttpStatus httpStatus) {
    return new EbsApiClientException(message, httpStatus);
  }
}
