package uk.gov.laa.ccms.caab.config;

import fi.solita.clamav.ClamAVClient;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import uk.gov.laa.ccms.caab.util.UserRoleUtil;

/**
 * Configuration class for creating WebClient instances used for making HTTP requests.
 */
@Configuration
@EnableConfigurationProperties({AssessmentApiProperties.class, CaabApiProperties.class,
    EbsApiProperties.class, SoaApiProperties.class})
public class ApplicationConfig implements WebMvcConfigurer {

  private final EbsApiProperties ebsApiProperties;

  private final SoaApiProperties soaApiProperties;

  private final CaabApiProperties caabApiProperties;

  private final AssessmentApiProperties assessmentApiProperties;

  private final String osApiUrl;

  private final String avApiHostName;

  private final Integer avApiPort;

  private final Integer avApiTimeout;

  private final LoggingInterceptor loggingInterceptor;


  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loggingInterceptor);
  }

  /**
   * Constructs the ApplicationConfig instance with API URLs.
   *
   * @param ebsApiProperties        The connection details for the data API.
   * @param soaApiProperties        The connection details for the SOA Gateway API.
   * @param caabApiProperties       The connection details for the CAAB API.
   * @param osApiUrl                The URL of the ordinance survey API.
   * @param loggingInterceptor      A logging interceptor for the caab.
   */
  public ApplicationConfig(final EbsApiProperties ebsApiProperties,
                           final SoaApiProperties soaApiProperties,
                           final CaabApiProperties caabApiProperties,
                           final AssessmentApiProperties assessmentApiProperties,
                           @Value("${os.api.url}") final String osApiUrl,
                           @Value("${av.api.hostname}") final String avApiHostName,
                           @Value("${av.api.port}") final Integer avApiPort,
                           @Value("${av.api.timeout}") final Integer avApiTimeout,
                           final LoggingInterceptor loggingInterceptor) {
    this.ebsApiProperties = ebsApiProperties;
    this.soaApiProperties = soaApiProperties;
    this.caabApiProperties = caabApiProperties;
    this.assessmentApiProperties = assessmentApiProperties;
    this.osApiUrl = osApiUrl;
    this.avApiHostName = avApiHostName;
    this.avApiPort = avApiPort;
    this.avApiTimeout = avApiTimeout;
    this.loggingInterceptor = loggingInterceptor;
  }

  /**
   * Creates a WebClient bean for interacting with the Ebs API.
   *
   * @return A WebClient instance configured for the Ebs API URL.
   */
  @Bean("ebsApiWebClient")
  WebClient ebsApiWebClient() {
    return createWebClient(ebsApiProperties);
  }

  /**
   * Creates a WebClient bean for interacting with the SOA API.
   *
   * @return A WebClient instance configured for the SOA API URL.
   */
  @Bean("soaApiWebClient")
  WebClient soaApiWebClient() {
    return createWebClient(soaApiProperties);
  }

  /**
   * Creates a WebClient bean for interacting with the CAAB API.
   *
   * @return A WebClient instance configured for the CAAB API URL.
   */
  @Bean("caabApiWebClient")
  WebClient caabApiWebClient() {
    return createWebClient(caabApiProperties);
  }

  /**
   * Creates a WebClient bean for interacting with the Assessment API.
   *
   * @return A WebClient instance configured for the Assessment API URL.
   */
  @Bean("assessmentApiWebClient")
  WebClient assessmentApiWebClient() {
    return createWebClient(assessmentApiProperties);
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
   * Creates a bean for interacting with the ClamAV API.
   *
   * @return A ClamAVClient instance.
   */
  @Bean("clamAvClient")
  ClamAVClient clamAvClient() {
    return new ClamAVClient(avApiHostName, avApiPort, avApiTimeout);
  }

  /**
   * Configures the @{link ThymeleafViewResolver} to make custom utility classes available to all
   * templates.
   *
   * @param templateEngine the template engine.
   * @return the configured @{link ThymeleafViewResolver}.
   */
  @Bean
  public ThymeleafViewResolver thymeleafViewResolver(
      @Autowired SpringTemplateEngine templateEngine) {
    ThymeleafViewResolver thymeleafViewResolver = new ThymeleafViewResolver();
    thymeleafViewResolver.setTemplateEngine(templateEngine);
    thymeleafViewResolver.setCharacterEncoding("UTF-8");
    thymeleafViewResolver.addStaticVariable("userRoleUtil", new UserRoleUtil());
    return thymeleafViewResolver;
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

  private WebClient createWebClient(final ApiProperties apiProperties) {
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();
    return WebClient.builder()
        .baseUrl(apiProperties.getUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, apiProperties.getAccessToken())
        .exchangeStrategies(strategies)
        .build();
  }

}
