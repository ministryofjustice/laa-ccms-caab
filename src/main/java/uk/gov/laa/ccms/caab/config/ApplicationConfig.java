package uk.gov.laa.ccms.caab.config;

import fi.solita.clamav.ClamAVClient;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import uk.gov.laa.ccms.caab.util.UserRoleUtil;

/** Configuration class for creating WebClient instances used for making HTTP requests. */
@Configuration
@EnableConfigurationProperties({
  AssessmentApiProperties.class,
  CaabApiProperties.class,
  EbsApiProperties.class,
  EntraProperties.class,
  SoaApiProperties.class,
  UserDetailsApiProperties.class
})
public class ApplicationConfig implements WebMvcConfigurer {

  // Must match the registration ids declared by the api-oauth2 profile; a mismatch silently
  // leaves that API on its static access token.
  static final String EBS_API_REGISTRATION = "ebs-api";
  static final String SOA_API_REGISTRATION = "soa-api";
  static final String CAAB_API_REGISTRATION = "caab-api";
  static final String ASSESSMENT_API_REGISTRATION = "assessment-api";

  private final EbsApiProperties ebsApiProperties;

  private final SoaApiProperties soaApiProperties;

  private final CaabApiProperties caabApiProperties;

  private final AssessmentApiProperties assessmentApiProperties;

  private final UserDetailsApiProperties userDetailsApiProperties;

  private final String osApiUrl;

  private final String connectorApiUrl;

  private final String avApiHostName;

  private final Integer avApiPort;

  private final Integer avApiTimeout;

  private final LoggingInterceptor loggingInterceptor;

  private final ClientRegistrationRepository clientRegistrationRepository;

  private final OAuth2AuthorizedClientManager authorizedClientManager;

