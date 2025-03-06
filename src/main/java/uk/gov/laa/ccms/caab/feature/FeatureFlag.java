package uk.gov.laa.ccms.caab.feature;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureFlag {

  private Feature feature;
  private Boolean enabled;

  public boolean isEnabled() {
    return Boolean.TRUE.equals(enabled);
  }

}
