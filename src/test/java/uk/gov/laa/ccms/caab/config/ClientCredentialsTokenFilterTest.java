package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client credentials token filter test")
class ClientCredentialsTokenFilterTest {

  private static final String REGISTRATION_ID = "ebs-api";

  @Mock private OAuth2AuthorizedClientManager authorizedClientManager;

  private final AtomicReference<ClientRequest> exchanged = new AtomicReference<>();

  private final ExchangeFunction next =
      request -> {
        exchanged.set(request);
        return Mono.just(ClientResponse.create(HttpStatus.OK).build());
      };

  private static ClientRequest request() {
    return ClientRequest.create(HttpMethod.GET, URI.create("http://an-api/resource")).build();
  }

  private void authorizedWithToken(final String tokenValue) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId("a-client")
            .clientSecret("a-secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://an-idp/oauth2/token")
            .build();
    OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            tokenValue,
            Instant.now(),
            Instant.now().plusSeconds(300));
    when(authorizedClientManager.authorize(any()))
        .thenReturn(new OAuth2AuthorizedClient(registration, REGISTRATION_ID, token));
  }

  @Test
  @DisplayName("Sends the access token as a bearer token")
  void sendsBearerToken() {
    authorizedWithToken("an-access-token");

    new ClientCredentialsTokenFilter(authorizedClientManager, REGISTRATION_ID)
        .filter(request(), next)
        .block();

    assertEquals("Bearer an-access-token", exchanged.get().headers().getFirst("Authorization"));
  }

  @Test
  @DisplayName("Asks for the token of the API it was built for")
  void authorizesTheConfiguredRegistration() {
    authorizedWithToken("an-access-token");

    new ClientCredentialsTokenFilter(authorizedClientManager, REGISTRATION_ID)
        .filter(request(), next)
        .block();

    ArgumentCaptor<OAuth2AuthorizeRequest> captor =
        ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
    org.mockito.Mockito.verify(authorizedClientManager).authorize(captor.capture());
    assertEquals(REGISTRATION_ID, captor.getValue().getClientRegistrationId());
  }

  @Test
  @DisplayName("Fails the request rather than calling the API unauthenticated")
  void failsWhenNoTokenIsAvailable() {
    when(authorizedClientManager.authorize(any())).thenReturn(null);

    StepVerifier.create(
            new ClientCredentialsTokenFilter(authorizedClientManager, REGISTRATION_ID)
                .filter(request(), next))
        .expectErrorMatches(
            error ->
                error instanceof IllegalStateException
                    && error.getMessage().contains(REGISTRATION_ID))
        .verify();
  }
}
