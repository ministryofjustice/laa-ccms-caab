package uk.gov.laa.ccms.caab.feature;

import java.util.Arrays;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Controller advice class responsible for controlling access to features depending on whether they
 * have been enabled or not.
 */
@Slf4j
@Service
@EnableConfigurationProperties(FeatureProperties.class)
public final class FeatureService {

  private final FeatureProperties featureProperties;

  /**
   * Construct the feature service.
   *
   * @param featureProperties the list of configured features and their status (enabled / disabled).
   */
  private FeatureService(FeatureProperties featureProperties) {
    this.featureProperties = featureProperties;
    logFeatureInitialization();
  }

  /** Log the status of each feature. */
  private void logFeatureInitialization() {
    Arrays.stream(Feature.values())
        .forEach(
            feature ->
                log.info(
                    "{} feature: {}",
                    feature.getName(),
                    isEnabled(feature) ? "enabled" : "disabled"));
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

  /**
   * Checks whether a given feature is disabled and a condition is met. If so, throw a {@link
   * FeatureDisabledException} exception.
   *
   * @param feature The feature to check.
   * @param conditionSupplier The condition to check.
   */
  public void featureRequired(Feature feature, Supplier<Boolean> conditionSupplier) {

    if (!isEnabled(feature) && Boolean.TRUE.equals(conditionSupplier.get())) {
      throw new FeatureDisabledException(feature);
    }
  }
}
