package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;

import jakarta.servlet.http.HttpSession;
import lombok.experimental.UtilityClass;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/** Utility class for submission-related operations. */
@UtilityClass
public class SubmissionUtil {

  private static final String SUBMISSION_CONFIRMED = "confirmed";
  private static final String SUBMISSION_FAILED = "failed";

  /**
   * Returns {@code true} when the session holds a confirmed submission result, indicating the user
   * has already successfully submitted and any re-attempt should be blocked.
   *
   * @param session the current HTTP session
   * @return {@code true} if the submission has already been confirmed
   */
  public static boolean isAlreadySubmitted(final HttpSession session) {
    return SUBMISSION_CONFIRMED.equals(session.getAttribute(SUBMISSION_RESULT));
  }

  /**
   * Requires that a session attribute is not null.
   *
   * @param attribute the attribute to check
   * @param attributeName the name of the attribute
   * @throws IllegalStateException if the attribute is null
   */
  public static void requireSessionAttribute(final Object attribute, final String attributeName) {
    if (attribute == null) {
      throw new IllegalStateException("Missing session attribute '%s'".formatted(attributeName));
    }
  }

  /**
   * Redirects to the submission result page.
   *
   * @param session the HTTP session
   * @param caseContext the case context
   * @param submissionType the submission type
   * @return the redirect string
   */
  public static String redirectToSubmissionResult(
      final HttpSession session, final CaseContext caseContext, final String submissionType) {
    final String submissionResult = (String) session.getAttribute(SUBMISSION_RESULT);
    final String resultPath =
        SUBMISSION_CONFIRMED.equals(submissionResult) ? SUBMISSION_CONFIRMED : SUBMISSION_FAILED;
    return "redirect:/%s/%s/%s".formatted(caseContext.getPathValue(), submissionType, resultPath);
  }
}
