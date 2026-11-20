package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;

/**
 * The SiLAS identity exchange against a real exchange rather than a mocked WebClient, because what
 * matters here is whether a 404 body is decoded - which a mocked response spec cannot show.
 */
@DisplayName("User Details API client integration test")
public class UserDetailsApiClientIntegrationTest extends AbstractIntegrationTest {

  private static final String SILAS_PATH = "/api/v1/user-details/silas/a-silas-id";

  @RegisterExtension
  protected static WireMockExtension wiremock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  public static void properties(final DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.user-details-api.port", wiremock::getPort);
  }

  @Autowired private UserDetailsApiClient userDetailsApiClient;

  @Test
  @DisplayName("A known identity returns its CCMS user")
  void returnsTheCcmsUser() {
    wiremock.stubFor(
        get(urlPathEqualTo(SILAS_PATH))
            .willReturn(okJson("{\"ccmsUserDetails\":{\"userLoginId\":\"A-USER\"}}")));

    StepVerifier.create(userDetailsApiClient.getUserBySilasId("a-silas-id"))
        .expectNextMatches(user -> "A-USER".equals(user.getCcmsUserDetails().getUserLoginId()))
        .verifyComplete();
  }

  @Test
  @DisplayName("An unknown identity is empty even when the 404 carries a body")
  void isEmptyForANotFoundWithABody() {
    wiremock.stubFor(
        get(urlPathEqualTo(SILAS_PATH))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"unknown identity\"}")));

    StepVerifier.create(userDetailsApiClient.getUserBySilasId("a-silas-id")).verifyComplete();
  }

  @Test
  @DisplayName("An unknown identity is empty even when the 404 body is not JSON")
  void isEmptyForANotFoundWithAnHtmlBody() {
    wiremock.stubFor(
        get(urlPathEqualTo(SILAS_PATH))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "text/html")
                    .withBody("<html><body>Not found</body></html>")));

    StepVerifier.create(userDetailsApiClient.getUserBySilasId("a-silas-id")).verifyComplete();
  }

  @Test
  @DisplayName("A server error still surfaces as a failure")
  void aServerErrorSurfaces() {
    wiremock.stubFor(get(urlPathEqualTo(SILAS_PATH)).willReturn(aResponse().withStatus(500)));

    StepVerifier.create(userDetailsApiClient.getUserBySilasId("a-silas-id"))
        .expectError(UserDetailsApiClientException.class)
        .verify();
  }
}
