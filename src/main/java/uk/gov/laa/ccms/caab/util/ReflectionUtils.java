package uk.gov.laa.ccms.caab.util;

import java.lang.reflect.Field;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * A utility class providing functionality related to Java reflection.
 * <p>
 * This class is used to manipulate object properties using reflection.
 * </p>
 */
public class ReflectionUtils {

  /**
   * Iterates over all string fields of a given object. If any string field has a value
   * that is either empty or consists only of whitespace, it sets that field to null.
   * <p>
   * For instance, if an object has a string field 'name' with a value of "  ", this
   * method will set 'name' to null.
   * </p>
   *
   * @param o the object whose string fields are to be checked and potentially set to null
   * @throws RuntimeException if any reflection operation fails
   */
  public static void nullifyStrings(Object o) {
    for (Field f : o.getClass().getDeclaredFields()) {
      f.setAccessible(true);
      try {
        if (f.getType().equals(String.class)) {
          String value = (String) f.get(o);
          if (value != null && value.trim().isEmpty()) {
            f.set(o, null);
          }
        }
      } catch (IllegalAccessException e) {
        throw new CaabApplicationException("Failed to nullify field: " + f.getName(), e);
      }
    }
  }
}
