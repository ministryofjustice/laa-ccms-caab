package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection properties for the Assessment API.
 */
@ConfigurationProperties(prefix = "laa.ccms.assessment-api")
public class AssessmentApiProperties extends ApiProperties {

  public AssessmentApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }

}
