package uk.gov.laa.ccms.caab.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Provides error-handling capabilities for the User Details API client interactions. */
@Component
public class UserDetailsApiClientErrorHandler extends AbstractApiClientErrorHandler {

  @Override
  public UserDetailsApiClientException createException(
      final String message, final Throwable cause) {
    return new UserDetailsApiClientException(message, cause);
  }

  @Override
  public UserDetailsApiClientException createException(String message, HttpStatus httpStatus) {
    return new UserDetailsApiClientException(message, httpStatus);
  }
}
