package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.web.servlet.HandlerMapping;

/** Names the route a request took for the logs, without the identifiers in its path. */
public final class RequestRoute {

  private static final Pattern PATH_PARAMETERS = Pattern.compile(";[^/]*");
  private static final String UNMATCHED_ROUTE = "[unmatched route]";
  private static final int MAX_LENGTH = 200;

  private static volatile Set<String> knownSegments = Set.of();

  private RequestRoute() {}

  /**
   * Adds literal route segments, such as {@code case} or {@code overview}, that may be logged.
   *
   * @param segments the segments to allow
   */
  public static synchronized void registerKnownSegments(Set<String> segments) {
    Set<String> merged = new HashSet<>(knownSegments);
    merged.addAll(segments);
    knownSegments = Set.copyOf(merged);
  }

  /** Forgets the registered segments. Intended for tests. */
  public static synchronized void clearKnownSegments() {
    knownSegments = Set.of();
  }

  /**
   * Returns the matched route template, such as {@code /case/{id}/overview}. A request that no
   * handler has matched yet, such as one refused by a security filter, falls back to its path with
   * path parameters removed and every segment that is not a literal part of a route replaced by an
   * asterisk. Until the routes are known, only a fixed marker is returned.
   *
   * @param request the request
   * @return the route or redacted path
   */
  public static String of(HttpServletRequest request) {
    if (request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        instanceof String route) {
      return route;
    }
    String uri = PATH_PARAMETERS.matcher(request.getRequestURI()).replaceAll("");
    Set<String> known = knownSegments;
    if (known.isEmpty()) {
      return UNMATCHED_ROUTE;
    }
    String redacted = keepKnownSegments(uri, request.getContextPath(), known);
    return redacted.length() > MAX_LENGTH ? redacted.substring(0, MAX_LENGTH) : redacted;
  }

  private static String keepKnownSegments(String uri, String contextPath, Set<String> known) {
    String context = uri.startsWith(contextPath) ? contextPath : "";
    String path = uri.substring(context.length());
    return context
        + Arrays.stream(path.split("/", -1))
            .map(segment -> segment.isEmpty() || known.contains(segment) ? segment : "*")
            .collect(Collectors.joining("/"));
  }
}
