package uk.gov.laa.ccms.caab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.data.model.ProviderResponse;
import uk.gov.laa.ccms.data.model.UserResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.laa.ccms.caab.service.DataServiceErrorHandler.USER_ERROR_MESSAGE;

public class DataServiceIntegrationTest extends AbstractIntegrationTest {

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
