package uk.gov.laa.ccms.caab.client;

import java.util.Optional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Base class for API clients, providing utility methods and shared configuration. This class is
 * designed to be extended by specific API client implementations to handle interactions with
 * external services.
 *
 * @author Jamie Briggs
 * @see WebClient
 */
public abstract class BaseApiClient {

  protected final WebClient webClient;

  protected BaseApiClient(WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * Creates a default set of query parameters for API requests. The default query parameters
   * include a pre-set "size" parameter with a value of "1000".
   *
   * @return a {@link MultiValueMap} containing the default query parameters.
   */
  protected static MultiValueMap<String, String> createDefaultQueryParams() {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("size", "1000");
    return queryParams;
  }

  /**
   * Adds a query parameter to the provided {@code queryParams} map. If the given {@code key}
   * already exists in the map, it is overridden by the new value. If the provided {@code value} is
   * null, the key is not added back to the map.
   *
   * @param queryParams the map of query parameters to which the key-value pair is added
   * @param key the key for the query parameter to be added
   * @param value the value associated with the given key; if null, the key is not added
   */
  protected static void addQueryParam(
      MultiValueMap<String, String> queryParams, String key, Object value) {
    // Remove key in case key is being overridden.
    queryParams.remove(key);
    Optional.ofNullable(value).ifPresent(v -> queryParams.add(key, String.valueOf(v)));
  }
}
