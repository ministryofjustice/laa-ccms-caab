package uk.gov.laa.ccms.caab.client;

/**
 * Custom exception class to indicate that the ClamAV service call has
 * returned a non-clean response to its scan.
 */
public class AvApiVirusFoundException extends AvApiClientException {
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AvApiVirusFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public AvApiVirusFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}