package uk.gov.laa.ccms.caab.service;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.laa.ccms.caab.service.DataServiceErrorHandler.USER_ERROR_MESSAGE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.data.model.OfficeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

public class DataServiceIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    protected static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("laa.ccms.data-api.port", wiremock::getPort);
    }

    @Autowired
    private DataService dataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetUser_returnData() throws Exception{
        UserDetails expectedUserDetails = buildUserDetails();
        String userJson = objectMapper.writeValueAsString(expectedUserDetails);

        wiremock.stubFor(get(String.format("/users/%s", expectedUserDetails.getLoginId()))
            .willReturn(okJson(userJson)));

        Mono<UserDetails> userDetailsMono = dataService.getUser(expectedUserDetails.getLoginId());

        UserDetails userDetails = userDetailsMono.block();

        assertEquals(userJson, objectMapper.writeValueAsString(userDetails));
    }

    @Test
    public void testGetUser_notFound() {
        String loginId = "user1";
        String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

        wiremock.stubFor(get(String.format("/users/%s", loginId))
            .willReturn(notFound()));

        Mono<UserDetails> userDetailsMono = dataService.getUser(loginId);

        StepVerifier.create(userDetailsMono)
                .expectErrorMatches(throwable -> throwable instanceof DataServiceException &&
                        throwable.getMessage().equals(expectedMessage)
                ).verify();
     }

    private UserDetails buildUserDetails() {
        return new UserDetails()
            .userId(1)
            .userType("testUserType")
            .loginId("user1")
            .addFirmsItem(
                new ProviderDetails()
                    .id(1)
                    .name("testProvider"))
            .provider(new ProviderDetails()
                .id(2)
                .addOfficesItem(new OfficeDetails()
                    .id(1)
                    .name("Office 1")))
            .addFunctionsItem("testFunction");
    }

}
