package uk.gov.laa.ccms.caab.security;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

/**
 * Refuses the application's client pages for a submitted case, whose client is amended through the
 * amendment pages, which need CD. A draft application has no submitted case behind it.
 */
public class SubmittedCaseClientInterceptor implements HandlerInterceptor {

  private final AccessDeniedHandler refusal = new NotAuthorisedAccessDeniedHandler();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    HttpSession session = request.getSession(false);
    if (session == null
        || !(session.getAttribute(CASE) instanceof ApplicationDetail submittedCase)) {
      return true;
    }
    // With no active case set, ActiveCaseModelAdvice builds one from the submitted case.
    if (session.getAttribute(ACTIVE_CASE) instanceof ActiveCase activeCase
        && !Objects.equals(
            activeCase.getCaseReferenceNumber(), submittedCase.getCaseReferenceNumber())) {
      return true;
    }
    refusal.handle(
        request,
        response,
        new AccessDeniedException(
            "A submitted case's client is amended through the amendment pages"));
    return false;
  }
}
