package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
import uk.gov.laa.ccms.caab.logging.ChangesData;
import uk.gov.laa.ccms.caab.logging.HandledException;
import uk.gov.laa.ccms.caab.logging.RequestRoute;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;

/**
 * Interceptor class responsible for logging incoming requests and redirects after POST requests.
 * This interceptor logs any request that's being handled by a method within a @Controller
 * or @RestController. For POST requests, it additionally logs any redirect URL after the controller
 * processing.
 */
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  private static final Map<String, String> RESOURCE_VARIABLES =
      Map.of(
          "notification_id", "notification_id",
          "attachment_id", "attachment_id",
          "evidence-document-id", "document_id",
          "document-id", "document_id",
          "case-reference-number", "requested_case_reference",
          "case-reference-id", "requested_case_reference",
          "client-reference-number", "requested_client_reference");

  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9/_.-]{1,40}");

  @Setter private Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

  private final SecurityEventLogger securityEventLogger;

  /**
   * Logs the request method and URI before the controller method is invoked.
   *
   * @param request The incoming HTTP request.
   * @param response The outgoing HTTP response.
   * @param handler The object that is handling the current request. This could be a controller or
   *     any other type of handler.
   * @return true to indicate processing should continue, false to indicate processing of the
   *     current request should stop.
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod handlerMethod) {
      Class<?> beanType = handlerMethod.getBeanType();

      // Check if it's a controller or rest controller
      if (beanType.isAnnotationPresent(Controller.class)) {
        log.info("[{}] {}", request.getMethod(), request.getRequestURI());
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
          && modelAndView != null
          && modelAndView.getViewName() != null) {

        printAllSessionAttributeNames(request.getSession());

        log.debug("[DISPLAY] {}", modelAndView.getViewName());
      }
    }
  }

  /**
   * Logs which client and case a controller request touched, as identifiers only.
   *
   * @param request The incoming HTTP request.
   * @param response The outgoing HTTP response.
   * @param handler The object that handled the request.
   * @param ex Any exception thrown by the handler, or null.
   */
  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (!(handler instanceof HandlerMethod handlerMethod)
        || !handlerMethod.getBeanType().isAnnotationPresent(Controller.class)) {
      return;
    }
    String caseReference = null;
    String clientReference = null;
    try {
      HttpSession session = request.getSession(false);
      if (session != null) {
        caseReference = (String) session.getAttribute(SessionConstants.CASE_REFERENCE_NUMBER);
        clientReference = (String) session.getAttribute(SessionConstants.CLIENT_REFERENCE);
        if (session.getAttribute(SessionConstants.ACTIVE_CASE) instanceof ActiveCase activeCase) {
          caseReference = activeCase.getCaseReferenceNumber();
          clientReference = activeCase.getClientReferenceNumber();
        }
      }
    } catch (IllegalStateException invalidated) {
      // Logout can invalidate the session between looking it up and reading it.
    }
    Map<String, String> resources = pathResources(request);
    if (caseReference == null && clientReference == null && resources.isEmpty()) {
      return;
    }
    boolean succeeded =
        ex == null
            && !HandledException.isMarked(request)
            && request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE) == null
            && response.getStatus() < 400;
    boolean changes =
        !SAFE_METHODS.contains(request.getMethod())
            || handlerMethod.hasMethodAnnotation(ChangesData.class);
    securityEventLogger.dataAccessed(
        changes ? "CHANGE" : "VIEW",
        succeeded,
        caseReference,
        clientReference,
        resources,
        RequestRoute.of(request));
  }

  private static Map<String, String> pathResources(HttpServletRequest request) {
    if (!(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
        instanceof Map<?, ?> variables)) {
      return Map.of();
    }
    Map<String, String> resources = new LinkedHashMap<>();
    RESOURCE_VARIABLES.forEach(
        (variable, field) -> {
          if (variables.get(variable) instanceof String value
              && SAFE_IDENTIFIER.matcher(value).matches()) {
            resources.put(field, value);
          }
        });
    return resources;
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
      sessionLogs.append("%s,".formatted(attributeName));
    }
    sessionLogs.append("]");
    log.debug("{}", sessionLogs);
  }
}
