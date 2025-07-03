package uk.gov.laa.ccms.caab.service;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.AvApiClient;
import uk.gov.laa.ccms.caab.client.AvApiClientException;
import uk.gov.laa.ccms.caab.client.AvApiVirusFoundException;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;

/** Service class to handle calls to an external Antivirus Scanning facility. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvScanService {

  protected static final String VIRUS_FOUND_ERROR_FORMAT =
      "Virus found while scanning %s: %s. File has not been uploaded";

  protected static final String SCAN_ERROR_FORMAT =
      "Error while scanning %s for viruses: %s. File has not been uploaded";

  /** A client which handles calls to the antivirus service. */
  private final AvApiClient avApiClient;

  /** Flag to indicate whether the antivirus scanning service is enabled. */
  @Value("${av.enabled}")
  private final Boolean enabled;

  /**
   * Perform an antivirus scan on the provided input stream.
   *
   * @param caseReferenceNumber - the related case reference for auditing purposes
   * @param providerId - the related provider id for auditing purposes.
   * @param userId - the user requesting the file upload.
   * @param ccmsModule - the source of the file upload.
   * @param inputStream - the input stream to scan for viruses.
   * @throws AvVirusFoundException if a virus is reported.
   * @throws AvScanException if an error occurs in the av service.
   */
  public void performAvScan(
      final String caseReferenceNumber,
      final Integer providerId,
      final String userId,
      final CcmsModule ccmsModule,
      final String filename,
      final InputStream inputStream) {

    if (!enabled) {
      log.warn("Antivirus service is currently disabled. Files will not be scanned for malware.");
    } else {
      try {
        avApiClient.scan(inputStream);
      } catch (AvApiVirusFoundException virusFoundException) {
        /*
         * todo: CCLS-2228 - Log common audit event to record the scan failure once audit
         *  microservice in place. Include caseReferenceNumber etc in audit log.
         */
        log.error(
            "********** Malware scan service reported virus found **********", virusFoundException);
        throw new AvVirusFoundException(
            VIRUS_FOUND_ERROR_FORMAT.formatted(filename, virusFoundException.getMessage()),
            virusFoundException);

      } catch (AvApiClientException avApiClientException) {
        /*
         * todo: CCLS-2228 - Log common audit event to record the scan failure once audit
         *  microservice in place. Include caseReferenceNumber etc in audit log.
         */

        log.error(
            "********** Malware scan service threw exception **********", avApiClientException);
        throw new AvScanException(
            SCAN_ERROR_FORMAT.formatted(filename, avApiClientException.getMessage()),
            avApiClientException);
      }
    }
  }
}
