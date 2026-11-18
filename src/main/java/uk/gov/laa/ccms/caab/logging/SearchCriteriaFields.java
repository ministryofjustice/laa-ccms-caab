package uk.gov.laa.ccms.caab.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Names the search criteria a user filled in, never their values. */
public final class SearchCriteriaFields {

  private static final Set<String> NOT_CRITERIA = Set.of("page", "size", "sort");

  private SearchCriteriaFields() {}

  /**
   * Returns the names of the populated fields of a search criteria bean, in alphabetical order.
   * Blank text, false flags, empty collections and paging fields are not counted. Only declared
   * fields are read, never getters, because a derived getter can throw or have side effects.
   *
   * @param criteria the criteria bean, which may be null
   * @return the names of the populated fields
   */
  public static List<String> populated(Object criteria) {
    return populated(criteria, Set.of());
  }

  /**
   * Returns the names of the populated fields, leaving out fields the application fills in itself.
   *
   * @param criteria the criteria bean, which may be null
   * @param notUserEntered fields to leave out because the application, not the user, sets them
   * @return the names of the populated fields
   */
  public static List<String> populated(Object criteria, Set<String> notUserEntered) {
    if (criteria == null) {
      return List.of();
    }
    Set<String> names = new TreeSet<>();
    for (Class<?> type = criteria.getClass();
        type != null && type != Object.class;
        type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (isCriteriaField(field)
            && !notUserEntered.contains(field.getName())
            && isPopulated(valueOf(field, criteria))) {
          names.add(field.getName());
        }
      }
    }
    return List.copyOf(names);
  }

  private static boolean isCriteriaField(Field field) {
    return !Modifier.isStatic(field.getModifiers())
        && !field.isSynthetic()
        && !NOT_CRITERIA.contains(field.getName());
  }

  private static Object valueOf(Field field, Object criteria) {
    try {
      field.setAccessible(true);
      return field.get(criteria);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }

  private static boolean isPopulated(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof CharSequence text) {
      return !text.toString().isBlank();
    }
    if (value instanceof Boolean flag) {
      return flag;
    }
    return !(value instanceof Collection<?> collection) || !collection.isEmpty();
  }
}
