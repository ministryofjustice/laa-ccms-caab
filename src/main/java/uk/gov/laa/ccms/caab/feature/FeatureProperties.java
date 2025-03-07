package uk.gov.laa.ccms.caab.feature;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The list of configured features and their status (enabled / disabled).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "laa.ccms")
public class FeatureProperties {

  List<FeatureFlag> features;

}
