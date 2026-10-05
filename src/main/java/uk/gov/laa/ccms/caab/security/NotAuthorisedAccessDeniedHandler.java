package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

/**
 * Handles a request for an action the user does not hold the function for, the way the legacy PUI
 * does: the user is returned to the page they came from, which then shows "You are not authorized
 * to perform this function."
 *
 * <p>That page is the recorded page matching the referring one, if it was shown by a plain page
 * load, so loading it again is safe; otherwise the last page that was; otherwise the home page. The
 * redirect always uses the recorded page, never the Referer header itself. CSRF failures and
 * background requests get the default 403.
 */
public class NotAuthorisedAccessDeniedHandler implements AccessDeniedHandler {

  /** Model attribute set on the page a user is returned to after a refused action. */
  public static final String NOT_AUTHORISED_ATTRIBUTE = "notAuthorised";

  private static final String HOME_PATH = "/home";

  private final AccessDeniedHandler forbiddenHandler = new AccessDeniedHandlerImpl();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    if (accessDeniedException instanceof CsrfException
        || !RefusedActionSession.isPageNavigation(request)) {
      forbiddenHandler.handle(request, response, accessDeniedException);
      return;
    }

    HttpSession session = request.getSession();
    // A refused page load must not return to itself. A refused form post may return to the page
    // it was posted from, which was shown by a page load and so can be loaded again.
    boolean refusedPageLoad = "GET".equals(request.getMethod());
    Predicate<String> notRefusedPage =
        page ->
            !refusedPageLoad || !RefusedActionSession.pathOf(page).equals(request.getRequestURI());
    String target =
        refererOnThisSite(request)
            .flatMap(referer -> RefusedActionSession.findRenderedPage(session, referer))
            .filter(notRefusedPage)
            .or(() -> RefusedActionSession.lastRenderedPage(session).filter(notRefusedPage))
            .filter(page -> RefusedActionSession.isLocalPage(page, request.getContextPath()))
            .orElse(request.getContextPath() + HOME_PATH);

    RefusedActionSession.markNotAuthorised(session);
    response.sendRedirect(target);
  }

  /** The referring page as a path and query, if it is a page of this application. */
  private static Optional<String> refererOnThisSite(HttpServletRequest request) {
    String referer = request.getHeader(HttpHeaders.REFERER);
    if (referer == null) {
      return Optional.empty();
    }
    try {
      URI uri = new URI(referer);
      String path = uri.getRawPath();
      if (!request.getServerName().equalsIgnoreCase(uri.getHost())
          || path == null
          || !path.startsWith(request.getContextPath() + "/")) {
        return Optional.empty();
      }
      return Optional.of(uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery());
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }
}
