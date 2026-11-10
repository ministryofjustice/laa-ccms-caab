package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import uk.gov.laa.ccms.caab.logging.RequestRoute;

/** Logs requests refused for a bad CSRF token, then responds exactly as Spring's default does. */
@RequiredArgsConstructor
public class CsrfLoggingAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityEventLogger securityEventLogger;

  private final AccessDeniedHandler delegate = new AccessDeniedHandlerImpl();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, jakarta.servlet.ServletException {
    if (accessDeniedException instanceof CsrfException) {
      securityEventLogger.csrfRejected(
          request.getMethod(), RequestRoute.of(request), accessDeniedException);
    }
    delegate.handle(request, response, accessDeniedException);
  }
}
