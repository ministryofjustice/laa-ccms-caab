package uk.gov.laa.ccms.caab.client;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A generic API client exception providing http status if available. */
@Getter
public class ApiClientException extends RuntimeException {
  HttpStatus httpStatus;

  /**
   * Checks whether the exception has the provided http status.
   *
   * @param status the status to compare.
   * @return true if the status matches, false otherwise.
   */
  public boolean hasHttpStatus(HttpStatus status) {
    return this.httpStatus != null && this.httpStatus.equals(status);
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public ApiClientException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public ApiClientException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified detail message and http status from the response.
   *
   * @param message the detail message
   * @param httpStatus the http status of the response
   */
  public ApiClientException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }
}
