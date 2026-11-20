package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Session state used to return a user to a page after a refused action: the pages recently shown by
 * a plain page load, and whether the not-authorised message is waiting to be shown.
 */
public final class RefusedActionSession {

  private static final String RENDERED_PAGES = RefusedActionSession.class.getName() + ".PAGES";
  private static final String NOT_AUTHORISED = RefusedActionSession.class.getName() + ".PENDING";
  private static final int PAGE_LIMIT = 50;
  private static final String FETCH_MODE_HEADER = "Sec-Fetch-Mode";

  private RefusedActionSession() {}

  /**
   * Records a page shown by a plain page load, so it can be safely loaded again.
   *
   * @param session the user's session.
   * @param pathAndQuery the page's request path, including the context path, and query.
   */
  public static void recordRenderedPage(HttpSession session, String pathAndQuery) {
    List<String> pages = new ArrayList<>(renderedPages(session));
    pages.remove(pathAndQuery);
    pages.add(pathAndQuery);
    if (pages.size() > PAGE_LIMIT) {
      pages.removeFirst();
    }
    session.setAttribute(RENDERED_PAGES, new ArrayList<>(pages));
  }

  /**
   * The recorded page matching a referring page: the same page and query if recorded, otherwise the
   * most recent page at the same path.
   */
  static Optional<String> findRenderedPage(HttpSession session, String pathAndQuery) {
    List<String> pages = renderedPages(session);
    int exact = pages.indexOf(pathAndQuery);
    if (exact >= 0) {
      return Optional.of(pages.get(exact));
    }
    String path = pathOf(pathAndQuery);
    return pages.reversed().stream().filter(page -> pathOf(page).equals(path)).findFirst();
  }

  /**
   * Whether a page is a path within this application, so redirecting to it cannot leave the site. A
   * leading {@code //} or a backslash would let a browser read it as another host.
   */
  static boolean isLocalPage(String page, String contextPath) {
    return page.startsWith(contextPath + "/") && !page.startsWith("//") && !page.contains("\\");
  }

  /** The page most recently shown by a plain page load. */
  static Optional<String> lastRenderedPage(HttpSession session) {
    List<String> pages = renderedPages(session);
    return pages.isEmpty() ? Optional.empty() : Optional.of(pages.getLast());
  }

  static void markNotAuthorised(HttpSession session) {
    session.setAttribute(NOT_AUTHORISED, Boolean.TRUE);
  }

  /** Whether the not-authorised message is waiting, clearing it so it is shown only once. */
  public static boolean consumeNotAuthorised(HttpSession session) {
    boolean pending = Boolean.TRUE.equals(session.getAttribute(NOT_AUTHORISED));
    if (pending) {
      session.removeAttribute(NOT_AUTHORISED);
    }
    return pending;
  }

  /**
   * Whether the browser is loading a whole page, rather than making a background request. Browsers
   * that do not send {@code Sec-Fetch-Mode} are judged by whether they accept HTML.
   */
  static boolean isPageNavigation(HttpServletRequest request) {
    String fetchMode = request.getHeader(FETCH_MODE_HEADER);
    if (fetchMode != null) {
      return "navigate".equals(fetchMode);
    }
    String accept = request.getHeader("Accept");
    return accept == null || accept.contains("text/html") || accept.contains("*/*");
  }

  static String pathOf(String pathAndQuery) {
    int queryStart = pathAndQuery.indexOf('?');
    return queryStart < 0 ? pathAndQuery : pathAndQuery.substring(0, queryStart);
  }

  @SuppressWarnings("unchecked")
  private static List<String> renderedPages(HttpSession session) {
    Object pages = session.getAttribute(RENDERED_PAGES);
    return pages instanceof List<?> list ? (List<String>) list : List.of();
  }
}
