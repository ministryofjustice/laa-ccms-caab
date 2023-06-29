package uk.gov.laa.ccms.caab.service;

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
import uk.gov.laa.ccms.data.model.ProviderResponse;
import uk.gov.laa.ccms.data.model.UserResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.laa.ccms.caab.service.DataServiceErrorHandler.USER_ERROR_MESSAGE;

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
        UserResponse expectedUserResponse = buildUserResponse();
        String userJson = objectMapper.writeValueAsString(expectedUserResponse);

        wiremock.stubFor(get(String.format("/users/%s", expectedUserResponse.getLoginId()))
            .willReturn(okJson(userJson)));

        Mono<UserResponse> userResponseMono = dataService.getUser(expectedUserResponse.getLoginId());

        UserResponse userResponse = userResponseMono.block();

        assertEquals(userJson, objectMapper.writeValueAsString(userResponse));
    }

    @Test
    public void testGetUser_notFound() {
        String loginId = "user1";
        String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

        wiremock.stubFor(get(String.format("/users/%s", loginId))
            .willReturn(notFound()));

        Mono<UserResponse> userResponseMono = dataService.getUser(loginId);

        StepVerifier.create(userResponseMono)
                .expectErrorMatches(throwable -> throwable instanceof DataServiceException &&
                        throwable.getMessage().equals(expectedMessage)
                ).verify();
     }

    private UserResponse buildUserResponse() {
        return new UserResponse()
            .userId(1)
            .userType("testUserType")
            .loginId("user1")
            .addFirmsItem(
                new ProviderResponse()
                    .id(1)
                    .name("testProvider"))
            .addFunctionsItem("testFunction");
    }

}
