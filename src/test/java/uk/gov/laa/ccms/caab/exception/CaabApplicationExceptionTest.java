package uk.gov.laa.ccms.caab.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaabApplicationExceptionTest {

  @Test
  public void testConstructorWithMessage() {
    final String expectedMessage = "Test Exception";
    final CaabApplicationException exception = new CaabApplicationException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testConstructorWithMessageAndCause() {
    final String expectedMessage = "Test Exception";
    final Exception cause = new Exception("Cause Exception");
    final CaabApplicationException exception = new CaabApplicationException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}