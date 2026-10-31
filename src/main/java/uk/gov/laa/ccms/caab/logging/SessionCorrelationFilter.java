package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds an opaque logging identifier without exposing authentication session IDs. */
@Component
public class SessionCorrelationFilter extends OncePerRequestFilter {

  static final String REQUEST_ID = "request_id";
  static final String REQUEST_ID_HEADER = "X-Request-Id";
  static final String SESSION_ID = "sessionID";
  static final String SOURCE_IP = "source.ip";
  static final String FORWARDED_IP = "network.forwarded_ip";

  private static final String FORWARDED_FOR = "X-Forwarded-For";

  private static final String ATTRIBUTE = SessionCorrelationFilter.class.getName() + ".sessionID";
  private static final String REQUEST_ATTRIBUTE =
      SessionCorrelationFilter.class.getName() + ".requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Map<String, String> previous = new HashMap<>();
    try {
      String requestId = (String) request.getAttribute(REQUEST_ATTRIBUTE);
      if (requestId == null) {
        requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
      }
      response.setHeader(REQUEST_ID_HEADER, requestId);
      String id = (String) request.getAttribute(ATTRIBUTE);
      if (id == null) {
        id = correlationId(request);
        request.setAttribute(ATTRIBUTE, id);
      }
      put(previous, REQUEST_ID, requestId);
      put(previous, SESSION_ID, id);
      put(previous, SOURCE_IP, request.getRemoteAddr());
      put(previous, FORWARDED_IP, SourceAddress.forwardedIp(request.getHeader(FORWARDED_FOR)));
      chain.doFilter(request, response);
    } finally {
      previous.forEach(
          (key, value) -> {
            if (value == null) {
              MDC.remove(key);
            } else {
              MDC.put(key, value);
            }
          });
    }
  }

  private static void put(Map<String, String> previous, String key, String value) {
    if (!previous.containsKey(key)) {
      previous.put(key, MDC.get(key));
    }
    if (value != null) {
      MDC.put(key, value);
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
