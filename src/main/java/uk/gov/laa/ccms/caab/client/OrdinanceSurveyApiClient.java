package uk.gov.laa.ccms.caab.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;

/**
 * Client class responsible for interacting with the ordinance survey api microservice to retrieve
 * address data entities.
 */
@Service
public class OrdinanceSurveyApiClient {

  private static final int MAX_RESULTS = 100;

  private final WebClient osApiWebClient;
  private final String osApiKey;

  public OrdinanceSurveyApiClient(
      final WebClient osApiWebClient, @Value("${os.api.key}") final String osApiKey) {
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
    return getAddresses(postcode, 0, null, new ArrayList<>());
  }

  private Mono<OrdinanceSurveyResponse> getAddresses(
      final String postcode,
      final int offset,
      final Integer expectedTotal,
      final List<OrdinanceSurveyResult> accumulatedResults) {
    return osApiWebClient
        .get()
        .uri(
            builder ->
                builder
                    .path("/search/places/v1/postcode")
                    .queryParam("postcode", postcode)
                    .queryParam("key", osApiKey)
                    .queryParam("maxresults", MAX_RESULTS)
                    .queryParam("offset", offset)
                    .build())
        .retrieve()
        .bodyToMono(OrdinanceSurveyResponse.class)
        .flatMap(
            response -> {
              if (response.getHeader() == null
                  || response.getHeader().getTotalResults() == null
                  || response.getResults() == null) {
                return Mono.error(
                    new OrdinanceSurveyResponseException(
                        "Address lookup response omitted required pagination data"));
              }

              final int totalResults = response.getHeader().getTotalResults();
              if (totalResults < 0 || (expectedTotal != null && expectedTotal != totalResults)) {
                return Mono.error(
                    new OrdinanceSurveyResponseException(
                        "Address lookup returned inconsistent pagination data"));
              }

              final int expectedPageSize = Math.min(MAX_RESULTS, totalResults - offset);
              if (response.getResults().size() != expectedPageSize) {
                return Mono.error(
                    new OrdinanceSurveyResponseException(
                        "Address lookup returned an incomplete result page"));
              }

              accumulatedResults.addAll(response.getResults());
              final int nextOffset = offset + response.getResults().size();
              if (nextOffset < totalResults) {
                return getAddresses(postcode, nextOffset, totalResults, accumulatedResults);
              }

              response.setResults(accumulatedResults);
              return Mono.just(response);
            });
  }
}
