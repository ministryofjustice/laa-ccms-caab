package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AvApiClientErrorHandlerTest {

  private final AvApiClientErrorHandler handler = new AvApiClientErrorHandler();

  @Test
  void handleVirusFoundErrorThrowsSafeMessage() {
    AvApiVirusFoundException exception =
        assertThrows(
            AvApiVirusFoundException.class,
            () -> handler.handleVirusFoundError("stream: Eicar-Test-Signature FOUND"));

    assertEquals(AvApiClientErrorHandler.VIRUS_FOUND_MSG, exception.getMessage());
  }
}
