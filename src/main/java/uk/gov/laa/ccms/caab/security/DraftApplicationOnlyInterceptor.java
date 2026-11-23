package uk.gov.laa.ccms.caab.security;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

/**
 * Refuses the application's editing pages unless the session holds a draft application. Those pages
 * act on the application in the session, whichever URL they are reached by, so without this an
 * amendment or submitted case left in the session could be changed through them without the
 * amendment rules, which need VC. The refusal is handled by {@code GlobalExceptionHandler}, which
 * logs it as a denial.
 */
public class DraftApplicationOnlyInterceptor implements HandlerInterceptor {

  /**
   * The application pages that edit, submit or abandon the application in the session. The sections
   * page itself is left out, as it builds the draft's active case.
   */
  public static final List<String> GUARDED_PATHS =
      List.of(
          "/application/sections/*/**",
          "/application/case-costs",
          "/application/proceedings/**",
          "/application/proceedings-and-costs",
          "/application/prior-authorities/**",
          "/application/evidence/**",
          "/application/assessments/*/remove",
          "/application/submit/**",
          "/application/validate",
          "/application/declaration",
          "/application/abandon",
          "/application/abandon/**");

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return true;
    }
    if (session.getAttribute(ACTIVE_CASE) instanceof ActiveCase activeCase) {
      if (activeCase.isDraftApplication()) {
        return true;
      }
    } else if (!submittedCaseInSession(session)) {
      return true;
    }
    throw new AccessDeniedException(
        "Only a draft application can be changed through the application pages");
  }

  /** With no active case, ActiveCaseModelAdvice builds one from a submitted case in the session. */
  private static boolean submittedCaseInSession(HttpSession session) {
    return session.getAttribute(CASE) instanceof ApplicationDetail;
  }
}
