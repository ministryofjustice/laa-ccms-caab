package uk.gov.laa.ccms.caab.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

/**
 * Utility class for handling display related operations.
 */
public class DisplayUtil {

  /**
   * Convenience method to handle concatenating a list of Strings into a comma-separated string,
   * with an alternative delimiter of ' or ' before the final entry.
   *
   * @param items - the list of strings.
   * @return Delimited String.
   */
  public static String getCommaDelimitedString(final List<String> items) {
    return getDelimitedString(items, ", ", " or ");
  }

  /**
   * Method to handle concatenating a list of Strings into a delimited string,
   * with an alternative delimiter before the final entry.
   *
   * @param items - the list of strings.
   * @param delimiter - the delimiter for all but the last entry.
   * @param lastDelimiter - the final delimiter.
   * @return Delimited String.
   */
  public static String getDelimitedString(
      final List<String> items,
      final String delimiter,
      final String lastDelimiter) {
    return items.stream().collect(Collectors.collectingAndThen(Collectors.toList(),
        strings -> {
          int last = strings.size() - 1;
          if (last < 1) {
            return String.join(delimiter, strings);
          }

          return String.join(
              lastDelimiter,
              String.join(delimiter, strings.subList(0, last)),
              strings.get(last));
        }));
  }

  /**
   * Format a name for display.
   *
   * @param firstName - the first name.
   * @param lastName - the last name.
   * @return formatted name.
   */
  public static String getFullName(final String firstName, final String lastName) {
    return "%s%s".formatted(
        StringUtils.hasText(firstName) ? firstName + " " : "",
        StringUtils.hasText(lastName) ? lastName : "").trim();
  }

  /**
   * Get the display value from a StringDisplayValue with null checks, returning a blank
   * string if the supplied StringDisplayValue is null.
   *
   * @param stringDisplayValue - the string display value
   * @return the display value, or an empty string.
   */
  public static String getDisplayValue(final StringDisplayValue stringDisplayValue) {
    return getDisplayValue(stringDisplayValue, "");
  }

  /**
   * Get the display value from a StringDisplayValue with null checks.
   *
   * @param stringDisplayValue - the string display value.
   * @param defaultDisplayValue - the value to return if the supplied StringDisplayValue is null.
   * @return the display value, or the supplied default if stringDisplayValue is null.
   */
  public static String getDisplayValue(final StringDisplayValue stringDisplayValue,
      final String defaultDisplayValue) {
    return Optional.ofNullable(stringDisplayValue)
        .map(StringDisplayValue::getDisplayValue)
        .orElse(defaultDisplayValue);
  }

  /**
   * Get the display value from an IntDisplayValue with null checks, returning a blank
   * string if the supplied IntDisplayValue is null.
   *
   * @param intDisplayValue - the int display value
   * @return the display value, or an empty string.
   */
  public static String getDisplayValue(final IntDisplayValue intDisplayValue) {
    return getDisplayValue(intDisplayValue, "");
  }

  /**
   * Get the display value from a IntDisplayValue with null checks.
   *
   * @param intDisplayValue - the int display value.
   * @param defaultDisplayValue - the value to return if the supplied IntDisplayValue is null.
   * @return the display value, or the supplied default if intDisplayValue is null.
   */
  public static String getDisplayValue(final IntDisplayValue intDisplayValue,
      final String defaultDisplayValue) {
    return Optional.ofNullable(intDisplayValue)
        .map(IntDisplayValue::getDisplayValue)
        .orElse(defaultDisplayValue);
  }

}
