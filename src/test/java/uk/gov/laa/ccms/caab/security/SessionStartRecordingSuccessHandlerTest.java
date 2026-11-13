package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("Session start recording success handler test")
class SessionStartRecordingSuccessHandlerTest {

  @Mock private AuthenticationSuccessHandler delegate;

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final Authentication authentication =
      new TestingAuthenticationToken("A-CCMS-USER", "n/a");

  @Test
  @DisplayName("Stamps the session at sign-in, so the absolute timeout runs from then")
  void stampsTheSessionAtSignIn() throws Exception {
    Instant before = Instant.now();

    new SessionStartRecordingSuccessHandler(delegate)
        .onAuthenticationSuccess(request, response, authentication);

    Object stamped =
        request.getSession().getAttribute(AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT);
    assertThat(stamped)
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.INSTANT)
        .isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("Still completes the sign-in through the handler it wraps")
  void delegatesTheRedirect() throws Exception {
    new SessionStartRecordingSuccessHandler(delegate)
        .onAuthenticationSuccess(request, response, authentication);

    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  @DisplayName("A session is ended a fixed time after sign-in, not after its first page")
  void theFilterMeasuresFromTheStampTheHandlerWrote() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("A-CCMS-USER", "n/a", "VC"));
    try {
      // Sign in, then let the session age past the maximum before the first request reaches the
      // filter. Before the handler existed the filter stamped it on that request, so the clock
      // started there and the session survived.
      new SessionStartRecordingSuccessHandler(delegate)
          .onAuthenticationSuccess(request, response, authentication);
      HttpSession session = request.getSession();
      session.setAttribute(
          AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT, Instant.now().minus(Duration.ofHours(13)));

      MockHttpServletResponse later = new MockHttpServletResponse();
      new AbsoluteSessionTimeoutFilter(Duration.ofHours(12), "/home")
          .doFilter(request, later, new MockFilterChain());

      assertThat(later.getRedirectedUrl()).isEqualTo("/home");
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
