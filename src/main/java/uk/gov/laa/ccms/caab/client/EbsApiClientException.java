package uk.gov.laa.ccms.caab.client;

/**
 * Custom exception class representing ebs-api client related exceptions.
 */
public class EbsApiClientException extends RuntimeException {
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public EbsApiClientException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public EbsApiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}