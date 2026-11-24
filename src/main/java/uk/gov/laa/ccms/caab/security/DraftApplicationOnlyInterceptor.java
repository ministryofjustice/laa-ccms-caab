package uk.gov.laa.ccms.caab.security;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.server.PathContainer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
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

  /**
   * Submitting or abandoning through these creates or ends the application's case, so they are for
   * a draft only: an amendment is validated and submitted under /amendments, under SUBAMD.
   */
  private static final List<PathPattern> DRAFT_ONLY_PATHS =
      Stream.of(
              "/application/validate",
              "/application/submit/**",
              "/application/declaration",
              "/application/abandon",
              "/application/abandon/**")
          .map(PathPatternParser.defaultInstance::parse)
          .toList();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(ACTIVE_CASE) instanceof ActiveCase activeCase
        && (activeCase.isDraftApplication()
            || (activeCase.isAmendment()
                && !isDraftOnlyPath(request)
                && holdsAmendmentFunctions(request)))) {
      return true;
    }
    throw new AccessDeniedException(
        "Only a draft, or an amendment the user may view, can be changed through these pages");
  }

  /**
   * The functions the amendment pages' own rules need: VC, as an amendment is made to an open case,
   * and CD as well for its client's details.
   */
  private static boolean isDraftOnlyPath(HttpServletRequest request) {
    PathContainer path =
        PathContainer.parsePath(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
    return DRAFT_ONLY_PATHS.stream().anyMatch(pattern -> pattern.matches(path));
  }

  private static boolean holdsAmendmentFunctions(HttpServletRequest request) {
    // Decoded, as MVC matches handlers on decoded segments.
    boolean clientDetails =
        UrlPathHelper.defaultInstance
            .getPathWithinApplication(request)
            .startsWith("/application/sections/client/details");
    return clientDetails
        ? HeldFunctions.holdsAll(UserRole.VIEW_CASE_DETAILS, UserRole.VIEW_CLIENT_DETAILS)
        : HeldFunctions.holdsAll(UserRole.VIEW_CASE_DETAILS);
  }
}
