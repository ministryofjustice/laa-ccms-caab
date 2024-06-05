package uk.gov.laa.ccms.caab.service;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.AvApiClient;
import uk.gov.laa.ccms.caab.client.AvApiClientException;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvScanNotEnabledException;

/**
 * Service class to handle calls to an external Antivirus Scanning facility.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvScanService {

  protected static final String SCAN_ERROR_FORMAT =
      "Error while scanning %s for viruses: %s. File has not been uploaded";

  /**
   * A client which handles calls to the antivirus service.
   */
  private final AvApiClient avApiClient;

  /**
   * Flag to indicate whether the antivirus scanning service is enabled.
   */
  @Value("${av.enabled}")
  private final Boolean enabled;

  /**
   * Perform an antivirus scan on the provided input stream.
   *
   * @param caseReferenceNumber - the related case reference for auditing purposes
   * @param providerId - the related provider id for auditing purposes.
   * @param userId - the user requesting the file upload.
   * @param source - the source of the file upload.
   * @param inputStream - the input stream to scan for viruses.
   * @throws AvScanException if a virus is reported, or an error occurs in the av service.
   */
  public void performAvScan(
      final String caseReferenceNumber,
      final String providerId,
      final String userId,
      final String source,
      final String filename,
      final InputStream inputStream) {

    if (!enabled) {
      throw new AvScanNotEnabledException();
    }

    try {
      avApiClient.scan(inputStream);
    } catch (AvApiClientException e) {
      /*
       * todo: Log common audit event to record the scan failure once audit microservice in place.
       *  Include caseReferenceNumber etc in audit log.
       */

      log.error("********** Malware scan service threw exception **********", e);
      throw new AvScanException(
          String.format(SCAN_ERROR_FORMAT, filename, e.getMessage()), e);
    }
  }

}
