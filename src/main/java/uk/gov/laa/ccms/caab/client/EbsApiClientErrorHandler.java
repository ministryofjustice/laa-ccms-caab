package uk.gov.laa.ccms.caab.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provides error-handling capabilities for the EBS API client interactions.
 */
@Component
public class EbsApiClientErrorHandler extends AbstractApiClientErrorHandler {

  @Override
  public EbsApiClientException createException(final String message, final Throwable cause) {
    return new EbsApiClientException(message, cause);
  }

}
