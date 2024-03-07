package uk.gov.laa.ccms.caab.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

/**
 * Configuration class for creating WebClient instances used for making HTTP requests.
 */
@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

  private final String ebsApiUrl;

  private final String soaApiUrl;

  private final String caabApiUrl;

  private final String osApiUrl;

  private final LoggingInterceptor loggingInterceptor;


  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loggingInterceptor);
  }

  /**
   * Constructs the ApplicationConfig instance with API URLs.
   *
   * @param ebsApiUrl          The URL of the data API.
   * @param soaApiUrl          The URL of the SOA Gateway API.
   * @param caabApiUrl         The URL of the CAAB API.
   * @param osApiUrl           The URL of the ordinance survey API.
   * @param loggingInterceptor A logging interceptor for the caab.
   */
  public ApplicationConfig(@Value("${laa.ccms.ebs-api.url}") String ebsApiUrl,
                           @Value("${laa.ccms.soa-api.url}") String soaApiUrl,
                           @Value("${laa.ccms.caab-api.url}") String caabApiUrl,
                           @Value("${os.api.url}") String osApiUrl,
                           LoggingInterceptor loggingInterceptor) {
    this.ebsApiUrl = ebsApiUrl;
    this.soaApiUrl = soaApiUrl;
    this.caabApiUrl = caabApiUrl;
    this.osApiUrl = osApiUrl;
    this.loggingInterceptor = loggingInterceptor;
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

  /**
   * Creates a LocaleResolver bean for setting the default locale to UK.
   *
   * @return A LocaleResolver instance configured for the UK locale.
   */
  @Bean
  public LocaleResolver localeResolver() {
    final FixedLocaleResolver localeResolver = new FixedLocaleResolver();
    localeResolver.setDefaultLocale(Locale.UK);
    return localeResolver;
  }
}
