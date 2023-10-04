package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor class responsible for logging incoming requests and redirects after POST requests.
 * This interceptor logs any request that's being handled by a method within a @Controller
 * or @RestController. For POST requests, it additionally logs any redirect URL after the controller
 * processing.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

  @Setter
  private Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

  /**
   * Logs the request method and URI before the controller method is invoked.
   *
   * @param request The incoming HTTP request.
   * @param response The outgoing HTTP response.
   * @param handler The object that is handling the current request.
   *                This could be a controller or any other type of handler.
   * @return true to indicate processing should continue, false to indicate processing of
   *         the current request should stop.
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      Class<?> beanType = handlerMethod.getBeanType();

      // Check if it's a controller or rest controller
      if (beanType.isAnnotationPresent(Controller.class)) {
        log.info("[{}] {}",
            request.getMethod(),
            request.getRequestURI());
      }
    }

    return true;
  }

  /**
   * Logs any redirect URL after the controller has processed a POST request.
   *
   * @param request The incoming HTTP request.
   * @param response The outgoing HTTP response.
   * @param handler The object that is handling the current request.
   * @param modelAndView The model and view returned by the handler method.
   */
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    if (handler instanceof HandlerMethod handlerMethod) {
      Class<?> beanType = handlerMethod.getBeanType();

      // Check if it's a controller or rest controller
      if ((beanType.isAnnotationPresent(Controller.class))
          && modelAndView != null && modelAndView.getViewName() != null) {

        printAllSessionAttributeNames(request.getSession());

        log.debug("[DISPLAY] {}",
            modelAndView.getViewName());
      }
    }
  }


  /**
   * Prints the names of all attributes stored in the provided HttpSession.
   *
   * @param session The HttpSession object from which attributes will be fetched and logged.
   */
  private void printAllSessionAttributeNames(HttpSession session) {
    Enumeration<String> attributeNames = session.getAttributeNames();

    StringBuilder sessionLogs = new StringBuilder("[SESSION KEYS] - [");
    while (attributeNames.hasMoreElements()) {
      String attributeName = attributeNames.nextElement();
      sessionLogs.append(String.format("%s,", attributeName));
    }
    sessionLogs.append("]");
    log.debug("{}", sessionLogs);
  }
}