package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3ApiFileNotFoundExceptionTest {

  @Test
  void constructorWithMessage() {
    String expectedMessage = "Test Exception";
    S3ApiFileNotFoundException exception = new S3ApiFileNotFoundException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructorWithMessageAndCause() {
    String expectedMessage = "Test Exception";
    Exception cause = new Exception("Cause Exception");
    S3ApiFileNotFoundException exception = new S3ApiFileNotFoundException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
