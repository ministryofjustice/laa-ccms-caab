package uk.gov.laa.ccms.caab.logging;

import java.util.regex.Pattern;

/**
 * Masks personal data patterns in free text, such as upstream error bodies, before it is logged.
 */
public final class LogMasker {

  static final int MAX_LENGTH = 500;

  private static final String MASK = "***";

  private static final Pattern NATIONAL_INSURANCE =
      Pattern.compile("\\b[A-Za-z]{2}\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-Za-z]\\b");
  private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
  private static final Pattern DATE =
      Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})\\b");
  private static final Pattern LONG_DIGITS = Pattern.compile("\\b\\d[\\d\\s-]{8,}\\d\\b");
  private static final Pattern URL_QUERY = Pattern.compile("\\?[^\\s\"'<>]*");

  private LogMasker() {}

  /**
   * Masks National Insurance numbers, email addresses, dates and long digit runs, and truncates.
   *
   * @param text the text to mask, which may be null
   * @return the masked, length-limited text, or an empty string for null
   */
  public static String mask(String text) {
    if (text == null) {
      return "";
    }
    String masked = EMAIL.matcher(text).replaceAll(MASK);
    masked = NATIONAL_INSURANCE.matcher(masked).replaceAll(MASK);
    masked = DATE.matcher(masked).replaceAll(MASK);
    masked = LONG_DIGITS.matcher(masked).replaceAll(MASK);
    return masked.length() > MAX_LENGTH
        ? masked.substring(0, MAX_LENGTH) + "...[truncated]"
        : masked;
  }

  /**
   * Removes the query string from a URL, which can carry identifiers or tokens.
   *
   * @param url the URL, which may be null
   * @return the URL without its query string, or an empty string for null
   */
  public static String withoutQuery(String url) {
    return url == null ? "" : URL_QUERY.matcher(url).replaceAll("");
  }
}
