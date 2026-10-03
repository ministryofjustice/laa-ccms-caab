package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;

/**
 * Records the pages a user is shown by a plain page load, which a refused action can return them
 * to, and shows a waiting not-authorised message on the next page that is rendered rather than on a
 * redirect.
 */
public class RefusedActionPageInterceptor implements HandlerInterceptor {

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    HttpSession session = request.getSession(false);
    if (session == null
        || modelAndView == null
        || isRedirectOrForward(modelAndView)
        || response.getStatus() >= 300) {
      return;
    }

    if (RefusedActionSession.consumeNotAuthorised(session)) {
      modelAndView.addObject(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE, true);
    }

    if ("GET".equals(request.getMethod()) && RefusedActionSession.isPageNavigation(request)) {
      String query = request.getQueryString();
      RefusedActionSession.recordRenderedPage(
          session, query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query);
    }
  }

  private static boolean isRedirectOrForward(ModelAndView modelAndView) {
    String viewName = modelAndView.getViewName();
    if (viewName != null) {
      return viewName.startsWith("redirect:") || viewName.startsWith("forward:");
    }
    return modelAndView.getView() instanceof SmartView view && view.isRedirectView();
  }
}
