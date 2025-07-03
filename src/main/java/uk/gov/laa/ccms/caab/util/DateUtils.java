package uk.gov.laa.ccms.caab.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/** DateUtils. */
public final class DateUtils {

  // Date pattern used for component-specific date formatting
  public static final String COMPONENT_DATE_PATTERN = "d/M/yyyy";

  private DateUtils() {}

  /**
   * Converts a date string in "d/M/yyyy" format to a {@link Date}.
   *
   * @param date the input date string in "d/M/yyyy" format
   * @return the corresponding {@link Date} object
   */
  public static Date convertToDate(String date) {
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(COMPONENT_DATE_PATTERN);
    LocalDate localDate = LocalDate.parse(date, inputFormatter);
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Converts a date string in "d/M/yyyy" format to an ISO 8601 date string ("yyyy-MM-dd").
   *
   * @param date the input date string in "d/M/yyyy" format
   * @return the corresponding date string in ISO 8601 format
   */
  public static String convertToDateString(String date) {
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(COMPONENT_DATE_PATTERN);
    return LocalDate.parse(date, inputFormatter).toString();
  }

  /**
   * Converts a {@link Date} object to a string in "d/M/yyyy" format.
   *
   * @param date the {@link Date} object to convert
   * @return the corresponding date string in "d/M/yyyy" format
   */
  public static String convertToComponentDate(Date date) {
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    return localDate.format(DateTimeFormatter.ofPattern(COMPONENT_DATE_PATTERN));
  }
}
