package uk.gov.laa.ccms.caab.client;

/** Custom exception class representing AWS S3 related exceptions. */
public class S3ApiClientException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public S3ApiClientException(final String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public S3ApiClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
