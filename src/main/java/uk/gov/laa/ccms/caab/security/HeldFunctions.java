package uk.gov.laa.ccms.caab.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.laa.ccms.caab.config.UserRole;

/** The user functions held by the logged-in user, as reloaded on Home. */
public final class HeldFunctions {

  private HeldFunctions() {}

  public static boolean holdsAll(UserRole... roles) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    Set<String> held =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    return Arrays.stream(roles).map(UserRole::getCode).allMatch(held::contains);
  }
}
