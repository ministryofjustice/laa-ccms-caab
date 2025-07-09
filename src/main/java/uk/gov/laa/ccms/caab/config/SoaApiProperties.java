package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection properties for the SOA API. */
@ConfigurationProperties(prefix = "laa.ccms.soa-api")
public class SoaApiProperties extends ApiProperties {

  public SoaApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
