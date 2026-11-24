package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

class ProviderContextFilterTest {

  private final ProviderContextFilter filter = new ProviderContextFilter();

  @AfterEach
  void tearDown() {
    ProviderContext.clear();
  }

  @Test
  void unauthenticatedRequestDoesNotCreateSessionAndCleansUpOnFailure() {
    final MockHttpServletRequest request = new MockHttpServletRequest();

    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                  assertNull(ProviderContext.getProviderId());
                  assertNull(request.getSession(false));
                  ProviderContext.setProviderId("55555");
                  throw new ServletException("synthetic failure");
                }));

    assertNull(ProviderContext.getProviderId());
    assertNull(request.getSession(false));
  }

  @Test
  void seedsProviderContextFromSessionUserAndRestoresOuterContext() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        SessionConstants.USER_DETAILS, new UserDetail().provider(new BaseProvider().id(12345)));
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);

    ProviderContext.setProviderId("99999");
    final String[] seen = new String[1];

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> seen[0] = ProviderContext.getProviderId());

    assertEquals("12345", seen[0]);
    assertEquals("99999", ProviderContext.getProviderId());
  }

  @Test
  void clearsPreExistingContextWhenSessionHasNoUserDetail() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionConstants.USER_DETAILS, "not-a-user-detail");
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);

    ProviderContext.setProviderId("99999");
    final String[] seen = new String[1];

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> seen[0] = ProviderContext.getProviderId());

    assertNull(seen[0]);
    assertEquals("99999", ProviderContext.getProviderId());
  }

  @Test
  void filtersAsyncAndErrorDispatchesAndCleansEachScope() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        SessionConstants.USER_DETAILS, new UserDetail().provider(new BaseProvider().id(24680)));
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    final MockHttpServletResponse response = new MockHttpServletResponse();

    final String[] seen = new String[2];
    request.setDispatcherType(DispatcherType.ASYNC);
    filter.doFilter(request, response, (req, res) -> seen[0] = ProviderContext.getProviderId());
    assertNull(ProviderContext.getProviderId());

    request.setDispatcherType(DispatcherType.ERROR);
    filter.doFilter(request, response, (req, res) -> seen[1] = ProviderContext.getProviderId());
    assertNull(ProviderContext.getProviderId());

    assertEquals("24680", seen[0]);
    assertEquals("24680", seen[1]);
  }

  @Test
  void invalidatedSessionDoesNotAbortRequestOrCreateReplacementSession() throws Exception {
    final HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(anyString())).thenThrow(new IllegalStateException("invalidated"));

    final MockHttpServletRequest request = spy(new MockHttpServletRequest());
    request.setSession(session);
    final String[] seen = new String[1];

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> seen[0] = ProviderContext.getProviderId());

    assertNull(seen[0]);
    assertNull(ProviderContext.getProviderId());
    verify(request, never()).getSession();
    verify(request, never()).getSession(true);
  }
}
