package uk.gov.laa.ccms.caab.config;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.security.ProviderContext;

/**
 * Propagates the active provider firm identifier in the {@code Caab-Provider-Id} header on outbound
 * internal API requests when present, omitting the header cleanly before a provider is established.
 */
public class ProviderIdHeaderFilter implements ExchangeFilterFunction {

  public ProviderIdHeaderFilter() {
    ProviderContext.registerAccessor();
  }

  @Override
  public Mono<ClientResponse> filter(final ClientRequest request, final ExchangeFunction next) {
    return Mono.deferContextual(
        contextView -> {
          final String providerId = ProviderContext.resolveProviderId(contextView);
          if (providerId == null) {
            return next.exchange(request);
          }
          final ClientRequest filteredRequest =
              ClientRequest.from(request)
                  .headers(headers -> headers.set(ProviderContext.HEADER_NAME, providerId))
                  .build();
          return next.exchange(filteredRequest);
        });
  }
}
