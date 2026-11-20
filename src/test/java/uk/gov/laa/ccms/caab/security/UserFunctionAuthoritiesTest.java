package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("User function authorities")
class UserFunctionAuthoritiesTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static OAuth2AuthenticationToken authentication(String... authorities) {
    OidcUser user = mock(OidcUser.class);
    when(user.getName()).thenReturn("user");
    return new OAuth2AuthenticationToken(
        user, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList(), "idp");
  }

  private static List<String> currentAuthorities() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  @Test
  @DisplayName("The reloaded functions replace the previous ones, keeping the OIDC groups")
  void replacesFunctionsAndKeepsGroups() {
    MockHttpSession session = new MockHttpSession();

    UserFunctionAuthorities.replace(
        authentication("group1", "CA", "NOT"),
        new UserDetail().functions(List.of("CA", "NOT")),
        new UserDetail().functions(List.of("NOT", "VC")),
        session);

    assertThat(currentAuthorities()).containsExactlyInAnyOrder("group1", "NOT", "VC");
    OAuth2AuthenticationToken refreshed =
        (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    assertThat(refreshed.getName()).isEqualTo("user");
    assertThat(refreshed.getAuthorizedClientRegistrationId()).isEqualTo("idp");
    assertThat(refreshed.getPrincipal().getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("group1", "NOT", "VC");
    assertThat(
            (SecurityContext)
                session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .isSameAs(SecurityContextHolder.getContext());
  }

  @Test
  @DisplayName("A function revoked before the previous user was loaded is still removed")
  void removesFunctionMissingFromPreviousUser() {
    UserFunctionAuthorities.replace(
        authentication("group1", "CA", "NOT"),
        new UserDetail().functions(List.of("NOT")),
        new UserDetail().functions(List.of("NOT")),
        new MockHttpSession());

    assertThat(currentAuthorities()).containsExactlyInAnyOrder("group1", "NOT");
  }

  @Test
  @DisplayName("A login that is not OIDC is left alone")
  void ignoresOtherAuthentication() {
    UserFunctionAuthorities.replace(
        null, new UserDetail(), new UserDetail().functions(List.of("CA")), new MockHttpSession());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
