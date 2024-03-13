package uk.gov.laa.ccms.caab.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;

/**
 * Client class responsible for interacting with the ordinance survey api microservice to
 * retrieve address data entities.
 */
@Service
public class OrdinanceSurveyApiClient {

  private final WebClient osApiWebClient;
  private final String osApiKey;

  public OrdinanceSurveyApiClient(
      final WebClient osApiWebClient,
      @Value("${os.api.key}") final String osApiKey) {
    this.osApiWebClient = osApiWebClient;
    this.osApiKey = osApiKey;
  }

  /**
   * Returns a string of addresses.
   *
   * @param postcode the ID associated with the user login
   * @return a Mono signaling the returned address payload
   */
  public Mono<OrdinanceSurveyResponse> getAddresses(final String postcode) {
    return osApiWebClient
      .get()
          .uri(builder -> builder.path("/search/places/v1/postcode")
              .queryParam("postcode", postcode)
              .queryParam("key", osApiKey)
              .build())
          .retrieve()
          .bodyToMono(OrdinanceSurveyResponse.class);
  }

}
