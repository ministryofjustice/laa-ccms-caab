package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class for errors originating in the CAAB's Controllers.
 */
public class CaabApplicationException extends RuntimeException {

  /**
   * Constructs a new exception.
   *
   */
  public CaabApplicationException() {
    super();
  }

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message The error message.
   */
  public CaabApplicationException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message The error message.
   * @param cause The cause of the exception.
   */
  public CaabApplicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
