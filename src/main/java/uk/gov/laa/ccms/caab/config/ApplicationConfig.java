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

  private final String dataApiUrl;

  private final String soaGatewayApiUrl;

  private final String caabApiUrl;

  /**
   * Constructs the ApplicationConfig instance with API URLs.
   *
   * @param dataApiUrl The URL of the data API.
   * @param soaGatewayApiUrl The URL of the SOA Gateway API.
   * @param caabApiUrl The URL of the CAAB API.
   */
  public ApplicationConfig(@Value("${laa.ccms.data-api.url}") String dataApiUrl,
                           @Value("${laa.ccms.soa-gateway-api.url}") String soaGatewayApiUrl,
                           @Value("${laa.ccms.caab-api.url}") String caabApiUrl) {
    this.dataApiUrl = dataApiUrl;
    this.soaGatewayApiUrl = soaGatewayApiUrl;
    this.caabApiUrl = caabApiUrl;
  }

  /**
   * Creates a WebClient bean for interacting with the data API.
   *
   * @return A WebClient instance configured for the data API URL.
   */
  @Bean("dataWebClient")
  WebClient dataWebClient() {
    return WebClient.create(dataApiUrl);
  }

  /**
   * Creates a WebClient bean for interacting with the SOA Gateway API.
   *
   * @return A WebClient instance configured for the SOA Gateway API URL.
   */
  @Bean("soaGatewayWebClient")
  WebClient soaGatewayWebClient() {
    return WebClient.create(soaGatewayApiUrl);
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
}
