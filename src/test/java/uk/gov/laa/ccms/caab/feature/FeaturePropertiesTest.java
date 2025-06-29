package uk.gov.laa.ccms.caab.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {FeatureProperties.class})
@EnableConfigurationProperties(FeatureProperties.class)
public class FeaturePropertiesTest {

  @Nested
  @TestPropertySource(
      properties = {"laa.ccms.features[0].feature=amendments", "laa.ccms.features[0].enabled=true"})
  class FeaturePropertiesSetTrueFlagCorrectly {

    @Autowired private FeatureProperties featureProperties;

    @Test
    @DisplayName("Feature properties correctly set a true value")
    void featurePropertiesLoadsCorrectly() {

      FeatureFlag expected = new FeatureFlag();
      expected.setFeature(Feature.AMENDMENTS);
      expected.setEnabled(true);

      List<FeatureFlag> actualFeatures = featureProperties.getFeatures();

      assertEquals(1, actualFeatures.size());
      assertEquals(expected, actualFeatures.getFirst());
    }
  }

  @Nested
  @TestPropertySource(
      properties = {
        "laa.ccms.features[0].feature=amendments",
        "laa.ccms.features[0].enabled=false"
      })
  class FeaturePropertiesSetFalseFlagCorrectly {

    @Autowired private FeatureProperties featureProperties;

    @Test
    @DisplayName("Feature properties correctly set a false value")
    void featurePropertiesLoadsCorrectly() {

      FeatureFlag expected = new FeatureFlag();
      expected.setFeature(Feature.AMENDMENTS);
      expected.setEnabled(false);

      List<FeatureFlag> actualFeatures = featureProperties.getFeatures();

      assertEquals(1, actualFeatures.size());
      assertEquals(expected, actualFeatures.getFirst());
    }
  }

  @Nested
  class FeaturePropertiesHandleMissingProperties {

    @Autowired private FeatureProperties featureProperties;

    @Test
    @DisplayName("Feature properties handle missing properties")
    void featurePropertiesHandlesMissingProperties() {

      assertNull(featureProperties.getFeatures());
    }
  }
}
