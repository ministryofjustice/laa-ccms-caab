package uk.gov.laa.ccms.caab.feature;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Controller advice class responsible for controlling access to features depending on whether
 * they have been enabled or not.
 */
@Service
public class FeatureService {

  private final String enabledFeatures;

  private static final String FEATURE_DELIMITER = ",";

  private final Map<String, Boolean> featureMap = new HashMap<>();

  private FeatureService(
      @Value("${laa.ccms.enabled-features}") String enabledFeatures) {
    this.enabledFeatures = enabledFeatures;
  }

  /**
   * Initialise the enabled features.
   */
  @PostConstruct
  public void init() {
    // Disable all features by default
    Arrays.stream(Feature.values()).toList()
        .forEach(feature -> featureMap.put(feature.getName(), false));

    List<String> features = Arrays.stream(enabledFeatures.split(FEATURE_DELIMITER))
        .map(String::trim)
        .toList();

    validateFeatures(features);

    features.forEach(feature -> featureMap.put(feature, true));
  }

  /**
   * Validate whether a list of feature names are in the accepted list.
   *
   * @param features the list of feature names to validate.
   */
  private void validateFeatures(List<String> features) {
    List<String> invalidFeatures = features.stream()
        .filter(feature -> !feature.isBlank())
        .filter(feature -> !featureMap.containsKey(feature))
        .toList();

    if (!invalidFeatures.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Invalid feature flags provided: %s. "
                  + "Please only use accepted feature flags: %s",
                  invalidFeatures, featureMap.keySet()));
    }
  }

  /**
   * Check whether a feature is enabled.
   *
   * @param feature the feature to check.
   * @return true if enabled, false otherwise.
   */
  public boolean isEnabled(Feature feature) {
    return featureMap.getOrDefault(feature.getName(), false);
  }

}
