package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Records when the user signed in, so the absolute session timeout is measured from that moment.
 *
 * <p>The login filter answers the callback with a redirect rather than continuing the chain, so
 * {@link AbsoluteSessionTimeoutFilter} never sees the authenticating request and would otherwise
 * stamp the session on the first request after it - up to a whole idle timeout later.
 */
public class SessionStartRecordingSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthenticationSuccessHandler delegate;

  public SessionStartRecordingSuccessHandler() {
    this(new SavedRequestAwareAuthenticationSuccessHandler());
  }

  public SessionStartRecordingSuccessHandler(final AuthenticationSuccessHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onAuthenticationSuccess(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final Authentication authentication)
      throws IOException, ServletException {
    request.getSession().setAttribute(AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT, Instant.now());
    delegate.onAuthenticationSuccess(request, response, authentication);
  }
}
