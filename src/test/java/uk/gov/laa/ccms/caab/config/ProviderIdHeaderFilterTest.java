package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uk.gov.laa.ccms.caab.security.ProviderContext;

@ExtendWith(MockitoExtension.class)
class ProviderIdHeaderFilterTest {

  @Mock private OAuth2AuthorizedClientManager authorizedClientManager;

  private final ProviderIdHeaderFilter filter = new ProviderIdHeaderFilter();

  @AfterEach
  void tearDown() {
    ProviderContext.clear();
  }

  private static ClientRequest request() {
    return ClientRequest.create(HttpMethod.GET, URI.create("http://an-api/resource")).build();
  }

  @Test
  void attachesCaabProviderIdHeaderWhenProviderContextIsSet() {
    final AtomicReference<ClientRequest> captured = new AtomicReference<>();
    final ExchangeFunction next =
        req -> {
          captured.set(req);
          return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

    ProviderContext.setProviderId("12345");

    filter.filter(request(), next).block();

    assertThat(captured.get().headers().getFirst(ProviderContext.HEADER_NAME)).isEqualTo("12345");
  }

  @Test
  void omitsCaabProviderIdHeaderWhenProviderContextIsAbsent() {
    final AtomicReference<ClientRequest> captured = new AtomicReference<>();
    final ExchangeFunction next =
        req -> {
          captured.set(req);
          return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

    filter.filter(request(), next).block();

    assertThat(captured.get().headers().containsHeader(ProviderContext.HEADER_NAME)).isFalse();
  }

  @Test
  void propagatesCaabProviderIdAcrossAsyncReactiveHopsAndClientCredentialsFilter() {
    final ClientRegistration registration =
        ClientRegistration.withRegistrationId("caab-api")
            .clientId("client")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://idp/token")
            .build();
    final OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "s2s-token",
            Instant.now(),
            Instant.now().plusSeconds(300));
    when(authorizedClientManager.authorize(any()))
        .thenReturn(new OAuth2AuthorizedClient(registration, "caab-api", token));

    final List<ClientRequest> capturedRequests = new CopyOnWriteArrayList<>();
    final List<String> executionThreads = new CopyOnWriteArrayList<>();
    final ExchangeFunction terminal =
        req -> {
          capturedRequests.add(req);
          executionThreads.add(Thread.currentThread().getName());
          return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

    final ClientCredentialsTokenFilter tokenFilter =
        new ClientCredentialsTokenFilter(authorizedClientManager, "caab-api");
    final ExchangeFunction chained =
        req -> filter.filter(req, r -> tokenFilter.filter(r, terminal));

    final String callerThread = Thread.currentThread().getName();
    ProviderContext.setProviderId("67890");

    Mono.just("start")
        .publishOn(Schedulers.parallel())
        .flatMap(ignored -> chained.exchange(request()))
        .publishOn(Schedulers.boundedElastic())
        .flatMap(ignored -> chained.exchange(request()))
        .block();

    assertThat(capturedRequests).hasSize(2);
    assertThat(capturedRequests)
        .allSatisfy(
            req -> {
              assertThat(req.headers().getFirst(ProviderContext.HEADER_NAME)).isEqualTo("67890");
              assertThat(req.headers().getFirst("Authorization")).isEqualTo("Bearer s2s-token");
            });
    assertThat(executionThreads).noneMatch(callerThread::equals);
  }
}
