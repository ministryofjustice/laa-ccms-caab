package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("Absolute session timeout filter test")
class AbsoluteSessionTimeoutFilterTest {

  private static final Duration MAX_AGE = Duration.ofHours(12);

  private final AbsoluteSessionTimeoutFilter filter =
      new AbsoluteSessionTimeoutFilter(MAX_AGE, "/home");

  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private void signedIn() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("CCMSUSER", "n/a", "VCA"));
  }

  private MockHttpServletRequest requestWithSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/civil");
    request.getSession(true);
    return request;
  }

  @Test
  @DisplayName("Records when the user signed in")
  void stampsTheAuthenticationTime() throws Exception {
    signedIn();
    MockHttpServletRequest request = requestWithSession();

    filter.doFilter(request, response, chain);

    HttpSession session = request.getSession(false);
    assertNotNull(session.getAttribute(AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT));
    assertNotNull(chain.getRequest(), "the request should have continued");
  }

  @Test
  @DisplayName("Lets a session through while it is within its maximum age")
  void allowsSessionWithinMaximumAge() throws Exception {
    signedIn();
    MockHttpServletRequest request = requestWithSession();
    request
        .getSession()
        .setAttribute(
            AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT,
            Instant.now().minus(Duration.ofHours(11)));

    filter.doFilter(request, response, chain);

    assertNotNull(chain.getRequest(), "the request should have continued");
    assertEquals(200, response.getStatus());
  }

  @Test
  @DisplayName("Signs the user out once the session is older than its maximum age")
  void endsSessionBeyondMaximumAge() throws Exception {
    signedIn();
    MockHttpServletRequest request = requestWithSession();
    HttpSession session = request.getSession();
    session.setAttribute(
        AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT,
        Instant.now().minus(Duration.ofHours(12)).minusSeconds(1));

    filter.doFilter(request, response, chain);

    assertNull(chain.getRequest(), "the request must not have continued");
    assertEquals("/civil/home", response.getRedirectedUrl());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(((org.springframework.mock.web.MockHttpSession) session).isInvalid());
  }

  @Test
  @DisplayName("Ignores a request with no session")
  void ignoresRequestWithoutSession() throws Exception {
    signedIn();
    MockHttpServletRequest request = new MockHttpServletRequest();

    filter.doFilter(request, response, chain);

    assertNotNull(chain.getRequest(), "the request should have continued");
  }

  @Test
  @DisplayName("Ignores an anonymous user, who has not signed in")
  void ignoresAnonymousUser() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    MockHttpServletRequest request = requestWithSession();

    filter.doFilter(request, response, chain);

    assertNull(
        request.getSession(false).getAttribute(AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT));
    assertNotNull(chain.getRequest(), "the request should have continued");
  }
}
