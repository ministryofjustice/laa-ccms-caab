package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * The local profile must not hardcode what the setup guide says to switch with a variable.
 *
 * <p>A profile is read after {@code application.yml}, so a literal here silently wins over the
 * environment-backed value there. The symptom is not a failure: the application starts and signs
 * users in by the other path, so a documented switch appears to do nothing.
 */
@DisplayName("Local profile switches test")
class LocalProfileSwitchesTest {

  @SuppressWarnings("unchecked")
  private Map<String, Object> ccms() throws Exception {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource("src/main/resources/application-local.yml"));
    factory.afterPropertiesSet();
    return (Map<String, Object>)
        ((Map<String, Object>) ((Map<String, Object>) factory.getObject().get("laa")).get("ccms"))
            .getOrDefault("entra", Map.of());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> userDetailsApi() throws Exception {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource("src/main/resources/application-local.yml"));
    factory.afterPropertiesSet();
    return (Map<String, Object>)
        ((Map<String, Object>) ((Map<String, Object>) factory.getObject().get("laa")).get("ccms"))
            .get("user-details-api");
  }

  @Test
  @DisplayName("How an identity resolves is set by variable, so the documented switch works")
  void identityResolutionIsEnvironmentBacked() throws Exception {
    Map<String, Object> entra = ccms();

    assertTrue(
        entra.get("silas-enabled").toString().contains("IS_SILAS_ENABLED"),
        "silas-enabled must honour IS_SILAS_ENABLED, was " + entra.get("silas-enabled"));
    assertTrue(
        entra.get("custom-user-id-claim").toString().contains("ENTRA_CUSTOM_USER_ID_CLAIM"),
        "the claim must honour ENTRA_CUSTOM_USER_ID_CLAIM, was "
            + entra.get("custom-user-id-claim"));
  }

  @Test
  @DisplayName("The User Details API is set by variable, so an external sign-in can be pointed")
  void theUserDetailsApiIsEnvironmentBacked() throws Exception {
    Map<String, Object> api = userDetailsApi();

    assertTrue(
        api.get("hostname").toString().contains("USER_DETAILS_API_HOSTNAME"),
        "hostname must honour USER_DETAILS_API_HOSTNAME, was " + api.get("hostname"));
    assertTrue(
        api.get("access-token").toString().contains("USER_DETAILS_API_ACCESS_TOKEN"),
        "the token must honour USER_DETAILS_API_ACCESS_TOKEN, was " + api.get("access-token"));
  }
}
