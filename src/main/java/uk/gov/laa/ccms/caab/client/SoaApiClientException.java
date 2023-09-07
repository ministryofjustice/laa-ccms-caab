package uk.gov.laa.ccms.caab.client;

/**
 * Exception class representing errors related to the SoaApiClient.
 */
public class SoaApiClientException extends RuntimeException {
  public SoaApiClientException(String message) {
    super(message);
  }

  public SoaApiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}