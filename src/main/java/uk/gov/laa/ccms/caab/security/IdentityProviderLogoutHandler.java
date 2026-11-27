package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * Signs the user out of the identity provider as well as this application.
 *
 * <p>The request carries {@code id_token_hint} where there is an ID token to carry. Without it a
 * provider cannot tell whose session to end or validate the return URL: EntraID falls back to an
 * account picker, and an OIDC end-session endpoint declines the redirect.
 */
public class IdentityProviderLogoutHandler implements LogoutHandler {

  private final String logoutUrl;
  private final SecurityEventLogger securityEventLogger;

  public IdentityProviderLogoutHandler(
      final String logoutUrl, final SecurityEventLogger securityEventLogger) {
    this.logoutUrl = logoutUrl;
    this.securityEventLogger = securityEventLogger;
  }

  @Override
  public void logout(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final Authentication authentication) {
    securityEventLogger.logout(authentication);
    try {
      response.sendRedirect(endSessionUrl(authentication));
    } catch (IOException e) {
      throw new RuntimeException("Failed to redirect to Identity Provider.", e);
    }
  }

  private String endSessionUrl(final Authentication authentication) {
    final String idToken = idToken(authentication);
    if (idToken == null) {
      return logoutUrl;
    }
    return logoutUrl
        + (logoutUrl.contains("?") ? "&" : "?")
        + "id_token_hint="
        + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
  }

  private static String idToken(final Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
      return null;
    }
    return oidcUser.getIdToken() == null ? null : oidcUser.getIdToken().getTokenValue();
  }
}
