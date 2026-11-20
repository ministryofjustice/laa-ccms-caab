package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

@DisplayName("API OAuth2 profile test")
class ApiOauth2ProfileTest {

  @SuppressWarnings("unchecked")
  private Map<String, Object> client() throws Exception {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource("src/main/resources/application-api-oauth2.yml"));
    factory.afterPropertiesSet();

    Map<String, Object> client =
        (Map<String, Object>)
            ((Map<String, Object>)
                    ((Map<String, Object>)
                            ((Map<String, Object>) factory.getObject().get("spring"))
                                .get("security"))
                        .get("oauth2"))
                .get("client");
    return client;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> registrations() throws Exception {
    return (Map<String, Object>) client().get("registration");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> providers() throws Exception {
    return (Map<String, Object>) client().get("provider");
  }

  @Test
  @DisplayName("Declares a registration for each API client, under the id that client asks for")
  void declaresEveryApiRegistration() throws Exception {
    assertEquals(
        Set.of(
            ApplicationConfig.EBS_API_REGISTRATION,
            ApplicationConfig.SOA_API_REGISTRATION,
            ApplicationConfig.CAAB_API_REGISTRATION,
            ApplicationConfig.ASSESSMENT_API_REGISTRATION),
        registrations().keySet());
  }

  @Test
  @DisplayName("Every registration is app-only, so the token carries this application's app role")
  void everyRegistrationIsClientCredentials() throws Exception {
    registrations()
        .forEach(
            (id, config) -> {
              Map<?, ?> values = (Map<?, ?>) config;
              assertEquals(
                  "client_credentials", values.get("authorization-grant-type"), "for " + id);
              assertTrue(
                  values.get("scope").toString().contains("SCOPE"), "no scope set for " + id);
            });
  }

  @Test
  @DisplayName("Service tokens come from EntraID, not from whatever signs the user in")
  void everyRegistrationUsesTheEntraProvider() throws Exception {
    // silas-identity is the browser login, and development, test and feature point it at the OIDC
    // mock. A service token from there is rejected by the APIs, which validate the real tenant.
    registrations()
        .forEach(
            (id, config) ->
                assertEquals(
                    "entra-api", ((Map<?, ?>) config).get("provider"), "wrong provider for " + id));
  }

  @Test
  @DisplayName("That provider names the tenant's token endpoint and not an overridable issuer")
  void theProviderIsTheRealTenant() throws Exception {
    Map<?, ?> provider = (Map<?, ?>) providers().get("entra-api");
    String tokenUri = provider.get("token-uri").toString();

    assertTrue(tokenUri.contains("login.microsoftonline.com"), tokenUri);
    assertTrue(tokenUri.contains("${AZURE_TENANT_ID}"), tokenUri);
    assertFalse(tokenUri.contains("OIDC_ISSUER_URI"), "must not follow the mock override");
  }
}
