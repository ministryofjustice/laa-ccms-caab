package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

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
import uk.gov.laa.ccms.caab.model.BillCreate;
import uk.gov.laa.ccms.caab.model.Bills;

@SpringBootTest
public class CaabApiClientIntegrationTest extends AbstractIntegrationTest {

  @RegisterExtension
  protected static WireMockExtension wiremock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.caab-api.port", wiremock::getPort);
  }

  @Autowired private CaabApiClient caabApiClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCreateApplication() throws Exception {
    String loginId = "user1";
    ApplicationDetail applicationDetail =
        new ApplicationDetail(); // Populate this object with appropriate test data
    String applicationDetailJson = objectMapper.writeValueAsString(applicationDetail);

    wiremock.stubFor(
        post("/applications")
            .withHeader("Caab-User-Login-Id", equalTo(loginId))
            .withRequestBody(equalToJson(applicationDetailJson))
            .willReturn(ok().withHeader("Location", "https://laa-ccms-caab-api/applications/123")));

    Mono<String> responseMono = caabApiClient.createApplication(loginId, applicationDetail);
    responseMono.block();
  }

  @Test
  public void testCreateBill() throws Exception {
    String loginId = "user1";
    // The draft bill is created carrying only the case and provider, as the legacy PUI does; the
    // OPA interview fills in the rest.
    BillCreate bill = new BillCreate().lscCaseReference("300000123").providerId("12345");

    wiremock.stubFor(
        post("/bills")
            .withHeader("Caab-User-Login-Id", equalTo(loginId))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(bill)))
            .willReturn(ok()));

    caabApiClient.createBill(bill, loginId).block();

    wiremock.verify(postRequestedFor(urlEqualTo("/bills")));
  }

  @Test
  public void testGetBill() throws Exception {
    Bills bill = new Bills().lscCaseReferenceNumber("300000123").providerId("12345");

    wiremock.stubFor(
        get(urlPathEqualTo("/bills"))
            .withQueryParam("case-reference", equalTo("300000123"))
            .withQueryParam("provider-id", equalTo("12345"))
            .willReturn(okJson(objectMapper.writeValueAsString(bill))));

    Bills result = caabApiClient.getBill("300000123", "12345").block();

    assertThat(result).isNotNull();
    assertThat(result.getLscCaseReferenceNumber()).isEqualTo("300000123");
    assertThat(result.getProviderId()).isEqualTo("12345");
  }

  @Test
  public void testGetBillReturnsEmptyWhenThereIsNoDraft() {
    // A case with no draft bill is the normal state, not an error, so the 404 must come back as an
    // empty Mono rather than propagating - the bill details screen relies on it to decide whether
    // to create one.
    wiremock.stubFor(get(urlPathEqualTo("/bills")).willReturn(notFound()));

    assertThat(caabApiClient.getBill("300000123", "12345").blockOptional()).isEmpty();
  }
}
