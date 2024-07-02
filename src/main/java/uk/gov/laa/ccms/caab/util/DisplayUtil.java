package uk.gov.laa.ccms.caab.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

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
    return String.format("%s%s",
            StringUtils.hasText(firstName) ? firstName + " " : "",
            StringUtils.hasText(lastName) ? lastName : "").trim();
  }

}
