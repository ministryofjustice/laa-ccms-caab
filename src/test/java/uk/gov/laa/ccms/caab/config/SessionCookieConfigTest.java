package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * Guards the deployed session cookie defaults.
 *
 * <p>{@code SessionCookieIntegrationTest} proves the container emits the attributes, but switches
 * Secure on for its own context, so it cannot show what deployed environments actually get.
 */
@DisplayName("Session cookie configuration test")
class SessionCookieConfigTest {

  @SuppressWarnings("unchecked")
  private Map<String, Object> sessionCookieConfig(String resource) throws Exception {
    // By path, not classpath: src/test/resources/application.yml shadows the deployed one.
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource("src/main/resources/" + resource));
    factory.afterPropertiesSet();

    Map<String, Object> map = factory.getObject();
    Map<String, Object> servlet =
        (Map<String, Object>) ((Map<String, Object>) map.get("server")).get("servlet");
    return (Map<String, Object>) ((Map<String, Object>) servlet.get("session")).get("cookie");
  }

  @Test
  @DisplayName("Deployed environments restrict the cookie to HTTPS by default")
  void deployedCookieIsSecureByDefault() throws Exception {
    assertEquals(
        "${SESSION_COOKIE_SECURE:true}", sessionCookieConfig("application.yml").get("secure"));
  }

  @Test
  @DisplayName("Deployed environments use Lax, which the IdP redirect requires")
  void deployedCookieIsSameSiteLax() throws Exception {
    assertEquals("lax", sessionCookieConfig("application.yml").get("same-site"));
  }

  @Test
  @DisplayName("Local development turns Secure off, as it runs over plain http")
  void localCookieIsNotSecure() throws Exception {
    assertEquals(false, sessionCookieConfig("application-local.yml").get("secure"));
  }
}
