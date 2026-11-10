package uk.gov.laa.ccms.caab.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Set<String> SAFE_KEYS =
      Set.of("status", "error", "code", "field", "fieldName", "objectName", "type", "title");
  private static final int MAX_SUMMARY_ENTRIES = 20;
  private static final String OMITTED = "[body omitted]";

  private LogMasker() {}

  /**
   * Summarises an upstream JSON error body as its status, error, code and field names only.
   * Messages and rejected values are left out because they can echo personal data. A body that is
   * not JSON, or has none of those keys, is reported as omitted.
   *
   * @param body the response body, which may be null
   * @return the summary, an omission marker, or an empty string for a missing body
   */
  public static String errorSummary(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    List<String> entries = new ArrayList<>();
    try {
      collectSafeEntries(MAPPER.readTree(body), entries);
    } catch (RuntimeException e) {
      return OMITTED;
    }
    return entries.isEmpty() ? OMITTED : String.join(", ", entries);
  }

  private static void collectSafeEntries(JsonNode node, List<String> entries) {
    if (entries.size() >= MAX_SUMMARY_ENTRIES) {
      return;
    }
    if (node.isObject()) {
      node.properties()
          .forEach(
              property -> {
                if (SAFE_KEYS.contains(property.getKey()) && property.getValue().isValueNode()) {
                  entries.add(property.getKey() + "=" + mask(property.getValue().asString()));
                } else {
                  collectSafeEntries(property.getValue(), entries);
                }
              });
    } else if (node.isArray()) {
      node.forEach(child -> collectSafeEntries(child, entries));
    }
  }

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
