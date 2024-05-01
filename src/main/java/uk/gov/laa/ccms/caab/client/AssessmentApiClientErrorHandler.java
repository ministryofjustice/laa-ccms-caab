package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provides error-handling capabilities for the Assessment API client interactions.
 */
@Slf4j
@Component
public class AssessmentApiClientErrorHandler extends AbstractApiClientErrorHandler {
  @Override
  public AssessmentApiClientException createException(final String message, final Throwable cause) {
    return new AssessmentApiClientException(message, cause);
  }
}
