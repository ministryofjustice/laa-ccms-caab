package uk.gov.laa.ccms.caab.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Utility class which provides methods for handling user roles. */
public final class UserRoleUtil {

  /**
   * Finds the roles in {@code roleNames} that have not been granted as an authority.
   *
   * @param authorities the authorities granted to the logged-in user.
   * @param roleNames comma-separated {@link UserRole} names, e.g. {@code "VIEW_PROCEEDING"}.
   * @return the roles which have not been granted.
   */
  public static List<UserRole> findMissingRoles(
      Collection<? extends GrantedAuthority> authorities, String roleNames) {
    List<String> granted = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    return Arrays.stream(roleNames.split(","))
        .map(String::trim)
        .map(UserRole::valueOf)
        .filter(role -> !granted.contains(role.getCode()))
        .toList();
  }

  /**
   * Check whether the logged-in user has been granted the provided role.
   *
   * @param user the logged-in user.
   * @param role the role to check.
   * @return true if the user has access to the role, false otherwise.
   */
  public static boolean hasRole(UserDetail user, UserRole role) {
    return user.getFunctions().contains(role.getCode());
  }
}
