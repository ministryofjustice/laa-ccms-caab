package uk.gov.laa.ccms.caab.service;

/**
 * Represents exceptions that may occur while interacting with the CAAB API service.
 */
public class CaabApiServiceException extends RuntimeException {

  /**
   * Constructs a new CAAB API service exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *        {@link #getMessage()} method.
   */
  public CaabApiServiceException(String message) {
    super(message);
  }

  /**
   * Constructs a new CAAB API service exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public CaabApiServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
