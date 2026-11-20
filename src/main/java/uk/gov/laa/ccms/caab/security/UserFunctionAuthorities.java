package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Replaces the user's functions in the logged-in authentication after the user is reloaded, as the
 * legacy PUI reloads them on entering the home page and when switching provider.
 */
public final class UserFunctionAuthorities {

  private UserFunctionAuthorities() {}

  /**
   * Swaps the functions in the authentication for the reloaded user's, keeping authorities that are
   * not functions, such as the OIDC groups. Only an OIDC login is updated.
   *
   * @param authentication the logged-in authentication.
   * @param previousUser the user as loaded before.
   * @param reloadedUser the user as just reloaded.
   * @param session the session the security context is saved in.
   */
  public static void replace(
      Authentication authentication,
      UserDetail previousUser,
      UserDetail reloadedUser,
      HttpSession session) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)
        || !(oauth2Authentication.getPrincipal() instanceof OidcUser oidcUser)) {
      return;
    }
    // The authentication and the previous user are loaded separately, so the authentication may
    // hold a function the previous user no longer has. Every known function is removed as well.
    Set<String> functionCodes = new HashSet<>(functionsOf(previousUser));
    Arrays.stream(UserRole.values()).map(UserRole::getCode).forEach(functionCodes::add);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    oauth2Authentication.getAuthorities().stream()
        .filter(authority -> !functionCodes.contains(authority.getAuthority()))
        .forEach(authorities::add);
    functionsOf(reloadedUser).stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new OAuth2AuthenticationToken(
            new CcmsOidcUser(oidcUser, authentication.getName(), authorities),
            authorities,
            oauth2Authentication.getAuthorizedClientRegistrationId()));
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
  }

  private static List<String> functionsOf(UserDetail user) {
    return Optional.ofNullable(user.getFunctions()).orElse(List.of());
  }
}
