package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Provides error-handling capabilities for the ClamAV antivirus client interactions. */
@Component
@Slf4j
public class AvApiClientErrorHandler {

  /** Message to return in an exception when a virus has been found. */
  protected static final String VIRUS_FOUND_MSG = "Virus found by av scanning service: %s";

  /**
   * Message to return in an exception when an error has occurred while calling the external
   * antivirus service.
   */
  public static final String CLAM_AV_FAILURE_ERROR_MSG =
      "Failure in ClamAv process while scanning inputstream";

  /**
   * Handles an exception response from the ClamAv scan, wrapping and throwing a {@link
   * AvApiClientException}.
   *
   * @param e the exception thrown during the API operation.
   * @throws AvApiClientException wrapping the original throwable.
   */
  public void handleScanError(final Throwable e) throws AvApiClientException {
    log.error(CLAM_AV_FAILURE_ERROR_MSG, e);
    throw new AvApiClientException(CLAM_AV_FAILURE_ERROR_MSG, e);
  }

  /**
   * Handles a non-clean response from the ClamAv scan, wrapping and throwing a {@link
   * AvApiVirusFoundException}.
   *
   * @param responseMsg the response from ClamAv indicating a non-clean scan result.
   * @throws AvApiVirusFoundException wrapping the response from ClamAv.
   */
  public void handleVirusFoundError(final String responseMsg) throws AvApiVirusFoundException {
    final String message = VIRUS_FOUND_MSG.formatted(responseMsg);
    log.error(message);
    throw new AvApiVirusFoundException(message);
  }
}
