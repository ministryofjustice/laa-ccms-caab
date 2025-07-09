package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection properties for the EBS API. */
@ConfigurationProperties(prefix = "laa.ccms.ebs-api")
public class EbsApiProperties extends ApiProperties {

  public EbsApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
