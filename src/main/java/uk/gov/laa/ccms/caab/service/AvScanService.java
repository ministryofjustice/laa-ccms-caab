package uk.gov.laa.ccms.caab.service;

import static fi.solita.clamav.ClamAVClient.isCleanReply;

import fi.solita.clamav.ClamAVClient;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.exception.AvScanException;

/**
 * Service class to handle calls to an external Antivirus Scanning facility.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvScanService {

  /**
   * Indicator for a virus being found.
   */
  protected static final String VIRUS_FOUND_INDICATOR = "FOUND";

  /**
   * Indicator for the start of a response stream from the external antivirus service.
   */
  protected static final String STREAM_INDICATOR = "stream: ";

  /**
   * Message to return in an exception when a virus has been found.
   */
  protected static final String VIRUS_FOUND_MSG = "Virus found by av scanning service: %s";

  /**
   * Message to return in an exception when an error has occurred while calling the external
   * antivirus service.
   */
  public static final String AV_SERVICE_ERROR_MSG = "Error while calling av scanning service: %s";

  /**
   * A client which handles calls to the antivirus service.
   */
  private final ClamAVClient clamAvClient;

  /**
   * Perform an antivirus scan on the provided input stream.
   *
   * @param inputStream - the input stream to scan for viruses.
   * @throws AvScanException if a virus is reported, or an error occurs in the av service.
   */
  public void performAvScan(final InputStream inputStream) {

    try {
      byte[] reply = clamAvClient.scan(inputStream);
      final String stringResult =  new String(reply).replace(STREAM_INDICATOR, "");

      log.info("********** VIRUS SCAN RESULT ********** {}", stringResult);

      if (!isCleanReply(reply)) {
        final String formattedResponse = stringResult.replace(VIRUS_FOUND_INDICATOR, "");

        log.error("********** Malware scan service non-clean response **********: {}",
            formattedResponse);
        throw new AvScanException(String.format(VIRUS_FOUND_MSG, formattedResponse));
      }
    } catch (IOException e) {
      log.error("********** Malware scan service IOEXCEPTION ********** {}", e.getMessage());
      throw new AvScanException(String.format(AV_SERVICE_ERROR_MSG, e.getMessage()));
    }
  }

}
