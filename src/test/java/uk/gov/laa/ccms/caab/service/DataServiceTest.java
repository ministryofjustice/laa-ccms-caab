package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.UserDetails;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class DataServiceTest {

    @Mock
    private WebClient webClientMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @InjectMocks
    private DataService dataService;

    @Mock
    private DataServiceErrorHandler dataServiceErrorHandler;

    @Test
    void getUser_returnData() {

        String loginId = "user1";
        String expectedUri = "/users/{loginId}";

        UserDetails mockUser = new UserDetails();
        mockUser.setLoginId(loginId);

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.just(mockUser));

        Mono<UserDetails> userDetailsMono = dataService.getUser(loginId);

        StepVerifier.create(userDetailsMono)
                .expectNextMatches(user -> user.getLoginId().equals(loginId))
                .verifyComplete();
    }

    @Test
    void getUser_notFound() {
        String loginId = "user1";
        String expectedUri = "/users/{loginId}";

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(dataServiceErrorHandler.handleUserError(eq(loginId), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        Mono<UserDetails> userDetailsMono = dataService.getUser(loginId);

        StepVerifier.create(userDetailsMono)
                .verifyComplete();
    }

}
