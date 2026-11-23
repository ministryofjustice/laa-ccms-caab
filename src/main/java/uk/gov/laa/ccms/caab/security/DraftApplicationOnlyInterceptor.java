package uk.gov.laa.ccms.caab.security;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.config.UserRole;

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
    if (session != null
        && session.getAttribute(ACTIVE_CASE) instanceof ActiveCase activeCase
        && (activeCase.isDraftApplication()
            || (activeCase.isAmendment() && holdsAmendmentFunctions(request)))) {
      return true;
    }
    throw new AccessDeniedException(
        "Only a draft, or an amendment the user may view, can be changed through these pages");
  }

  /**
   * The functions the amendment pages' own rules need: VC, as an amendment is made to an open case,
   * and CD as well for its client's details.
   */
  private static boolean holdsAmendmentFunctions(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    Set<String> held =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    boolean clientDetails =
        request
            .getRequestURI()
            .startsWith(request.getContextPath() + "/application/sections/client/details");
    return held.contains(UserRole.VIEW_CASE_DETAILS.getCode())
        && (!clientDetails || held.contains(UserRole.VIEW_CLIENT_DETAILS.getCode()));
  }
}
