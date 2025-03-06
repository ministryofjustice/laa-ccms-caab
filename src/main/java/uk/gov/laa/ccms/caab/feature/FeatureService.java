package uk.gov.laa.ccms.caab.feature;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Controller advice class responsible for controlling access to features depending on whether
 * they have been enabled or not.
 */
@Service
@EnableConfigurationProperties(FeatureProperties.class)
public class FeatureService {

  private final FeatureProperties featureProperties;

  private FeatureService(
      FeatureProperties featureProperties) {
    this.featureProperties = featureProperties;
  }

  /**
   * Check whether a feature is enabled.
   *
   * @param feature the feature to check.
   * @return true if enabled, false otherwise.
   */
  public boolean isEnabled(Feature feature) {
    return featureProperties.getFeatures().stream()
        .filter(f -> f.getFeature().getName().equals(feature.getName()))
        .findFirst()
        .map(FeatureFlag::isEnabled)
        .orElse(false);
  }

}
