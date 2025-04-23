package uk.gov.laa.ccms.caab.client;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.client.AvApiClient.STREAM_INDICATOR;
import static uk.gov.laa.ccms.caab.client.AvApiClient.VIRUS_FOUND_INDICATOR;

import fi.solita.clamav.ClamAVClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvApiClientTest {

  @Mock
  private ClamAVClient clamAvClient;

  @Mock
  private AvApiClientErrorHandler avApiClientErrorHandler;

  @InjectMocks
  private AvApiClient avApiClient;

  @Test
  void scanInputStream_cleanInput_noExceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    byte[] expectedResponse = (STREAM_INDICATOR + "OK").getBytes(StandardCharsets.UTF_8);
    when(clamAvClient.scan(inputStream)).thenReturn(expectedResponse);

    avApiClient.scan(inputStream);

    verifyNoInteractions(avApiClientErrorHandler);
  }

  @Test
  void scanInputStream_nonCleanInput_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    byte[] expectedResponse = (STREAM_INDICATOR + VIRUS_FOUND_INDICATOR + expectedMsg)
        .getBytes(StandardCharsets.UTF_8);
    when(clamAvClient.scan(inputStream)).thenReturn(expectedResponse);

    avApiClient.scan(inputStream);

    verify(avApiClientErrorHandler).handleVirusFoundError(expectedMsg);
  }

  @Test
  void scanInputStream_avIoException_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    IOException ioException = new IOException(expectedMsg);
    when(clamAvClient.scan(inputStream)).thenThrow(ioException);

    avApiClient.scan(inputStream);

    verify(avApiClientErrorHandler).handleScanError(ioException);
  }

  @Test
  void scanInputStream_unknownResponse_exceptionThrown() throws Exception {
    InputStream inputStream = InputStream.nullInputStream();
    String expectedMsg = "hello";
    byte[] expectedResponse = expectedMsg.getBytes(StandardCharsets.UTF_8);
    when(clamAvClient.scan(inputStream)).thenReturn(expectedResponse);

    avApiClient.scan(inputStream);

    verify(avApiClientErrorHandler).handleVirusFoundError(expectedMsg);
  }

}
