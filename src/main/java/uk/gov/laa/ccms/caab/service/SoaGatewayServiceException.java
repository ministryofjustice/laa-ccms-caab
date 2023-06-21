package uk.gov.laa.ccms.caab.service;

public class SoaGatewayServiceException extends RuntimeException {
  public SoaGatewayServiceException(String message) {
    super(message);
  }

  public SoaGatewayServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}