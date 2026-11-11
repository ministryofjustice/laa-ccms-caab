package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.security.CcmsUserIdentityResolver;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.UserService;

@SpringJUnitWebConfig(SecurityEventsEndToEndTest.Config.class)
@TestPropertySource(properties = "portal.logoutUrl=https://idp.example.test/logout")
class SecurityEventsEndToEndTest {

  @Autowired private WebApplicationContext context;
  @MockitoBean private UserService userService;
  @MockitoBean private PuiMetricService puiMetricService;
  @MockitoBean private CcmsUserIdentityResolver ccmsUserIdentityResolver;

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger eventLogger = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    uk.gov.laa.ccms.caab.logging.RequestRoute.registerKnownSegments(
        java.util.Set.of("case", "overview"));
    appender.start();
    eventLogger.addAppender(appender);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void tearDown() {
    uk.gov.laa.ccms.caab.logging.RequestRoute.clearKnownSegments();
    eventLogger.detachAppender(appender);
  }

  @Test
  void aRouteTheUserLacksTheFunctionForIsRefusedAndLoggedAsADenial() throws Exception {
    int status =
        mockMvc
            .perform(get("/case/overview").with(user("jane").authorities()))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isEqualTo(403);
    assertThat(eventTypes()).contains("AUTHORIZATION_DENIED");
    assertThat(keyValue("AUTHORIZATION_DENIED", "event_detail")).isEqualTo("GET /case/overview");
    org.mockito.Mockito.verify(puiMetricService)
        .incrementSecurityEvents("AUTHORIZATION_DENIED", "DENIED");
  }

  @Test
  void aCallbackWithoutAnAuthorizationRequestIsLoggedAsAFailedSignIn() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/login/oauth2/code/silas-identity")
                    .param("code", "an-opaque-authorization-code")
                    .param("state", "a-state-value"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(302);
    assertThat(result.getResponse().getRedirectedUrl()).endsWith("/authentication-error");
    assertThat(eventTypes()).contains("AUTHENTICATION_FAILURE");
    assertOnlyAnErrorCodeWasLogged("an-opaque-authorization-code");
    org.mockito.Mockito.verify(puiMetricService)
        .incrementSecurityEvents("AUTHENTICATION_FAILURE", "FAILURE");
  }

  private void assertOnlyAnErrorCodeWasLogged(String payloadFragment) {
    ILoggingEvent event =
        appender.list.stream()
            .filter(
                e ->
                    e.getKeyValuePairs().stream()
                        .anyMatch(kv -> kv.value.equals("AUTHENTICATION_FAILURE")))
            .findFirst()
            .orElseThrow();
    String detail =
        event.getKeyValuePairs().stream()
            .filter(kv -> kv.key.equals("event_detail"))
            .map(kv -> String.valueOf(kv.value))
            .findFirst()
            .orElseThrow();
    assertThat(detail).matches("[a-z_]+");
    assertThat(event.getFormattedMessage()).doesNotContain(payloadFragment);
    assertThat(detail).doesNotContain(payloadFragment);
  }

  @Test
  void aCsrfRejectionOfASignedInUserIsLoggedWithThatUser() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/case/overview")
                    .with(user("jane").authorities())
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.csrf()
                            .useInvalidToken()))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isEqualTo(403);
    ILoggingEvent event =
        appender.list.stream()
            .filter(
                e -> e.getKeyValuePairs().stream().anyMatch(kv -> kv.value.equals("CSRF_REJECTED")))
            .findFirst()
            .orElseThrow();
    assertThat(event.getMDCPropertyMap().get("user_id"))
        .isEqualTo(uk.gov.laa.ccms.caab.logging.UserIdentifier.pseudonymise("jane"));
  }

  private String keyValue(String eventType, String key) {
    return appender.list.stream()
        .filter(
            e ->
                e.getKeyValuePairs().stream()
                    .anyMatch(kv -> kv.key.equals("event_type") && kv.value.equals(eventType)))
        .flatMap(e -> e.getKeyValuePairs().stream())
        .filter(kv -> kv.key.equals(key))
        .map(kv -> String.valueOf(kv.value))
        .findFirst()
        .orElse(null);
  }

  private List<String> eventTypes() {
    return appender.list.stream()
        .flatMap(e -> e.getKeyValuePairs().stream())
        .filter(kv -> kv.key.equals("event_type"))
        .map(kv -> String.valueOf(kv.value))
        .collect(Collectors.toList());
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @Import({
    SecurityConfiguration.class,
    AuthorizationEventsConfiguration.class,
    SecurityEventLogger.class
  })
  static class Config {

    /** Boot registers this at runtime; the slim test context needs it for Duration binding. */
    @Bean
    static ConversionService conversionService() {
      return ApplicationConversionService.getSharedInstance();
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("silas-identity")
              .clientId("caab")
              .clientSecret("secret")
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .scope("openid")
              .authorizationUri("https://idp.example.test/authorize")
              .tokenUri("https://idp.example.test/token")
              .jwkSetUri("https://idp.example.test/keys")
              .issuerUri("https://idp.example.test")
              .userNameAttributeName("sub")
              .build());
    }
  }
}
