package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Seeds {@link ProviderContext} from an existing HTTP session without creating sessions for
 * unauthenticated requests, and guarantees thread cleanup when the dispatch completes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class ProviderContextFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    final String previous = ProviderContext.getProviderId();
    try {
      ProviderContext.setFromUser(resolveSessionUser(request));
      filterChain.doFilter(request, response);
    } finally {
      if (previous == null) {
        ProviderContext.clear();
      } else {
        ProviderContext.setProviderId(previous);
      }
    }
  }

  private static UserDetail resolveSessionUser(final HttpServletRequest request) {
    try {
      final HttpSession session = request.getSession(false);
      if (session != null
          && session.getAttribute(SessionConstants.USER_DETAILS) instanceof UserDetail user) {
        return user;
      }
    } catch (final IllegalStateException invalidated) {
      return null;
    }
    return null;
  }
}
