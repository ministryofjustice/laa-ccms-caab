package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.laa.ccms.caab.constants.CaseContext;

@ExtendWith(MockitoExtension.class)
class SubmissionUtilTest {

  @Mock private HttpSession session;

  @Test
  void requireSessionAttribute_withNonNullAttribute_doesNotThrow() {
    SubmissionUtil.requireSessionAttribute(new Object(), "attributeName");
  }

  @Test
  void requireSessionAttribute_withNullAttribute_throwsIllegalStateException() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> SubmissionUtil.requireSessionAttribute(null, "testAttribute"));
    assertEquals("Missing session attribute 'testAttribute'", exception.getMessage());
  }

  @Test
  void redirectToSubmissionResult_withConfirmedResult_returnsConfirmedRedirect() {
    when(session.getAttribute(SUBMISSION_RESULT)).thenReturn("confirmed");
    String result =
        SubmissionUtil.redirectToSubmissionResult(session, CaseContext.APPLICATION, "test-type");
    assertEquals("redirect:/application/test-type/confirmed", result);
  }

  @Test
  void redirectToSubmissionResult_withFailedResult_returnsFailedRedirect() {
    when(session.getAttribute(SUBMISSION_RESULT)).thenReturn("failed");
    String result =
        SubmissionUtil.redirectToSubmissionResult(session, CaseContext.AMENDMENTS, "test-type");
    assertEquals("redirect:/amendments/test-type/failed", result);
  }

  @Test
  void redirectToSubmissionResult_withNullResult_returnsFailedRedirect() {
    when(session.getAttribute(SUBMISSION_RESULT)).thenReturn(null);
    String result =
        SubmissionUtil.redirectToSubmissionResult(session, CaseContext.APPLICATION, "test-type");
    assertEquals("redirect:/application/test-type/failed", result);
  }
}
