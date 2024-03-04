package uk.gov.laa.ccms.caab.client;

/**
 * Represents exceptions that may occur while interacting with the CAAB API microservice.
 */
public class CaabApiClientException extends RuntimeException {

  /**
   * Constructs a new CAAB API client exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *        {@link #getMessage()} method.
   */
  public CaabApiClientException(final String message) {
    super(message);
  }

  /**
   * Constructs a new CAAB API client exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public CaabApiClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
