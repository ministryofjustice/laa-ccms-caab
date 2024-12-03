package uk.gov.laa.ccms.caab.util;

import java.util.List;
import uk.gov.laa.ccms.caab.config.UserRole;

/**
 * Utility class which provides methods for handling lists in view templates.
 */
public class ViewListUtil {

  /**
   * Determines whether at least one of the values in two lists is common between them.
   *
   * @param list1 the first list.
   * @param list2 the second list.
   * @return true if a common value was found, otherwise false.
   */
  public static boolean containsAny(List<Object> list1, List<Object> list2) {
    return list1.stream().anyMatch(list2::contains);
  }

  /**
   * Finds any values in the second list that are missing in the first list.
   *
   * @param list1 the first list.
   * @param list2 the second list.
   * @return the list of missing values.
   */
  public static List<UserRole> findMissingRoles(List<String> list1, List<String> list2) {
    return list2.stream()
        .filter(element -> !list1.contains(element))
        .map(UserRole::findByCode)
        .toList();
  }
}
