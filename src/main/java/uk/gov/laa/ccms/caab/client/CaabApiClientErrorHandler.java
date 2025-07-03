package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Provides error-handling capabilities for the CAAB API client interactions. */
@Slf4j
@Component
public class CaabApiClientErrorHandler extends AbstractApiClientErrorHandler {
  @Override
  public CaabApiClientException createException(final String message, final Throwable cause) {
    return new CaabApiClientException(message, cause);
  }

  @Override
  public CaabApiClientException createException(String message, HttpStatus httpStatus) {
    return new CaabApiClientException(message, httpStatus);
  }
}
