package uk.gov.laa.ccms.caab.util;

import java.util.List;

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
}
