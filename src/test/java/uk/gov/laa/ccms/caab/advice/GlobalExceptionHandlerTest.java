package uk.gov.laa.ccms.caab.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.security.RefusedActionSession;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

  @MockitoBean private Model model;

  @MockitoBean private HttpSession session;

  @Autowired private GlobalExceptionHandler globalExceptionHandler;

  @Test
  void handleDataApiClientException() {
    final String errorMsg = "Test Exception";
    EbsApiClientException ebsApiClientException = new EbsApiClientException(errorMsg);

    globalExceptionHandler.handleException(model, session, ebsApiClientException);

    verify(model).addAttribute("error", GlobalExceptionHandler.GENERIC_ERROR_MESSAGE);
  }

  @Test
  void handleCaabApplicationException() {
    final String errorMsg = "Test Exception";
    CaabApplicationException caabApplicationException = new CaabApplicationException(errorMsg);

    globalExceptionHandler.handleException(model, session, caabApplicationException);

    verify(model).addAttribute("error", GlobalExceptionHandler.GENERIC_ERROR_MESSAGE);
  }

  @Test
  void handleServletRequestBindingException() {
    final String errorMsg = "Test Exception";
    ServletRequestBindingException servletRequestBindingException =
        new ServletRequestBindingException(errorMsg);

    globalExceptionHandler.handleException(model, session, servletRequestBindingException);

    verify(model).addAttribute("error", GlobalExceptionHandler.GENERIC_ERROR_MESSAGE);
  }

  @Test
  void accessDeniedReturnsTheUserWithTheRefusalAndLogsADenial() throws Exception {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    handler.setApplicationEventPublisher(publisher);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/application/assessments");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handleAccessDenied(request, response, new AccessDeniedException("denied"));

    assertEquals("/home", response.getRedirectedUrl());
    assertTrue(RefusedActionSession.consumeNotAuthorised(request.getSession()));
    verify(publisher).publishEvent(any(AuthorizationDeniedEvent.class));
  }
}
