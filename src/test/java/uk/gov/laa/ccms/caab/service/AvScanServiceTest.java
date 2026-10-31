package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.laa.ccms.caab.service.AvScanService.SCAN_ERROR_FORMAT;
import static uk.gov.laa.ccms.caab.service.AvScanService.VIRUS_FOUND_ERROR_FORMAT;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.client.AvApiClient;
import uk.gov.laa.ccms.caab.client.AvApiClientException;
import uk.gov.laa.ccms.caab.client.AvApiVirusFoundException;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger.Outcome;

@ExtendWith(MockitoExtension.class)
public class AvScanServiceTest {

  @Mock private AvApiClient avApiClient;

  @Mock private SecurityEventLogger securityEventLogger;

  private AvScanService avScanService;

  private final String caseReferenceNumber = "123";

  private final Integer providerId = 456;

  private final String userId = "789";

  private final CcmsModule source = CcmsModule.APPLICATION;

  private final String filename = "the file name";

  @Test
  void scanInputStream_noClientExceptionThrown() {
    avScanService = new AvScanService(avApiClient, Boolean.TRUE, securityEventLogger);
    InputStream inputStream = InputStream.nullInputStream();

    avScanService.performAvScan(
        caseReferenceNumber, providerId, userId, source, filename, inputStream);

    verify(avApiClient).scan(inputStream);
    verify(securityEventLogger).fileScanned(Outcome.SUCCESS, caseReferenceNumber, source);
  }

  @Test
  void scanInputStream_handlesAvApiClientExceptionThrown() {
    avScanService = new AvScanService(avApiClient, Boolean.TRUE, securityEventLogger);
    InputStream inputStream = InputStream.nullInputStream();
    AvApiClientException avApiClientException = new AvApiClientException("error");

    doThrow(avApiClientException).when(avApiClient).scan(inputStream);

    Exception e =
        assertThrows(
            AvScanException.class,
            () ->
                avScanService.performAvScan(
                    caseReferenceNumber, providerId, userId, source, filename, inputStream));

    assertEquals(SCAN_ERROR_FORMAT.formatted(filename), e.getMessage());
    verify(securityEventLogger).fileScanned(Outcome.FAILURE, caseReferenceNumber, source);
  }

  @Test
  void scanInputStream_handlesAvVirusFoundExceptionThrown() {
    avScanService = new AvScanService(avApiClient, Boolean.TRUE, securityEventLogger);
    InputStream inputStream = InputStream.nullInputStream();
    AvApiVirusFoundException avApiVirusFoundException = new AvApiVirusFoundException("error");

    doThrow(avApiVirusFoundException).when(avApiClient).scan(inputStream);

    Exception e =
        assertThrows(
            AvVirusFoundException.class,
            () ->
                avScanService.performAvScan(
                    caseReferenceNumber, providerId, userId, source, filename, inputStream));

    assertEquals(VIRUS_FOUND_ERROR_FORMAT.formatted(filename), e.getMessage());
    verify(securityEventLogger).fileScanned(Outcome.DENIED, caseReferenceNumber, source);
  }

  @Test
  void avScanNotEnabled_scanNotPerformed() {
    avScanService = new AvScanService(avApiClient, Boolean.FALSE, securityEventLogger);

    verifyNoInteractions(avApiClient, securityEventLogger);
  }
}
