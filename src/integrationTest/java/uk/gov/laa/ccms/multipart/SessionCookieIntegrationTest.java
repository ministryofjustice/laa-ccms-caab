package uk.gov.laa.ccms.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = SessionCookieIntegrationTest.App.class,
    properties = "spring.config.import=file:src/main/resources/application.yml",
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Value("${server.servlet.context-path}")
  String contextPath;

  @Test
  void secureHttpOnlyCookieAndForwardedHttps() throws Exception {
    var plain = request(port, contextPath, false);
    String cookie = plain.headers().firstValue("set-cookie").orElseThrow();
    assertThat(cookie).contains("Secure", "HttpOnly").doesNotContain("SameSite");
    assertThat(plain.headers().firstValue("strict-transport-security")).isEmpty();
    var forwarded = request(port, contextPath, true);
    assertThat(forwarded.headers().firstValue("strict-transport-security")).isPresent();
  }

  static HttpResponse<String> request(int port, String contextPath, boolean forwarded)
      throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + contextPath + "/session"))
            .GET();
    if (forwarded) builder.header("X-Forwarded-Proto", "https");
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude = Saml2RelyingPartyAutoConfiguration.class)
  @Import(SessionEndpoint.class)
  static class App {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
      return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }
  }

  @RestController
  static class SessionEndpoint {
    @GetMapping("/session")
    String session(HttpServletRequest request) {
      request.getSession();
      return "ok";
    }
  }
}

@ActiveProfiles("local")
@SpringBootTest(
    classes = SessionCookieIntegrationTest.App.class,
    properties = "spring.config.import=file:src/main/resources/application.yml",
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalSessionCookieIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Value("${server.servlet.context-path}")
  String contextPath;

  @Test
  void localCookieWorksOverHttp() throws Exception {
    String cookie =
        SessionCookieIntegrationTest.request(port, contextPath, false)
            .headers()
            .firstValue("set-cookie")
            .orElseThrow();
    assertThat(cookie).contains("CAAB_JSESSIONID", "HttpOnly").doesNotContain("Secure", "SameSite");
  }
}
