package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection properties for the User Details API. */
@ConfigurationProperties(prefix = "laa.ccms.user-details-api")
public class UserDetailsApiProperties extends ApiProperties {

  public UserDetailsApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
