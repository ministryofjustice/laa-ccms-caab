package uk.gov.laa.ccms.caab.service;

/**
 * Exception class representing errors related to the SoaGatewayService.
 */
public class SoaGatewayServiceException extends RuntimeException {
  public SoaGatewayServiceException(String message) {
    super(message);
  }

  public SoaGatewayServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}