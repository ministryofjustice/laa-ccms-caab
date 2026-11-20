package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;

/**
 * Records the pages a user is shown by a plain page load, which a refused action can return them
 * to, and shows a waiting not-authorised message on the next page that is rendered rather than on a
 * redirect.
 */
public class RefusedActionPageInterceptor implements HandlerInterceptor {

  /**
   * Pages rendered without the layout, which is where the refusal message is shown. They are
   * neither returned to after a refusal nor allowed to use up its message.
   */
  static final Set<String> VIEWS_WITHOUT_LAYOUT =
      Set.of(
          "submissions/submissionInProgress",
          "application/assessments/assessment-get",
          "application/privacy-notice-agreement-printable",
          "application/sections/application-summary-complete-printable");

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
        || response.getStatus() >= 300
        || !RefusedActionSession.isPageNavigation(request)
        || VIEWS_WITHOUT_LAYOUT.contains(modelAndView.getViewName())) {
      return;
    }

    if (RefusedActionSession.consumeNotAuthorised(session)) {
      modelAndView.addObject(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE, true);
    }

    if ("GET".equals(request.getMethod())) {
      String query = request.getQueryString();
      String page = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
      if (RefusedActionSession.isLocalPage(page, request.getContextPath())) {
        RefusedActionSession.recordRenderedPage(session, page);
      }
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
