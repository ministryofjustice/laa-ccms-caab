package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class SessionStateLockFilterTest {

  private final SessionStateLockFilter filter = new SessionStateLockFilter();

  @Test
  void statelessRequestsDoNotCreateASession() throws Exception {
    var request = new MockHttpServletRequest();
    filter.doFilter(request, new MockHttpServletResponse(), (req, response) -> {});
    assertNull(request.getSession(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/assets/test.css",
        "/ccms/ccms.css",
        "/govuk-dialect/accessible-autocomplete.css",
        "/favicon.ico"
      })
  void staticAssetsDoNotWaitForTheSessionPermit(String path) throws Exception {
    var session = new MockHttpSession();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var assetServed = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var statefulRequest =
          executor.submit(
              () -> {
                filter.doFilter(
                    request(session),
                    new MockHttpServletResponse(),
                    (req, response) -> {
                      entered.countDown();
                      await(release);
                    });
                return null;
              });
      assertTrue(entered.await(5, TimeUnit.SECONDS));

      var assetRequest = new MockHttpServletRequest("GET", path);
      assetRequest.setServletPath(path);
      assetRequest.setSession(session);
      var asset =
          executor.submit(
              () -> {
                filter.doFilter(
                    assetRequest,
                    new MockHttpServletResponse(),
                    (req, response) -> assetServed.countDown());
                return null;
              });
      try {
        assertTrue(assetServed.await(1, TimeUnit.SECONDS));
      } finally {
        release.countDown();
      }

      statefulRequest.get(5, TimeUnit.SECONDS);
      asset.get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void failingRequestsReleaseTheirPermit() throws Exception {
    var session = new MockHttpSession();
    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request(session),
                new MockHttpServletResponse(),
                (req, response) -> {
                  throw new ServletException("failed");
                }));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var next =
          executor.submit(
              () -> {
                filter.doFilter(
                    request(session),
                    new MockHttpServletResponse(),
                    (req, response) -> session.setAttribute("next", "completed"));
                return session.getAttribute("next");
              });
      assertEquals("completed", next.get(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void resetsWaitForWriteBackButOtherSessionsAreIndependent() throws Exception {
    var firstSession = new MockHttpSession();
    var secondSession = new MockHttpSession();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var resetStarted = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(3)) {
      var writeBack =
          executor.submit(
              () -> {
                filter.doFilter(
                    request(firstSession),
                    new MockHttpServletResponse(),
                    (req, response) -> {
                      entered.countDown();
                      await(release);
                      // Simulates MVC write-back after the handler returns, still inside the filter
                      // chain.
                      firstSession.setAttribute("registration", "old");
                    });
                return null;
              });
      try {
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var reset =
            executor.submit(
                () -> {
                  resetStarted.countDown();
                  filter.doFilter(
                      request(firstSession),
                      new MockHttpServletResponse(),
                      (req, response) -> firstSession.setAttribute("registration", "new"));
                  return null;
                });
        assertTrue(resetStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> reset.get(100, TimeUnit.MILLISECONDS));
        var independent =
            executor.submit(
                () -> {
                  filter.doFilter(
                      request(secondSession),
                      new MockHttpServletResponse(),
                      (req, response) -> secondSession.setAttribute("registration", "independent"));
                  return secondSession.getAttribute("registration");
                });
        assertEquals("independent", independent.get(5, TimeUnit.SECONDS));
        release.countDown();
        writeBack.get(5, TimeUnit.SECONDS);
        reset.get(5, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
    }
    assertEquals("new", firstSession.getAttribute("registration"));
  }

  @Test
  void asynchronousRequestsHoldTheirPermitUntilCompletion() throws Exception {
    var session = new MockHttpSession();
    var asyncRequest = request(session);
    asyncRequest.setAsyncSupported(true);
    var response = new MockHttpServletResponse();
    filter.doFilter(asyncRequest, response, (req, res) -> asyncRequest.startAsync(req, res));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var next =
          executor.submit(
              () -> {
                filter.doFilter(
                    request(session),
                    new MockHttpServletResponse(),
                    (req, res) -> session.setAttribute("next", "completed"));
                return session.getAttribute("next");
              });
      try {
        assertThrows(TimeoutException.class, () -> next.get(100, TimeUnit.MILLISECONDS));
      } finally {
        asyncRequest.getAsyncContext().complete();
      }
      assertEquals("completed", next.get(5, TimeUnit.SECONDS));
    }
  }

  private MockHttpServletRequest request(MockHttpSession session) {
    var request = new MockHttpServletRequest();
    request.setSession(session);
    return request;
  }

  private static void await(CountDownLatch latch) throws ServletException {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new ServletException("Test request timed out");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new ServletException(interrupted);
    }
  }
}
