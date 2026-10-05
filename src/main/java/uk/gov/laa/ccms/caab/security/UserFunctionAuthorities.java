package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Replaces the user's functions in the logged-in authentication after the user is reloaded, as the
 * legacy PUI reloads them on entering the home page and when switching provider.
 */
public final class UserFunctionAuthorities {

  private UserFunctionAuthorities() {}

  /**
   * Swaps the previous user's functions for the reloaded user's in the authentication, keeping
   * authorities that are not functions, such as the SAML groups. Only a SAML login is updated.
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
    if (!(authentication instanceof Saml2AssertionAuthentication saml2Authentication)) {
      return;
    }
    List<String> previousFunctions = functionsOf(previousUser);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    saml2Authentication.getAuthorities().stream()
        .filter(authority -> !previousFunctions.contains(authority.getAuthority()))
        .forEach(authorities::add);
    functionsOf(reloadedUser).stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new Saml2AssertionAuthentication(
            saml2Authentication.getPrincipal(),
            saml2Authentication.getCredentials(),
            authorities,
            saml2Authentication.getRelyingPartyRegistrationId()));
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
  }

  private static List<String> functionsOf(UserDetail user) {
    return Optional.ofNullable(user.getFunctions()).orElse(List.of());
  }
}
