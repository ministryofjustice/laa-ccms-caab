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
   * The only guarded pages an amendment may use: the validation corrections post its provider
   * details and correspondence address here, and its client's details share these pages. Every
   * other amendment page is under /amendments, behind the amendment's own rules.
   */
  private static final List<PathPattern> AMENDMENT_CORRECTION_PATHS =
      Stream.of(
              "/application/sections/provider-details",
              "/application/sections/correspondence-address",
              "/application/sections/client/details/**")
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
                && isAmendmentCorrectionPath(request)
                && holdsAmendmentFunctions(request)))) {
      return true;
    }
    throw new AccessDeniedException(
        "Only a draft, or an amendment the user may view, can be changed through these pages");
  }

  private static boolean isAmendmentCorrectionPath(HttpServletRequest request) {
    // Decoded, as MVC matches handlers on decoded segments.
    PathContainer path =
        PathContainer.parsePath(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
    return AMENDMENT_CORRECTION_PATHS.stream().anyMatch(pattern -> pattern.matches(path));
  }

  /**
   * The functions the amendment pages' own rules need: VC, as an amendment is made to an open case,
   * and CD as well for its client's details.
   */
  private static boolean holdsAmendmentFunctions(HttpServletRequest request) {
    boolean clientDetails =
        UrlPathHelper.defaultInstance
            .getPathWithinApplication(request)
            .startsWith("/application/sections/client/details");
    return clientDetails
        ? HeldFunctions.holdsAll(UserRole.VIEW_CASE_DETAILS, UserRole.VIEW_CLIENT_DETAILS)
        : HeldFunctions.holdsAll(UserRole.VIEW_CASE_DETAILS);
  }
}
