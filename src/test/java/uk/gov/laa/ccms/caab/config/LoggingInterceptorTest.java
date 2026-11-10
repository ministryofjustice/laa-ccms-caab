package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

public class LoggingInterceptorTest {

  private Logger loggerMock;
  private final uk.gov.laa.ccms.caab.security.SecurityEventLogger securityEventLogger =
      mock(uk.gov.laa.ccms.caab.security.SecurityEventLogger.class);
  private LoggingInterceptor loggingInterceptor;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpSession session;

  @BeforeEach
  public void setUp() {
    loggerMock = mock(Logger.class); // Initialize the loggerMock
    loggingInterceptor = new LoggingInterceptor(securityEventLogger);
    loggingInterceptor.setLog(loggerMock);

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    session = mock(HttpSession.class);
  }

  @Test
  public void afterCompletionReportsDataAccessWithIdentifiersOnly() throws Exception {
    var req = requestWithActiveCase("GET", "/case/overview");

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verify(securityEventLogger).dataAccessed("VIEW", true, "300001", "CL1", "/case/overview");
  }

  @Test
  public void aPostIsRecordedAsAChange() throws Exception {
    var req = requestWithActiveCase("POST", "/case/overview");

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verify(securityEventLogger).dataAccessed("CHANGE", true, "300001", "CL1", "/case/overview");
  }

  @Test
  public void aGetHandlerMarkedAsChangingDataIsRecordedAsAChange() throws Exception {
    var req = requestWithActiveCase("GET", "/case/evidence/1/remove");

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testRemove"),
        null);

    verify(securityEventLogger)
        .dataAccessed("CHANGE", true, "300001", "CL1", "/case/evidence/1/remove");
  }

  @Test
  public void aFailedRequestIsRecordedAsAFailure() throws Exception {
    var req = requestWithActiveCase("GET", "/case/overview");

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        new IllegalStateException("boom"));

    verify(securityEventLogger).dataAccessed("VIEW", false, "300001", "CL1", "/case/overview");
  }

  @Test
  public void afterCompletionReportsNothingWithoutACaseOrClient() throws Exception {
    var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/home");
    req.setSession(new org.springframework.mock.web.MockHttpSession());

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verifyNoInteractions(securityEventLogger);
  }

  @Test
  public void theDataAccessEventNamesTheRouteNotTheIdentifiersInThePath() throws Exception {
    var req = requestWithActiveCase("GET", "/case/300001/overview");
    req.setAttribute(
        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
        "/case/{reference}/overview");

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verify(securityEventLogger)
        .dataAccessed("VIEW", true, "300001", "CL1", "/case/{reference}/overview");
  }

  @Test
  public void aFailureThatAnExceptionHandlerResolvedIsRecordedAsAFailure() throws Exception {
    var req = requestWithActiveCase("GET", "/case/overview");
    req.setAttribute(
        org.springframework.web.servlet.DispatcherServlet.EXCEPTION_ATTRIBUTE,
        new IllegalStateException("handled elsewhere"));

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verify(securityEventLogger).dataAccessed("VIEW", false, "300001", "CL1", "/case/overview");
  }

  private static org.springframework.mock.web.MockHttpServletRequest requestWithActiveCase(
      String method, String uri) {
    var mockSession = new org.springframework.mock.web.MockHttpSession();
    mockSession.setAttribute(
        "activeCase",
        uk.gov.laa.ccms.caab.bean.ActiveCase.builder()
            .caseReferenceNumber("300001")
            .clientReferenceNumber("CL1")
            .client("Jane Doe")
            .build());
    var req = new org.springframework.mock.web.MockHttpServletRequest(method, uri);
    req.setSession(mockSession);
    req.setAttribute(
        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, uri);
    return req;
  }

  @Test
  public void testPreHandleWithControllerAnnotation() throws Exception {
    // Create a mock Controller instance
    MyController myController = new MyController();
    // Create a HandlerMethod with the MyController instance as the beanType
    HandlerMethod handlerMethod = new HandlerMethod(myController, "testGet");

    boolean result = loggingInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(request, times(1)).getMethod();
    verify(request, times(1)).getRequestURI();
    verify(loggerMock).info(eq("[{}] {}"), any(), any());
  }

  @Test
  public void testPreHandleWithoutControllerOrRestControllerAnnotation() throws Exception {
    // Create a mock Controller instance
    NoControllerClass noControllerClass = new NoControllerClass();
    // Create a HandlerMethod with the MyController instance as the beanType
    HandlerMethod handlerMethod = new HandlerMethod(noControllerClass, "test");

    boolean result = loggingInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result); // Continue processing
    verifyNoInteractions(request);
    verifyNoInteractions(loggerMock);
  }

  @Test
  public void testPostHandleWithSessionAttributes() throws NoSuchMethodException {
    ModelAndView modelAndView = new ModelAndView("redirect:/some-url");

    // Create a mock Controller instance
    MyController myController = new MyController();
    // Create a HandlerMethod with the MyController instance as the beanType
    HandlerMethod handlerMethod = new HandlerMethod(myController, "testPost");

    // Stub the behavior for the request to return our session mock
    when(request.getSession()).thenReturn(session);
    when(request.getSession(anyBoolean())).thenReturn(session);

    // Prepare mock attribute names and stub the getSession method to return them
    List<String> attributeNamesList = Arrays.asList("attribute1", "attribute2", "attribute3");
    when(session.getAttributeNames()).thenReturn(Collections.enumeration(attributeNamesList));

    loggingInterceptor.postHandle(request, response, handlerMethod, modelAndView);

    InOrder inOrder = inOrder(loggerMock);
    // verify that the first debug call was made with those specific arguments
    inOrder.verify(loggerMock).debug(eq("{}"), Optional.ofNullable(any(StringBuilder.class)));

    // verify that the second debug call was made with those specific arguments
    inOrder.verify(loggerMock).debug(eq("[DISPLAY] {}"), eq(modelAndView.getViewName()));
  }

  // Example controller and rest controller classes for testing
  @Controller
  private static class MyController {
    @GetMapping("/get")
    public String testGet() {
      // Add some logic to the method if needed
      return "test"; // Return the name of the view if it's relevant
    }

    @GetMapping("/remove")
    @uk.gov.laa.ccms.caab.logging.ChangesData
    public String testRemove() {
      return "redirect:/get";
    }

    @GetMapping("/post")
    public String testPost() {
      // Add some logic to the method if needed
      return "redirect:/get"; // Return the name of the view if it's relevant
    }
  }

  private static class NoControllerClass {
    public String test() {
      // Add some logic to the method if needed
      return "test"; // Return the name of the view if it's relevant
    }
  }

  @Test
  public void aSessionInvalidatedByALogoutDoesNotBreakTheAfterCompletion() throws Exception {
    var invalidated = mock(HttpSession.class);
    when(invalidated.getAttribute(org.mockito.ArgumentMatchers.anyString()))
        .thenThrow(new IllegalStateException("session invalidated"));
    var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/case/overview");
    req.setSession(invalidated);

    loggingInterceptor.afterCompletion(
        req,
        new org.springframework.mock.web.MockHttpServletResponse(),
        new HandlerMethod(new MyController(), "testGet"),
        null);

    verifyNoInteractions(securityEventLogger);
  }
}
