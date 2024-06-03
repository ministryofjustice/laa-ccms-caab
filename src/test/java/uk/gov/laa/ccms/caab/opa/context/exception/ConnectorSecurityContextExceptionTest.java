package uk.gov.laa.ccms.caab.opa.context.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectorSecurityContextExceptionTest {

  @Test
  void shouldConstructWithNoArgs() {
    final ConnectorSecurityContextException exception =
        new ConnectorSecurityContextException();
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void shouldConstructWithMessage() {
    final String message = "Test message";
    final ConnectorSecurityContextException exception =
        new ConnectorSecurityContextException(message);
    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void shouldConstructWithCause() {
    final Throwable cause = new RuntimeException("Test cause");
    final ConnectorSecurityContextException exception =
        new ConnectorSecurityContextException(cause);
    assertEquals("java.lang.RuntimeException: Test cause", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void shouldConstructWithMessageAndCause() {
    final String message = "Test message";
    final Throwable cause = new RuntimeException("Test cause");
    final ConnectorSecurityContextException exception =
        new ConnectorSecurityContextException(message, cause);
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}