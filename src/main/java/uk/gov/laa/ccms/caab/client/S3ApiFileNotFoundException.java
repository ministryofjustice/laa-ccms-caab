package uk.gov.laa.ccms.caab.client;

/**
 * Custom exception class to indicate that the requested file was not found in S3.
 */
public class S3ApiFileNotFoundException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public S3ApiFileNotFoundException(final String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public S3ApiFileNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
