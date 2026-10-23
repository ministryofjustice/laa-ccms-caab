package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends a session a fixed time after the user signed in, however active they have been.
 *
 * <p>{@code server.servlet.session.timeout} only measures idle time, so a user who keeps clicking
 * is never signed out.
 */
@Slf4j
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

  /** Session attribute holding when the user authenticated. */
  static final String AUTHENTICATED_AT = "authenticatedAt";

  private final Duration maximumSessionAge;
  private final String expiredDestination;

  /**
   * Constructs the filter.
   *
   * @param maximumSessionAge how long a session may live once the user has signed in.
   * @param expiredDestination where to send the user once their session has expired.
   */
  public AbsoluteSessionTimeoutFilter(
      final Duration maximumSessionAge, final String expiredDestination) {
    this.maximumSessionAge = maximumSessionAge;
    this.expiredDestination = expiredDestination;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    final HttpSession session = request.getSession(false);

    if (session == null || !isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    final Object authenticatedAt = session.getAttribute(AUTHENTICATED_AT);

    if (authenticatedAt == null) {
      session.setAttribute(AUTHENTICATED_AT, Instant.now());
      filterChain.doFilter(request, response);
      return;
    }

    if (hasExpired((Instant) authenticatedAt)) {
      log.info("Session reached its maximum age of {}, signing the user out", maximumSessionAge);
      session.invalidate();
      SecurityContextHolder.clearContext();
      response.sendRedirect(request.getContextPath() + expiredDestination);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean hasExpired(final Instant authenticatedAt) {
    return Duration.between(authenticatedAt, Instant.now()).compareTo(maximumSessionAge) > 0;
  }

  private boolean isAuthenticated() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
