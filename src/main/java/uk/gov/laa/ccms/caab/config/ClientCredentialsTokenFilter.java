package uk.gov.laa.ccms.caab.config;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Attaches a client credentials bearer token to every request made to an API.
 *
 * <p>The security context is deliberately not consulted. These calls are app-only, so the token
 * identifies this application through its app role rather than the signed-in user - EntraID is not
 * asked to act on a user's behalf. That also keeps the token available on whichever thread the
 * exchange runs on.
 */
class ClientCredentialsTokenFilter implements ExchangeFilterFunction {

  private final OAuth2AuthorizedClientManager authorizedClientManager;
  private final String registrationId;

  ClientCredentialsTokenFilter(
      final OAuth2AuthorizedClientManager authorizedClientManager, final String registrationId) {
    this.authorizedClientManager = authorizedClientManager;
    this.registrationId = registrationId;
  }

  @Override
  public Mono<ClientResponse> filter(final ClientRequest request, final ExchangeFunction next) {
    final OAuth2AuthorizeRequest authorizeRequest =
        OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
            .principal(registrationId)
            .build();

    // authorize() blocks while it fetches a token. filter() runs on whichever thread subscribes,
    // and a call made inside another response's flatMap subscribes on a Netty event-loop thread -
    // blocking one of those stalls every other exchange it carries.
    return Mono.fromCallable(() -> authorizedClientManager.authorize(authorizeRequest))
        .subscribeOn(Schedulers.boundedElastic())
        .switchIfEmpty(
            Mono.error(
                new IllegalStateException(
                    "No access token available for client registration [%s]"
                        .formatted(registrationId))))
        .flatMap(
            authorizedClient ->
                next.exchange(
                    ClientRequest.from(request)
                        .headers(
                            headers ->
                                headers.setBearerAuth(
                                    authorizedClient.getAccessToken().getTokenValue()))
                        .build()));
  }
}
