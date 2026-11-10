package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Notes on a request that an exception was thrown while handling it. Spring clears its own record
 * once an error view is rendered, so interceptors that run afterwards cannot otherwise tell.
 */
public final class HandledException {

  private static final String ATTRIBUTE = HandledException.class.getName();

  private HandledException() {}

  /**
   * Notes the exception on the request.
   *
   * @param request the request being handled
   * @param exception the exception thrown while handling it
   */
  public static void mark(HttpServletRequest request, Exception exception) {
    request.setAttribute(ATTRIBUTE, exception);
  }

  /**
   * Whether an exception was noted on the request.
   *
   * @param request the request
   * @return true if {@link #mark} was called for it
   */
  public static boolean isMarked(HttpServletRequest request) {
    return request.getAttribute(ATTRIBUTE) != null;
  }
}
