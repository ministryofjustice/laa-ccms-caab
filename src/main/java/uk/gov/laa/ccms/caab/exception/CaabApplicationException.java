package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class for errors originating in the CAAB's Controllers.
 */
public class CaabApplicationException extends RuntimeException {

  public CaabApplicationException(String message) {
    super(message);
  }

  public CaabApplicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
