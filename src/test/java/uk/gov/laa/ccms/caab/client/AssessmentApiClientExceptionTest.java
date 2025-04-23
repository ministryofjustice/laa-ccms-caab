package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssessmentApiClientExceptionTest {

  @Test
  void constructorWithMessage() {
    final String expectedMessage = "Test Exception";
    final AssessmentApiClientException exception =
        new AssessmentApiClientException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void constructorWithMessageAndCause() {
    final String expectedMessage = "Test Exception";
    final Exception cause = new Exception("Cause Exception");
    final AssessmentApiClientException exception =
        new AssessmentApiClientException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
