package uk.gov.laa.ccms.caab.feature;

/**
 * Exception raised when an attempt was made to access a disabled feature.
 */
public class FeatureDisabledException extends RuntimeException {

  public FeatureDisabledException(String message) {
    super(message);
  }
}
