package uk.gov.laa.ccms.caab.client;


/**
 * Represents exceptions that may occur while interacting with the Assessment API microservice.
 */
public class AssessmentApiClientException extends RuntimeException {

  /**
   * Constructs a new Assessment API client exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *        {@link #getMessage()} method.
   */
  public AssessmentApiClientException(final String message) {
    super(message);
  }

  /**
   * Constructs a new Assessment API client exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public AssessmentApiClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
