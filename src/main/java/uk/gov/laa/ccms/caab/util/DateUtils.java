package uk.gov.laa.ccms.caab.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;

/** DateUtils. */
public final class DateUtils {

  // Date pattern used for component-specific date formatting
  public static final String COMPONENT_DATE_PATTERN = "dd/MM/yyyy";
  private static final DateTimeFormatter COMPONENT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern(COMPONENT_DATE_PATTERN);
  private static final DateTimeFormatter STRICT_COMPONENT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter STRICT_FLEXIBLE_COMPONENT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

  private DateUtils() {}

  /**
   * Converts a date string in either "dd/MM/yyyy" or "d/M/yyyy" format to a {@link Date}.
   *
   * @param date the input date string
   * @return the corresponding {@link Date} object
   */
  public static Date convertToDate(String date) {
    LocalDate localDate = convertToLocalDate(date);
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Converts a date string in either "dd/MM/yyyy" or "d/M/yyyy" format to an ISO 8601 date string
   * ("yyyy-MM-dd").
   *
   * @param date the input date string
   * @return the corresponding date string in ISO 8601 format
   */
  public static String convertToDateString(String date) {
    return convertToLocalDate(date).toString();
  }

  /**
   * Converts a {@link Date} object to a string in "dd/MM/yyyy" format.
   *
   * @param date the {@link Date} object to convert
   * @return the corresponding date string in "dd/MM/yyyy" format
   */
  public static String convertToComponentDate(Date date) {
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    return localDate.format(COMPONENT_DATE_FORMATTER);
  }

  /**
   * Converts a date string in either "dd/MM/yyyy" or "d/M/yyyy" format to a {@link LocalDate}.
   *
   * @param date the input date string
   * @return the corresponding {@link LocalDate}
   */
  public static LocalDate convertToLocalDate(String date) {
    try {
      return LocalDate.parse(date, STRICT_COMPONENT_DATE_FORMATTER);
    } catch (DateTimeParseException ex) {
      return LocalDate.parse(date, STRICT_FLEXIBLE_COMPONENT_DATE_FORMATTER);
    }
  }

  /**
   * Normalises a component date to "dd/MM/yyyy".
   *
   * @param date the input date string
   * @return the normalised date string in "dd/MM/yyyy" format
   */
  public static String normaliseComponentDate(String date) {
    return convertToLocalDate(date).format(COMPONENT_DATE_FORMATTER);
  }

  /**
   * Attempts to normalise a component date to "dd/MM/yyyy", preserving the original value when the
   * input is blank or invalid so calling validators can surface field errors.
   *
   * @param date the input date string
   * @return normalised date when valid, otherwise the original value
   */
  public static String normaliseComponentDateIfValid(String date) {
    if (date == null || date.isBlank()) {
      return date;
    }

    try {
      return normaliseComponentDate(date);
    } catch (DateTimeParseException ex) {
      return date;
    }
  }

  /**
   * Only keeps digits and forward slashes, as the accepted date format is "dd/MM/yyyy".
   *
   * @param date the input date string
   * @return sanitised date string, or null/blank if input is null/blank
   */
  public static String sanitiseDateInput(String date) {
    if (date == null || date.isBlank()) {
      return date;
    }
    return date.replaceAll("[^0-9/]", "");
  }

  /**
   * Checks if a date string contains invalid characters. A date string is considered invalid if it
   * contains characters other than digits and forward slashes (the accepted format is
   * "dd/MM/yyyy").
   *
   * @param date the input date string to check
   * @return true if the date contains invalid characters, false otherwise
   */
  public static boolean hasInvalidDateCharacters(String date) {
    if (date == null || date.isBlank()) {
      return false;
    }
    String sanitised = sanitiseDateInput(date);
    return !date.equals(sanitised);
  }
}
