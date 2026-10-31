package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class UserContextFilterTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void setsPseudonymisedUserForRequestAndRemovesIt() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "jane.doe@example.com", "n/a", AuthorityUtils.NO_AUTHORITIES));
    String[] seen = new String[1];

    new UserContextFilter()
        .doFilter(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            (req, res) -> seen[0] = MDC.get("user_id"));

    assertEquals(UserIdentifier.pseudonymise("jane.doe@example.com"), seen[0]);
    assertNotEquals("jane.doe@example.com", seen[0]);
    assertNull(MDC.get("user_id"));
  }

  @Test
  void leavesUserUnsetForAnonymous() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    String[] seen = {"unset"};

    new UserContextFilter()
        .doFilter(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            (req, res) -> seen[0] = MDC.get("user_id"));

    assertNull(seen[0]);
  }

  @Test
  void pseudonymiseIsStableCaseInsensitiveAndHandlesBlank() {
    assertEquals(UserIdentifier.pseudonymise("A@b.com"), UserIdentifier.pseudonymise(" a@b.com "));
    assertEquals("unknown", UserIdentifier.pseudonymise(null));
  }
}
