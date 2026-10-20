package uk.gov.laa.ccms.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guards the security attributes on the session cookie.
 *
 * <p>Strict is deliberately not used: the identity provider returns the user by a cross-site
 * redirect and a Strict cookie is withheld on that hop, losing the authorization request mid
 * sign-in.
 */
@SpringBootTest(
    classes = SessionCookieIntegrationTest.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // Secure is switched on only for this context: it stops the client returning the cookie over
    // the plain http the other integration tests use.
    properties = "server.servlet.session.cookie.secure=true")
class SessionCookieIntegrationTest {

  @Value("${local.server.port}")
  private int port;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  private String sessionSetCookie() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + contextPath + "/start-session"))
            .GET()
            .build();

    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());

      Optional<String> setCookie =
          response.headers().allValues("set-cookie").stream()
              .filter(header -> header.contains("SESSIONID"))
              .findFirst();
      assertTrue(setCookie.isPresent(), "no session cookie was issued");
      return setCookie.get();
    }
  }

  @Test
  @DisplayName("The session cookie restricts cross-site sending without breaking the IdP redirect")
  void sessionCookieIsSameSiteLax() throws Exception {
    String cookie = sessionSetCookie();

    assertTrue(cookie.contains("SameSite=Lax"), "expected SameSite=Lax in: " + cookie);
    assertTrue(!cookie.contains("SameSite=Strict"), "Strict breaks the IdP redirect: " + cookie);
  }

  @Test
  @DisplayName("The session cookie is confined to HTTPS and hidden from scripts")
  void sessionCookieIsSecureAndHttpOnly() throws Exception {
    String cookie = sessionSetCookie();

    assertTrue(cookie.contains("Secure"), "expected Secure in: " + cookie);
    assertTrue(cookie.contains("HttpOnly"), "expected HttpOnly in: " + cookie);
  }

  /** Minimal application exposing an endpoint that creates a session. */
  @SpringBootConfiguration
  @EnableAutoConfiguration(
      exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
      })
  @RestController
  static class TestApplication {

    @GetMapping("/start-session")
    String startSession(HttpServletRequest request) {
      request.getSession(true);
      return "ok";
    }
  }
}
