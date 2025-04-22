package uk.gov.laa.ccms.caab.client;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class representing ebs-api client related exceptions.
 */
public class EbsApiClientException extends ApiClientException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public EbsApiClientException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public EbsApiClientException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified detail message and http status
   * from the response.
   *
   * @param message the detail message
   * @param httpStatus the http status of the response
   */
  public EbsApiClientException(String message, HttpStatus httpStatus) {
    super(message, httpStatus);
  }
}