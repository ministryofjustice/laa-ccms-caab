package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class SessionCorrelationFilterTest {
  @Test
  void statelessRequestHasCorrelationWithoutCreatingSessionAndCleansUpOnFailure() {
    var request = new MockHttpServletRequest();
    var filter = new SessionCorrelationFilter();
    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                  assertNotNull(MDC.get("sessionID"));
                  assertNull(request.getSession(false));
                  throw new ServletException("synthetic failure");
                }));
    assertNull(MDC.get("sessionID"));
  }

  @Test
  void reusesOpaqueSessionCorrelationAndRestoresPreviousContext() throws Exception {
    var session = new MockHttpSession();
    String[] ids = new String[2];
    MDC.put("sessionID", "outer-context");
    try {
      for (int i = 0; i < 2; i++) {
        int index = i;
        var request = new MockHttpServletRequest();
        request.setSession(session);
        new SessionCorrelationFilter()
            .doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> ids[index] = MDC.get("sessionID"));
        assertEquals("outer-context", MDC.get("sessionID"));
      }
      assertEquals(ids[0], ids[1]);
      assertNotEquals(session.getId(), ids[0]);
    } finally {
      MDC.remove("sessionID");
    }
  }

  @Test
  void reusesRequestCorrelationOnAsyncRedispatchAndCleansEachThreadScope() throws Exception {
    var request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var response = new MockHttpServletResponse();
    var filter = new SessionCorrelationFilter();
    String[] ids = new String[2];
    filter.doFilter(
        request,
        response,
        (req, res) -> {
          ids[0] = MDC.get("sessionID");
          request.startAsync();
        });
    assertNull(MDC.get("sessionID"));
    request.setDispatcherType(DispatcherType.ASYNC);
    filter.doFilter(request, response, (req, res) -> ids[1] = MDC.get("sessionID"));
    assertNotNull(ids[0]);
    assertEquals(ids[0], ids[1]);
    assertNull(MDC.get("sessionID"));
    assertNull(request.getSession(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void sessionInvalidatedDuringAttributeAccessDoesNotAbortRequest(boolean invalidateOnRead)
      throws Exception {
    HttpSession session = mock(HttpSession.class);
    if (invalidateOnRead) {
      when(session.getAttribute(anyString())).thenThrow(new IllegalStateException("invalidated"));
    } else {
      doThrow(new IllegalStateException("invalidated"))
          .when(session)
          .setAttribute(anyString(), any());
    }
    var request = spy(new MockHttpServletRequest());
    request.setSession(session);
    var response = new MockHttpServletResponse();
    var filter = new SessionCorrelationFilter();
    String[] ids = new String[2];
    filter.doFilter(request, response, (req, res) -> ids[0] = MDC.get("sessionID"));
    assertNotNull(ids[0]);
    assertTrue(ids[0].matches("[0-9a-f-]{36}"));
    assertNull(MDC.get("sessionID"));
    request.setDispatcherType(DispatcherType.ERROR);
    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/logging-error");
    filter.doFilter(request, response, (req, res) -> ids[1] = MDC.get("sessionID"));
    assertEquals(ids[0], ids[1]);
    assertNull(MDC.get("sessionID"));
    verify(request, never()).getSession();
    verify(request, never()).getSession(true);
    // The redispatch reuses the request attribute, even though its session is invalid.
    verify(request, times(1)).getSession(false);
  }

  @Test
  void eachRequestGetsItsOwnRequestIdEvenWithinOneSession() throws Exception {
    var session = new MockHttpSession();
    String[] ids = new String[2];
    for (int i = 0; i < 2; i++) {
      int index = i;
      var request = new MockHttpServletRequest();
      request.setSession(session);
      var response = new MockHttpServletResponse();
      new SessionCorrelationFilter()
          .doFilter(request, response, (req, res) -> ids[index] = MDC.get("request_id"));
      assertEquals(ids[index], response.getHeader("X-Request-Id"));
      assertNull(MDC.get("request_id"));
    }
    assertNotEquals(ids[0], ids[1]);
  }
}
