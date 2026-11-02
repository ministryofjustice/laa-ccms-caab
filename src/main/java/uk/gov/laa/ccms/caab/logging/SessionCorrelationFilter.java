package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds an opaque logging identifier without exposing authentication session IDs. */
@Component
public class SessionCorrelationFilter extends OncePerRequestFilter {

  private static final String ATTRIBUTE = SessionCorrelationFilter.class.getName() + ".sessionID";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String previous = MDC.get("sessionID");
    try {
      String id = (String) request.getAttribute(ATTRIBUTE);
      if (id == null) {
        id = correlationId(request);
        request.setAttribute(ATTRIBUTE, id);
      }
      MDC.put("sessionID", id);
      chain.doFilter(request, response);
    } finally {
      if (previous == null) {
        MDC.remove("sessionID");
      } else {
        MDC.put("sessionID", previous);
      }
    }
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  private String correlationId(HttpServletRequest request) {
    // Stateless API calls must not create an HTTP session just for logging.
    HttpSession session = request.getSession(false);
    if (session == null) {
      return UUID.randomUUID().toString();
    }
    String requestId = UUID.randomUUID().toString();
    try {
      synchronized (session) {
        String id = (String) session.getAttribute(ATTRIBUTE);
        if (id == null) {
          id = requestId;
          session.setAttribute(ATTRIBUTE, id);
        }
        return id;
      }
    } catch (IllegalStateException invalidated) {
      // Logout can invalidate a container session independently of this monitor.
      // Logging must not abort the request or create a replacement authentication session.
      return requestId;
    }
  }
}
