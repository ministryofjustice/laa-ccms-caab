package uk.gov.laa.ccms.caab.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Provides error-handling capabilities for the CAAB API client interactions.
 */
@Slf4j
@Component
public class CaabApiClientErrorHandler extends AbstractApiClientErrorHandler {
  @Override
  public CaabApplicationException createException(final String message, final Throwable cause) {
    return new CaabApplicationException(message, cause);
  }

}
