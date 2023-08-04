package uk.gov.laa.ccms.caab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SoaGatewayServiceIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    protected static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("laa.ccms.soa-gateway-api.port", wiremock::getPort);
    }


    @Autowired
    private SoaGatewayService soaGatewayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetNotificationsSummary_returnData() throws Exception {
        String loginId = "user1";
        String userType = "userType";
        NotificationSummary expectedSummary = buildNotificationSummary();
        String summaryJson = objectMapper.writeValueAsString(expectedSummary);

        wiremock.stubFor(get(String.format("/users/%s/notifications/summary", loginId))
                .withHeader("SoaGateway-User-Login-Id", equalTo(loginId))
                .withHeader("SoaGateway-User-Role", equalTo(userType))
                .willReturn(okJson(summaryJson)));

        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId, userType);

        NotificationSummary summary = summaryMono.block();

        assertEquals(summaryJson, objectMapper.writeValueAsString(summary));
    }

    @Test
    public void testGetCategoryOfLawCodes_returnData() throws Exception {
        Integer providerFirmId = 123;
        Integer officeId = 345;
        String loginId = "user1";
        String userType = "userType";
        ContractDetails contractDetails = buildContractDetails();
        String contractDetailsJson = objectMapper.writeValueAsString(contractDetails);

        wiremock.stubFor(get(String.format("/contract-details?providerFirmId=%s&officeId=%s", providerFirmId, officeId))
            .withHeader("SoaGateway-User-Login-Id", equalTo(loginId))
            .withHeader("SoaGateway-User-Role", equalTo(userType))
            .willReturn(okJson(contractDetailsJson)));

        List<String> response = soaGatewayService.getCategoryOfLawCodes(providerFirmId, officeId, loginId, userType, Boolean.TRUE);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("CAT1", response.get(0));
    }

    private NotificationSummary buildNotificationSummary() {
        return new NotificationSummary()
                .notifications(10)
                .standardActions(5)
                .overdueActions(2);
    }

    private ContractDetails buildContractDetails() {
        return new ContractDetails()
            .addContractsItem(new ContractDetail()
                .categoryofLaw("CAT1")
                .subCategory("SUBCAT1")
                .createNewMatters(true)
                .remainderAuthorisation(true)
                .contractualDevolvedPowers("CATDEVPOW")
                .authorisationType("AUTHTYPE1"));
    }
}
