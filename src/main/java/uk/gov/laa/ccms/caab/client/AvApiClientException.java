package uk.gov.laa.ccms.caab.client;

/**
 * Custom exception class representing clamav antivirus client related exceptions.
 */
public class AvApiClientException extends RuntimeException {
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AvApiClientException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public AvApiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
