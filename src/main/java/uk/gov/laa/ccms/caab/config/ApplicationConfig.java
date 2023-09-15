package uk.gov.laa.ccms.caab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for creating WebClient instances used for making HTTP requests.
 */
@Configuration
public class ApplicationConfig {

  private final String ebsApiUrl;

  private final String soaApiUrl;

  private final String caabApiUrl;

  private final String osApiUrl;

  /**
   * Constructs the ApplicationConfig instance with API URLs.
   *
   * @param ebsApiUrl The URL of the data API.
   * @param soaApiUrl The URL of the SOA Gateway API.
   * @param caabApiUrl The URL of the CAAB API.
   */
  public ApplicationConfig(@Value("${laa.ccms.ebs-api.url}") String ebsApiUrl,
                           @Value("${laa.ccms.soa-api.url}") String soaApiUrl,
                           @Value("${laa.ccms.caab-api.url}") String caabApiUrl,
                           @Value("${os.api.url}") String osApiUrl) {
    this.ebsApiUrl = ebsApiUrl;
    this.soaApiUrl = soaApiUrl;
    this.caabApiUrl = caabApiUrl;
    this.osApiUrl = osApiUrl;
  }

  /**
   * Creates a WebClient bean for interacting with the Ebs API.
   *
   * @return A WebClient instance configured for the Ebs API URL.
   */
  @Bean("ebsApiWebClient")
  WebClient ebsApiWebClient() {
    return WebClient.create(ebsApiUrl);
  }

  /**
   * Creates a WebClient bean for interacting with the SOA API.
   *
   * @return A WebClient instance configured for the SOA API URL.
   */
  @Bean("soaApiWebClient")
  WebClient soaApiWebClient() {
    return WebClient.create(soaApiUrl);
  }

  /**
   * Creates a WebClient bean for interacting with the CAAB API.
   *
   * @return A WebClient instance configured for the CAAB API URL.
   */
  @Bean("caabApiWebClient")
  WebClient caabApiWebClient() {
    return WebClient.create(caabApiUrl);
  }

  /**
   * Creates a WebClient bean for interacting with the Ordinance Survey API.
   *
   * @return A WebClient instance configured for the Ordinance Survey API.
   */
  @Bean("osApiWebClient")
  WebClient osApiWebClient() {
    return WebClient.create(osApiUrl);
  }
}
