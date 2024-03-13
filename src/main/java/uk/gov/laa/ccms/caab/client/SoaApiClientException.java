package uk.gov.laa.ccms.caab.client;

/**
 * Exception class representing errors related to the SoaApiClient.
 */
public class SoaApiClientException extends RuntimeException {
  public SoaApiClientException(final String message) {
    super(message);
  }

  public SoaApiClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}