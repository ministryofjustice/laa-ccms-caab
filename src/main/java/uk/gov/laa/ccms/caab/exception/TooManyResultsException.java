package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class for indicating that too many results have been returned from a SOA
 * request.
 */
public class TooManyResultsException extends CaabApplicationException {
  /**
   * Constructs a new exception with the specified message.
   *
   * @param message The error message.
   */
  public TooManyResultsException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message The error message.
   * @param cause The cause of the exception.
   */
  public TooManyResultsException(String message, Throwable cause) {
    super(message, cause);
  }
}
