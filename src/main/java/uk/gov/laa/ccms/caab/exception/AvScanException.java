package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class to report an error response from an external anti virus scanning service.
 */
public class AvScanException extends CaabApplicationException {
  /**
   * Constructs a new exception with the specified message.
   *
   * @param message The error message.
   */
  public AvScanException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message The error message.
   * @param cause The cause of the exception.
   */
  public AvScanException(String message, Throwable cause) {
    super(message, cause);
  }
}
