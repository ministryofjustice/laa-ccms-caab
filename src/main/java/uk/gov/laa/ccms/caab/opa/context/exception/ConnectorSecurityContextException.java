package uk.gov.laa.ccms.caab.opa.context.exception;

/** Exception thrown when there is a security context issue in the connector. */
public class ConnectorSecurityContextException extends Exception {

  /** Constructs a new exception with {@code null} as its detail message. */
  public ConnectorSecurityContextException() {}

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public ConnectorSecurityContextException(final String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public ConnectorSecurityContextException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public ConnectorSecurityContextException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
