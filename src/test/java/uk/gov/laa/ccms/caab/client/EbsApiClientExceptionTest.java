package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class EbsApiClientExceptionTest {

  @Test
  void constructorWithMessage() {
    String expectedMessage = "Test Exception";
    EbsApiClientException exception = new EbsApiClientException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructorWithMessageAndCause() {
    String expectedMessage = "Test Exception";
    Exception cause = new Exception("Cause Exception");
    EbsApiClientException exception = new EbsApiClientException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
