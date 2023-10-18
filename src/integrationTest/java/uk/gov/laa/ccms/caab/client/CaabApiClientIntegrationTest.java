package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

@SpringBootTest
public class CaabApiClientIntegrationTest extends AbstractIntegrationTest {

  @RegisterExtension
  protected static WireMockExtension wiremock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.caab-api.port", wiremock::getPort);
  }

  @Autowired
  private CaabApiClient caabApiClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCreateApplication() throws Exception {
    String loginId = "user1";
    ApplicationDetail applicationDetail =
        new ApplicationDetail(); // Populate this object with appropriate test data
    String applicationDetailJson = objectMapper.writeValueAsString(applicationDetail);

    wiremock.stubFor(post("/applications")
        .withHeader("Caab-User-Login-Id", equalTo(loginId))
        .withRequestBody(equalToJson(applicationDetailJson))
        .willReturn(ok()
            .withHeader("Location", "https://laa-ccms-caab-api/applications/123")));

    Mono<String> responseMono = caabApiClient.createApplication(loginId, applicationDetail);
    responseMono.block();
  }
}
