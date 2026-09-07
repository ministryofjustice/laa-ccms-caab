package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
@DisplayName("CCMS OIDC user test")
class CcmsOidcUserTest {

  @Mock private OidcUser delegate;

  private CcmsOidcUser ccmsOidcUser(final GrantedAuthority... authorities) {
    return new CcmsOidcUser(delegate, "CCMSUSER", Set.of(authorities));
  }

  @Test
  @DisplayName("Is named by the CCMS username rather than the EntraID identity")
  void isNamedByCcmsUsername() {
    assertEquals("CCMSUSER", ccmsOidcUser().getName());
  }

  @Test
  @DisplayName("Holds the authorities it was given")
  void holdsAuthorities() {
    final GrantedAuthority authority = new SimpleGrantedAuthority("VCA");

    assertEquals(Set.of(authority), Set.copyOf(ccmsOidcUser(authority).getAuthorities()));
  }

  @Test
  @DisplayName("Does not allow its authorities to be modified")
  void authoritiesAreUnmodifiable() {
    final CcmsOidcUser user = ccmsOidcUser(new SimpleGrantedAuthority("VCA"));

    assertThrows(
        UnsupportedOperationException.class,
        () -> ((Set<GrantedAuthority>) user.getAuthorities()).clear());
  }

  @Test
  @DisplayName("Leaves the EntraID claims, attributes and tokens untouched")
  void delegatesEverythingElse() {
    final Map<String, Object> claims = Map.of("email", "a.user@justice.gov.uk");
    final OidcIdToken idToken = new OidcIdToken("token", null, null, Map.of("sub", "a-subject"));
    final OidcUserInfo userInfo = new OidcUserInfo(claims);

    when(delegate.getClaims()).thenReturn(claims);
    when(delegate.getAttributes()).thenReturn(claims);
    when(delegate.getIdToken()).thenReturn(idToken);
    when(delegate.getUserInfo()).thenReturn(userInfo);

    final CcmsOidcUser user = ccmsOidcUser();

    assertSame(claims, user.getClaims());
    assertSame(claims, user.getAttributes());
    assertSame(idToken, user.getIdToken());
    assertSame(userInfo, user.getUserInfo());
  }

  @Test
  @DisplayName("Reads standard claims through the claims it delegates")
  void readsStandardClaims() {
    when(delegate.getClaims()).thenReturn(Map.of("email", "a.user@justice.gov.uk"));

    assertEquals("a.user@justice.gov.uk", ccmsOidcUser().getEmail());
  }

  @Test
  @DisplayName("Accepts an empty set of authorities")
  void acceptsNoAuthorities() {
    assertEquals(List.of(), List.copyOf(ccmsOidcUser().getAuthorities()));
  }
}
