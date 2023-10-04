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
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
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
  private LoggingInterceptor loggingInterceptor;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpSession session;

  @BeforeEach
  public void setUp() {
    loggerMock = mock(Logger.class); // Initialize the loggerMock
    loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLog(loggerMock);

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    session = mock(HttpSession.class);
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
}
