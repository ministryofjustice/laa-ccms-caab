package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssessmentApiClientExceptionTest {

  @Test
  public void testConstructorWithMessage() {
    final String expectedMessage = "Test Exception";
    final AssessmentApiClientException exception =
        new AssessmentApiClientException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testConstructorWithMessageAndCause() {
    final String expectedMessage = "Test Exception";
    final Exception cause = new Exception("Cause Exception");
    final AssessmentApiClientException exception =
        new AssessmentApiClientException(expectedMessage, cause);

    assertEquals(expectedMessage, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
