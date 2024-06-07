package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class to report a non-clean response from an external anti virus
 * scanning service.
 */
public class AvVirusFoundException extends CaabApplicationException {
  /**
   * Constructs a new exception with the specified message.
   *
   * @param message The error message.
   */
  public AvVirusFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message The error message.
   * @param cause The cause of the exception.
   */
  public AvVirusFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
