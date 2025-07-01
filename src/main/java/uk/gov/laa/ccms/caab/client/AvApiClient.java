package uk.gov.laa.ccms.caab.client;

import static fi.solita.clamav.ClamAVClient.isCleanReply;

import fi.solita.clamav.ClamAVClient;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.exception.AvScanException;

/** Client to handle interactions with the ClamAV antivirus service. */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvApiClient {

  /** Indicator for a virus being found. */
  protected static final String VIRUS_FOUND_INDICATOR = "FOUND";

  /** Indicator for the start of a response stream from the external antivirus service. */
  protected static final String STREAM_INDICATOR = "stream: ";

  /** A client which handles calls to the antivirus service. */
  private final ClamAVClient clamAvClient;

  /** Error handler for calls to the ClamAv service. */
  private final AvApiClientErrorHandler avApiClientErrorHandler;

  /**
   * Perform an antivirus scan on the provided input stream.
   *
   * @param inputStream - the input stream to scan for viruses.
   * @throws AvScanException if a virus is reported, or an error occurs in the av service.
   */
  public void scan(final InputStream inputStream) {
    try {
      byte[] reply = clamAvClient.scan(inputStream);
      final String stringResult = new String(reply).replace(STREAM_INDICATOR, "");

      log.info("********** VIRUS SCAN RESULT ********** {}", stringResult);

      if (!isCleanReply(reply)) {
        final String formattedResponse = stringResult.replace(VIRUS_FOUND_INDICATOR, "");

        avApiClientErrorHandler.handleVirusFoundError(formattedResponse);
      }
    } catch (IOException e) {
      avApiClientErrorHandler.handleScanError(e);
    }
  }
}
