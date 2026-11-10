package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.web.servlet.HandlerMapping;

/** Names the route a request took for the logs, without the identifiers in its path. */
public final class RequestRoute {

  private static final Pattern PATH_PARAMETERS = Pattern.compile(";[^/]*");
  private static final Pattern IDENTIFYING_SEGMENT = Pattern.compile("[^/]*[\\d@%][^/]*");
  private static final int MAX_LENGTH = 200;

  private RequestRoute() {}

  /**
   * Returns the matched route template, such as {@code /case/{id}/overview}. A request that no
   * handler has matched yet falls back to its path with path parameters removed and any segment
   * holding a digit, an at sign or an escape replaced by an asterisk.
   *
   * @param request the request
   * @return the route or redacted path
   */
  public static String of(HttpServletRequest request) {
    if (request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        instanceof String route) {
      return route;
    }
    String path = PATH_PARAMETERS.matcher(request.getRequestURI()).replaceAll("");
    String redacted = IDENTIFYING_SEGMENT.matcher(path).replaceAll("*");
    return redacted.length() > MAX_LENGTH ? redacted.substring(0, MAX_LENGTH) : redacted;
  }
}
