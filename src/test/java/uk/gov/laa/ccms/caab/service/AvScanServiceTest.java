package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.laa.ccms.caab.service.AvScanService.SCAN_ERROR_FORMAT;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.client.AvApiClient;
import uk.gov.laa.ccms.caab.client.AvApiClientException;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvScanNotEnabledException;

@ExtendWith(MockitoExtension.class)
public class AvScanServiceTest {

  @Mock
  private AvApiClient avApiClient;

  private AvScanService avScanService;

  private final String caseReferenceNumber = "123";

  private final String providerId = "provId";

  private final String userId = "testUser";

  private final String source = "source";

  private final String filename = "the file name";

  @Test
  void scanInputStream_noClientExceptionThrown() {
    avScanService = new AvScanService(avApiClient, Boolean.TRUE);
    InputStream inputStream = InputStream.nullInputStream();

    avScanService.performAvScan(
        caseReferenceNumber, providerId, userId, source, filename, inputStream);

    verify(avApiClient).scan(inputStream);
  }

  @Test
  void scanInputStream_handlesClientExceptionThrown() {
    avScanService = new AvScanService(avApiClient, Boolean.TRUE);
    InputStream inputStream = InputStream.nullInputStream();
    AvApiClientException avApiClientException = new AvApiClientException("error");

    doThrow(avApiClientException).when(avApiClient)
        .scan(inputStream);

    Exception e =
        assertThrows(AvScanException.class, () -> avScanService.performAvScan(
            caseReferenceNumber, providerId, userId, source, filename, inputStream));

    assertEquals(
        String.format(SCAN_ERROR_FORMAT, filename, avApiClientException.getMessage()),
        e.getMessage());
  }

  @Test
  void avScanNotEnabled_throwsAvScanNotEnabledException() {
    avScanService = new AvScanService(avApiClient, Boolean.FALSE);

    assertThrows(AvScanNotEnabledException.class, () -> avScanService.performAvScan(
        caseReferenceNumber, providerId, userId, source, filename, InputStream.nullInputStream()));
  }
}