package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Identity provider logout handler test")
class IdentityProviderLogoutHandlerTest {

  private static final String ENTRA =
      "https://login.microsoftonline.com/a-tenant/oauth2/v2.0/logout"
          + "?post_logout_redirect_uri=https://host/civil";
  private static final String MOCK = "http://localhost:9000/connect/logout";

  @Mock private SecurityEventLogger securityEventLogger;

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private static Authentication signedIn() {
    OidcIdToken idToken =
        new OidcIdToken(
            "an id token/with+characters",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of(IdTokenClaimNames.SUB, "a-subject"));
    return new TestingAuthenticationToken(new DefaultOidcUser(null, idToken), "n/a");
  }

  @Test
  @DisplayName("Carries the ID token so the provider knows whose session to end")
  void sendsTheIdTokenHint() {
    new IdentityProviderLogoutHandler(MOCK, securityEventLogger)
        .logout(request, response, signedIn());

    assertThat(response.getRedirectedUrl())
        .isEqualTo(MOCK + "?id_token_hint=an+id+token%2Fwith%2Bcharacters");
    verify(securityEventLogger).logout(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Appends to a URL that already carries the return destination")
  void appendsToAnExistingQuery() {
    new IdentityProviderLogoutHandler(ENTRA, securityEventLogger)
        .logout(request, response, signedIn());

    assertThat(response.getRedirectedUrl())
        .startsWith(ENTRA + "&id_token_hint=")
        .contains("post_logout_redirect_uri=https://host/civil");
  }

  @Test
  @DisplayName("Still signs out when there is no ID token to carry")
  void withoutAnIdToken() {
    new IdentityProviderLogoutHandler(ENTRA, securityEventLogger)
        .logout(request, response, new TestingAuthenticationToken("a-user", "n/a"));

    assertThat(response.getRedirectedUrl()).isEqualTo(ENTRA);
  }
}
