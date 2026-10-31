package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.UserService;

@SpringJUnitWebConfig(SecurityEventsEndToEndTest.Config.class)
@TestPropertySource(properties = "portal.logoutUrl=https://idp.example.test/logout")
class SecurityEventsEndToEndTest {

  @Autowired private WebApplicationContext context;
  @MockitoBean private UserService userService;

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger eventLogger = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    appender.start();
    eventLogger.addAppender(appender);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void tearDown() {
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
  }

  @Test
  void aSamlResponseThatCannotBeReadIsLoggedAsAFailedSignIn() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/login/saml2/sso/idp").param("SAMLResponse", "not-a-saml-response"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(302);
    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/login?error");
    assertThat(eventTypes()).contains("AUTHENTICATION_FAILURE");
    assertOnlyAnErrorCodeWasLogged("not-a-saml-response");
  }

  @Test
  void aWellFormedButUnsignedSamlResponseIsLoggedAsAFailedSignIn() throws Exception {
    String response =
        "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"r1\""
            + " Version=\"2.0\" IssueInstant=\"2026-01-01T00:00:00Z\""
            + " Destination=\"http://localhost/login/saml2/sso/idp\"/>";

    MvcResult result =
        mockMvc
            .perform(
                post("/login/saml2/sso/idp")
                    .param(
                        "SAMLResponse",
                        Base64.getEncoder()
                            .encodeToString(response.getBytes(StandardCharsets.UTF_8))))
            .andReturn();

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/login?error");
    assertThat(eventTypes()).contains("AUTHENTICATION_FAILURE");
    assertOnlyAnErrorCodeWasLogged("samlp:Response");
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
  @Import({SecurityConfiguration.class, SecurityEventLogger.class})
  static class Config {

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws Exception {
      X509Certificate certificate =
          (X509Certificate)
              CertificateFactory.getInstance("X.509")
                  .generateCertificate(
                      new ByteArrayInputStream(
                          Files.readAllBytes(
                              Path.of("src/test/resources/saml/test-idp-certificate.pem"))));
      return new InMemoryRelyingPartyRegistrationRepository(
          RelyingPartyRegistration.withRegistrationId("idp")
              .entityId("{baseUrl}/saml2/service-provider-metadata/{registrationId}")
              .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
              .assertingPartyMetadata(
                  party ->
                      party
                          .entityId("https://idp.example.test/metadata")
                          .singleSignOnServiceLocation("https://idp.example.test/sso")
                          .verificationX509Credentials(
                              c -> c.add(Saml2X509Credential.verification(certificate))))
              .build());
    }
  }
}
