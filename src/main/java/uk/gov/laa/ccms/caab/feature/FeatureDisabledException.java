package uk.gov.laa.ccms.caab.feature;

import lombok.Getter;

/**
 * Exception raised when an attempt was made to access a disabled feature.
 */
@Getter
public class FeatureDisabledException extends RuntimeException {

  private final Feature feature;

  public FeatureDisabledException(Feature feature) {
    super("The '%s' feature is currently disabled.".formatted(feature.getName()));
    this.feature = feature;
  }
}
