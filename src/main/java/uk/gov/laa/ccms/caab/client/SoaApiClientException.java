package uk.gov.laa.ccms.caab.client;

import org.springframework.http.HttpStatus;

/**
 * Exception class representing errors related to the SoaApiClient.
 */
public class SoaApiClientException extends ApiClientException {

  public SoaApiClientException(final String message) {
    super(message);
  }

  public SoaApiClientException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified detail message and http status
   * from the response.
   *
   * @param message the detail message
   * @param httpStatus the http status of the response
   */
  public SoaApiClientException(String message, HttpStatus httpStatus) {
    super(message, httpStatus);
  }
}
