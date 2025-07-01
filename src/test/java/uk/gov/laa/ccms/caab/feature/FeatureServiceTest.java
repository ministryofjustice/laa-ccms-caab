package uk.gov.laa.ccms.caab.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class FeatureServiceTest {

  @Mock FeatureProperties featureProperties;

  @InjectMocks FeatureService featureService;

  @ParameterizedTest
  @CsvSource(
      value = {"true, true", "false, false", "null, false"},
      nullValues = "null")
  @DisplayName("isEnabled correctly returns the flag value")
  void isEnabledCorrectlyReturnsFlagValue(Boolean flagEnabled, boolean expectedFlagValue) {
    Feature feature = mock(Feature.class);
    when(feature.getName()).thenReturn("feature");

    FeatureFlag featureFlag = new FeatureFlag();
    featureFlag.setFeature(feature);
    featureFlag.setEnabled(flagEnabled);
    List<FeatureFlag> featureFlags = Collections.singletonList(featureFlag);

    when(featureProperties.getFeatures()).thenReturn(featureFlags);

    boolean isEnabled = featureService.isEnabled(feature);

    assertEquals(expectedFlagValue, isEnabled);
  }

  @Test
  @DisplayName("featureRequired does not throw an exception when the given feature is enabled")
  void featureRequiredDoesNotThrowExceptionWhenFeatureIsEnabled() {
    Feature feature = mock(Feature.class);
    when(feature.getName()).thenReturn("feature");

    FeatureFlag featureFlag = new FeatureFlag();
    featureFlag.setFeature(feature);
    featureFlag.setEnabled(true);
    List<FeatureFlag> featureFlags = Collections.singletonList(featureFlag);

    when(featureProperties.getFeatures()).thenReturn(featureFlags);

    featureService.featureRequired(feature, () -> Boolean.FALSE);
  }

  @Test
  @DisplayName(
      "featureRequired does not throw an exception when the given feature is disabled, "
          + "but the provided condition is not met")
  void featureRequiredDoesNotThrowExceptionWhenFeatureIsDisabledButConditionIsNotMet() {
    Feature feature = mock(Feature.class);
    when(feature.getName()).thenReturn("feature");

    FeatureFlag featureFlag = new FeatureFlag();
    featureFlag.setFeature(feature);
    featureFlag.setEnabled(false);
    List<FeatureFlag> featureFlags = Collections.singletonList(featureFlag);

    when(featureProperties.getFeatures()).thenReturn(featureFlags);

    featureService.featureRequired(feature, () -> Boolean.FALSE);
  }

  @Test
  @DisplayName(
      "featureRequired throws an exception when the given feature is disabled and the"
          + "condition is met")
  void featureRequiredThrowsExceptionWhenFeatureIsDisabledAndConditionIsMet() {
    Feature feature = mock(Feature.class);
    when(feature.getName()).thenReturn("feature");

    FeatureFlag featureFlag = new FeatureFlag();
    featureFlag.setFeature(feature);
    featureFlag.setEnabled(false);
    List<FeatureFlag> featureFlags = Collections.singletonList(featureFlag);

    when(featureProperties.getFeatures()).thenReturn(featureFlags);

    assertThrows(
        FeatureDisabledException.class,
        () -> featureService.featureRequired(feature, () -> Boolean.TRUE),
        "Expected FeatureDisabledException to be thrown, but it wasn't.");
  }
}
