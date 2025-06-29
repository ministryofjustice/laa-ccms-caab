package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection properties for the Caab API. */
@ConfigurationProperties(prefix = "laa.ccms.caab-api")
public class CaabApiProperties extends ApiProperties {

  public CaabApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
