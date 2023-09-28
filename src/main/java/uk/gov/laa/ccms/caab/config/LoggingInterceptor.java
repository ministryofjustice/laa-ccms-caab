package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor class responsible for logging incoming requests and redirects after POST requests.
 * This interceptor logs any request that's being handled by a method within a @Controller
 * or @RestController. For POST requests, it additionally logs any redirect URL after the controller
 * processing.
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

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
      if (beanType.isAnnotationPresent(Controller.class)
          || beanType.isAnnotationPresent(RestController.class)) {
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
    if (handler instanceof HandlerMethod handlerMethod
        && request.getMethod().equalsIgnoreCase("POST")) {
      Class<?> beanType = handlerMethod.getBeanType();

      // Check if it's a controller or rest controller
      if ((beanType.isAnnotationPresent(Controller.class)
          || beanType.isAnnotationPresent(RestController.class))
          && modelAndView != null && modelAndView.getViewName() != null) {

        String viewName = modelAndView.getViewName();
        if (viewName.startsWith("redirect:")) {
          log.info("[{}] {} - Redirecting to: [GET] {}",
              request.getMethod(),
              request.getRequestURI(),
              viewName.substring("redirect:".length()));
        }
      }
    }
  }
}