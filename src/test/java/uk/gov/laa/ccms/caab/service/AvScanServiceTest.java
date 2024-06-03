package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.service.AvScanService.AV_SERVICE_ERROR_MSG;
import static uk.gov.laa.ccms.caab.service.AvScanService.STREAM_INDICATOR;
import static uk.gov.laa.ccms.caab.service.AvScanService.VIRUS_FOUND_INDICATOR;
import static uk.gov.laa.ccms.caab.service.AvScanService.VIRUS_FOUND_MSG;

import fi.solita.clamav.ClamAVClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.exception.AvScanException;

@ExtendWith(MockitoExtension.class)
public class AvScanServiceTest {

  @Mock
  private ClamAVClient avApiClient;

  @InjectMocks
  private AvScanService avScanService;

  @Test
  void scanInputStream_cleanInput_noExceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    byte[] expectedResponse = (STREAM_INDICATOR + "OK").getBytes(StandardCharsets.UTF_8);
    when(avApiClient.scan(inputStream)).thenReturn(expectedResponse);

    avScanService.performAvScan(inputStream);
  }

  @Test
  void scanInputStream_nonCleanInput_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    byte[] expectedResponse = (STREAM_INDICATOR + VIRUS_FOUND_INDICATOR + expectedMsg)
        .getBytes(StandardCharsets.UTF_8);
    when(avApiClient.scan(inputStream)).thenReturn(expectedResponse);

    Exception e =
        assertThrows(AvScanException.class, () -> avScanService.performAvScan(inputStream));

    assertEquals(String.format(VIRUS_FOUND_MSG, expectedMsg), e.getMessage());
  }

  @Test
  void scanInputStream_avIoException_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    when(avApiClient.scan(inputStream)).thenThrow(new IOException(expectedMsg));

    Exception e =
        assertThrows(AvScanException.class, () -> avScanService.performAvScan(inputStream));

    assertEquals(String.format(AV_SERVICE_ERROR_MSG, expectedMsg), e.getMessage());
  }

  @Test
  void scanInputStream_unknownResponse_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    byte[] expectedResponse = expectedMsg.getBytes(StandardCharsets.UTF_8);
    when(avApiClient.scan(inputStream)).thenReturn(expectedResponse);

    Exception e =
        assertThrows(AvScanException.class, () -> avScanService.performAvScan(inputStream));

    assertEquals(String.format(VIRUS_FOUND_MSG, expectedMsg), e.getMessage());
  }

}