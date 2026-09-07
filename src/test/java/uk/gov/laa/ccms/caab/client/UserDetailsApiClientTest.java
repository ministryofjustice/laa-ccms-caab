package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.user.CcmsUserDetails;
import uk.gov.laa.ccms.caab.model.user.UserViewModel;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
@DisplayName("User Details API client test")
class UserDetailsApiClientTest {

  private static final String EXPECTED_URI = "/api/v1/user-details/silas/{silasId}";

  @Mock private WebClient webClientMock;
  @Mock private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock private WebClient.ResponseSpec responseMock;

  @Mock private UserDetailsApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks private UserDetailsApiClient userDetailsApiClient;

  @Test
  @DisplayName("Should return the CCMS user held against the SiLAS identity")
  void getUserBySilasId_returnsData() {
    final String silasId = "a-silas-identifier";

    final CcmsUserDetails ccmsUserDetails = new CcmsUserDetails();
    ccmsUserDetails.setUserLoginId("CCMSUSER");
    final UserViewModel expected = new UserViewModel();
    expected.setCcmsUserDetails(ccmsUserDetails);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(EXPECTED_URI, silasId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserViewModel.class)).thenReturn(Mono.just(expected));

    StepVerifier.create(userDetailsApiClient.getUserBySilasId(silasId))
        .expectNextMatches(user -> user == expected)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should treat an unknown SiLAS identity as an empty result rather than an error")
  void getUserBySilasId_treatsNotFoundAsEmpty() {
    final String silasId = "an-unknown-identifier";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(EXPECTED_URI, silasId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserViewModel.class)).thenReturn(Mono.empty());

    StepVerifier.create(userDetailsApiClient.getUserBySilasId(silasId)).verifyComplete();

    final ArgumentCaptor<Predicate<HttpStatusCode>> statusCaptor =
        ArgumentCaptor.forClass(Predicate.class);
    verify(responseMock).onStatus(statusCaptor.capture(), any());

    // Only a 404 is swallowed - anything else still has to surface as a failure.
    assertTrue(statusCaptor.getValue().test(HttpStatus.NOT_FOUND));
    assertFalse(statusCaptor.getValue().test(HttpStatus.INTERNAL_SERVER_ERROR));
    assertFalse(statusCaptor.getValue().test(HttpStatus.UNAUTHORIZED));
  }

  @Test
  @DisplayName("Should handle error")
  void getUserBySilasId_handlesError() {
    final String silasId = "a-silas-identifier";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(EXPECTED_URI, silasId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserViewModel.class))
        .thenReturn(
            Mono.error(
                new WebClientResponseException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)));
    when(apiClientErrorHandler.handleApiRetrieveError(
            any(), eq("User"), eq("SiLAS id"), eq(silasId)))
        .thenReturn(Mono.empty());

    StepVerifier.create(userDetailsApiClient.getUserBySilasId(silasId)).verifyComplete();

    verify(apiClientErrorHandler)
        .handleApiRetrieveError(any(), eq("User"), eq("SiLAS id"), eq(silasId));
  }
}
