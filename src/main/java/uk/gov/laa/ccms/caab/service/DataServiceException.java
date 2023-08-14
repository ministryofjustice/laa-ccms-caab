package uk.gov.laa.ccms.caab.service;

/**
 * Custom exception class representing data service related exceptions.
 */
public class DataServiceException extends RuntimeException {
  /**
   * Constructs a new data service exception with the specified detail message.
   *
   * @param message the detail message
   */
  public DataServiceException(String message) {
    super(message);
  }

  /**
   * Constructs a new data service exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public DataServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}