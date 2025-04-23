package uk.gov.laa.ccms.caab.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TooManyResultsExceptionTest {

  @Test
  void constructorWithMessage() {
    final String expectedMessage = "Too many results";
    final TooManyResultsException exception = new TooManyResultsException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructorWithMessageAndCause() {
    final String expectedMessage = "Too many results";
    final Throwable cause = new Exception("Cause of too many results");
    final TooManyResultsException exception = new TooManyResultsException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
