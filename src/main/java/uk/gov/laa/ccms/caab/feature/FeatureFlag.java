package uk.gov.laa.ccms.caab.feature;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes the status of an individual feature.
 */
@Getter
@Setter
@EqualsAndHashCode
public class FeatureFlag {

  private Feature feature;
  private Boolean enabled;

  public boolean isEnabled() {
    return Boolean.TRUE.equals(enabled);
  }
}
