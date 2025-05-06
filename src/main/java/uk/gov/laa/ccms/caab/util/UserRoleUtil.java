package uk.gov.laa.ccms.caab.util;

import java.util.List;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Utility class which provides methods for handling user roles.
 */
public final class UserRoleUtil {

  /**
   * Finds any roles in the second list that are missing in the first list.
   *
   * @param list1 the first list.
   * @param list2 the second list.
   * @return the list of missing roles.
   */
  public static List<UserRole> findMissingRoles(List<String> list1, List<String> list2) {
    return list2.stream()
        .filter(element -> !list1.contains(element))
        .map(UserRole::findByCode)
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
