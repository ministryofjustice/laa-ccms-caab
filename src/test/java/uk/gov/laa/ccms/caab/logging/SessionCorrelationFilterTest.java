package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
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
}
