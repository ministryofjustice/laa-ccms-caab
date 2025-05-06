package uk.gov.laa.ccms.caab.constants;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component that holds constants for maximum search results in the application.
 */
@Component
@Getter
public class SearchConstants {

  /**
   * the maximum amount of results to return for an EBS client search.
   */
  @Value("${search.max-results.clients:100}")
  private Integer maxSearchResultsClients;

  /**
   * the maximum amount of results to return for an EBS case search.
   */
  @Value("${search.max-results.cases:100}")
  private Integer maxSearchResultsCases;

  /**
   * the maximum amount of results to return for an EBS organisation search.
   */
  @Value("${search.max-results.organisations:100}")
  private Integer maxSearchResultsOrganisations;
}
