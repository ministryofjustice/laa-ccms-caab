package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds the pseudonymised authenticated user to the logging context for the rest of the request. */
public class UserContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String previous = MDC.get(UserIdentifier.MDC_KEY);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      MDC.put(UserIdentifier.MDC_KEY, UserIdentifier.pseudonymise(authentication.getName()));
    }
    try {
      chain.doFilter(request, response);
    } finally {
      if (previous == null) {
        MDC.remove(UserIdentifier.MDC_KEY);
      } else {
        MDC.put(UserIdentifier.MDC_KEY, previous);
      }
    }
  }
}
