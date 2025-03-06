package uk.gov.laa.ccms.caab.feature;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "laa.ccms")
public class FeatureProperties {

  List<FeatureFlag> features;

}
