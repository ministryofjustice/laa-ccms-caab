package uk.gov.laa.ccms.caab.exception;

/**
 * RuntimeException class to indicate that the antivirus scanning service is not enabled.
 */
public class AvScanNotEnabledException extends CaabApplicationException {
  /**
   * Constructs a new exception.
   *
   */
  public AvScanNotEnabledException() {
    super();
  }

}
