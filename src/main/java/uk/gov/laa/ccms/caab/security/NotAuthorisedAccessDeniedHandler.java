package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * Handles a request for an action the user does not hold the function for, the way the legacy PUI
 * does: the user is returned to the page they came from, which then shows "You are not authorized
 * to perform this function." CSRF failures keep the default 403.
 */
public class NotAuthorisedAccessDeniedHandler implements AccessDeniedHandler {

  /** Flash attribute set when the user is returned to a page after a refused action. */
  public static final String NOT_AUTHORISED_ATTRIBUTE = "notAuthorised";

  private static final String HOME_PATH = "/home";

  private final AccessDeniedHandler csrfHandler = new AccessDeniedHandlerImpl();
  private final SessionFlashMapManager flashMapManager = new SessionFlashMapManager();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    if (accessDeniedException instanceof CsrfException) {
      csrfHandler.handle(request, response, accessDeniedException);
      return;
    }

    URI target =
        refererOnThisSite(request)
            .filter(referer -> !referer.getRawPath().equals(request.getRequestURI()))
            .orElseGet(() -> URI.create(request.getContextPath() + HOME_PATH));

    FlashMap flashMap = new FlashMap();
    flashMap.put(NOT_AUTHORISED_ATTRIBUTE, true);
    flashMap.setTargetRequestPath(target.getRawPath());
    flashMapManager.saveOutputFlashMap(flashMap, request, response);

    response.sendRedirect(target.toString());
  }

  /** The referring page as a path and query, if it is a page of this application. */
  private static Optional<URI> refererOnThisSite(HttpServletRequest request) {
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
      return Optional.of(
          URI.create(uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery()));
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }
}
