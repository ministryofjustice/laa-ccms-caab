package uk.gov.laa.ccms.caab.client;


import org.springframework.stereotype.Component;

/**
 * Provides error-handling capabilities for the SOA API client interactions.
 */
@Component
public class SoaApiClientErrorHandler extends AbstractApiClientErrorHandler {

  @Override
  public SoaApiClientException createException(final String message, final Throwable cause) {
    return new SoaApiClientException(message, cause);
  }

}
