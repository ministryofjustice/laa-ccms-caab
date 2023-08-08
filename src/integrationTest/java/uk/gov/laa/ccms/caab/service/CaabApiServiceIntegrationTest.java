package uk.gov.laa.ccms.caab.service;

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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
public class CaabApiServiceIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    protected static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("laa.ccms.caab-api.port", wiremock::getPort);
    }

    @Autowired
    private CaabApiService caabApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCreateApplication() throws Exception {
        String loginId = "user1";
        ApplicationDetail applicationDetail = new ApplicationDetail(); // Populate this object with appropriate test data
        String applicationDetailJson = objectMapper.writeValueAsString(applicationDetail);

        wiremock.stubFor(post("/applications")
                .withHeader("Caab-User-Login-Id", equalTo(loginId))
                .withRequestBody(equalToJson(applicationDetailJson))
                .willReturn(ok()));

        Mono<Void> responseMono = caabApiService.createApplication(loginId, applicationDetail);
        responseMono.block();
    }
}