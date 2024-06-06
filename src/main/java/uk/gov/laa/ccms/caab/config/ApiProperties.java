package uk.gov.laa.ccms.caab.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Base properties for an API connection.
 */
@Getter
@Setter
@AllArgsConstructor
abstract class ApiProperties {

  private final String url;
  private final String host;
  private final int port;
  private final String accessToken;

}