  private final UploadSizeErrorInterceptor uploadSizeErrorInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loggingInterceptor);
    registry.addInterceptor(uploadSizeErrorInterceptor);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToCaseContextConverter());
    registry.addConverter(new StringToBillingContextConverter());
  }

  /**
   * Constructs the ApplicationConfig instance with API URLs.
   *
   * @param ebsApiProperties The connection details for the data API.
   * @param soaApiProperties The connection details for the SOA Gateway API.
   * @param caabApiProperties The connection details for the CAAB API.
   * @param assessmentApiProperties The connection details for the Assessment API.
   * @param userDetailsApiProperties The connection details for the User Details API.
   * @param osApiUrl The URL of the ordinance survey API.
   * @param loggingInterceptor A logging interceptor for the caab.
   * @param clientRegistrationRepository The OAuth2 client registrations, absent when the OAuth2
   *     client is not configured.
   */
  public ApplicationConfig(
      final EbsApiProperties ebsApiProperties,
      final SoaApiProperties soaApiProperties,
      final CaabApiProperties caabApiProperties,
      final AssessmentApiProperties assessmentApiProperties,
      final UserDetailsApiProperties userDetailsApiProperties,
      @Value("${os.api.url}") final String osApiUrl,
      @Value("${laa.ccms.connector.url}") final String connectorApiUrl,
      @Value("${av.api.hostname}") final String avApiHostName,
      @Value("${av.api.port}") final Integer avApiPort,
      @Value("${av.api.timeout}") final Integer avApiTimeout,
      final LoggingInterceptor loggingInterceptor,
      final UploadSizeErrorInterceptor uploadSizeErrorInterceptor,
      final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
    this.ebsApiProperties = ebsApiProperties;
    this.soaApiProperties = soaApiProperties;
    this.caabApiProperties = caabApiProperties;
    this.assessmentApiProperties = assessmentApiProperties;
    this.userDetailsApiProperties = userDetailsApiProperties;
    this.osApiUrl = osApiUrl;
    this.connectorApiUrl = connectorApiUrl;
    this.avApiHostName = avApiHostName;
    this.avApiPort = avApiPort;
    this.avApiTimeout = avApiTimeout;
    this.loggingInterceptor = loggingInterceptor;
    this.uploadSizeErrorInterceptor = uploadSizeErrorInterceptor;
    this.clientRegistrationRepository = clientRegistrationRepository.getIfAvailable();
    this.authorizedClientManager = authorizedClientManager(this.clientRegistrationRepository);
  }

  private static OAuth2AuthorizedClientManager authorizedClientManager(
      final ClientRegistrationRepository registrations) {
    if (registrations == null) {
      return null;
    }
    final AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            registrations, new InMemoryOAuth2AuthorizedClientService(registrations));
    manager.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
    return manager;
  }

  /**
   * Creates a WebClient bean for interacting with the Ebs API.
   *
   * @return A WebClient instance configured for the Ebs API URL.
   */
  @Bean("ebsApiWebClient")
  WebClient ebsApiWebClient() {
    return createWebClient(ebsApiProperties, EBS_API_REGISTRATION);
  }

  /**
   * Creates a WebClient bean for interacting with the SOA API.
   *
   * @return A WebClient instance configured for the SOA API URL.
   */
  @Bean("soaApiWebClient")
  WebClient soaApiWebClient() {
    return createWebClient(soaApiProperties, SOA_API_REGISTRATION);
  }

  /**
   * Creates a WebClient bean for interacting with the CAAB API.
   *
   * @return A WebClient instance configured for the CAAB API URL.
   */
  @Bean("caabApiWebClient")
  WebClient caabApiWebClient() {
    return createWebClient(caabApiProperties, CAAB_API_REGISTRATION);
  }

  /**
   * Creates a WebClient bean for interacting with the Assessment API.
   *
   * @return A WebClient instance configured for the Assessment API URL.
   */
  @Bean("assessmentApiWebClient")
  WebClient assessmentApiWebClient() {
    return createWebClient(assessmentApiProperties, ASSESSMENT_API_REGISTRATION);
  }

  /**
   * Creates a WebClient bean for interacting with the User Details API.
   *
   * <p>The User Details API takes its access token in an {@code X-Authorization} header rather than
   * the standard {@code Authorization} one, so this is built here rather than by {@code
   * createWebClient}. The legacy PUI calls it the same way.
   *
   * @return A WebClient instance configured for the User Details API.
   */
  @Bean("userDetailsApiWebClient")
  WebClient userDetailsApiWebClient() {
    return WebClient.builder()
        .baseUrl(userDetailsApiProperties.getUrl())
        .defaultHeader("X-Authorization", userDetailsApiProperties.getAccessToken())
        .build();
  }

  /**
   * Creates a WebClient bean for interacting with the Ordinance Survey API.
   *
   * @return A WebClient instance configured for the Ordinance Survey API.
   */
  /**
   * Creates a WebClient bean for interacting with the OPA connector.
   *
   * <p>The connector needs no access token - it authenticates the caller from the encrypted context
   * token carried in the OPA session - so this is a plain client rather than one built by {@code
   * createWebClient}.
   *
   * @return A WebClient instance configured for the connector URL.
   */
  @Bean("connectorApiWebClient")
  WebClient connectorApiWebClient() {
    return WebClient.create(connectorApiUrl);
  }

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
   * Makes a {@link SpelExpressionParser} available to the application context for SpEL parsing.
   *
   * @return a {@link SpelExpressionParser}
   */
  @Bean
  public SpelExpressionParser spelExpressionParser() {
    return new SpelExpressionParser();
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

  /**
   * Builds a client for an API, authenticated by OAuth2 where the API has a client registration and
   * by its static access token otherwise.
   *
   * <p>Each API therefore migrates on its own, which is what the APIs themselves support: while
   * both are configured they accept a bearer token or an access token on the same endpoints.
   */
  private WebClient createWebClient(
      final ApiProperties apiProperties, final String registrationId) {
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();

    final WebClient.Builder builder =
        WebClient.builder().baseUrl(apiProperties.getUrl()).exchangeStrategies(strategies);

    if (registered(registrationId)) {
      builder.filter(new ClientCredentialsTokenFilter(authorizedClientManager, registrationId));
    } else {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, apiProperties.getAccessToken());
    }

    return builder.build();
  }

  private boolean registered(final String registrationId) {
    return clientRegistrationRepository != null
        && clientRegistrationRepository.findByRegistrationId(registrationId) != null;
  }
}
