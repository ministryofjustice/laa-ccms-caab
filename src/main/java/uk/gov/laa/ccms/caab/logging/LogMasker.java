package uk.gov.laa.ccms.caab.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Keeps personal data in upstream error bodies and URLs out of the logs. */
public final class LogMasker {

  private static final Pattern URL_QUERY_AND_FRAGMENT = Pattern.compile("[?#].*", Pattern.DOTALL);

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Set<String> IDENTIFIER_KEYS =
      Set.of("code", "field", "fieldName", "objectName");
  private static final Set<String> CONTAINER_KEYS = Set.of("errors", "fieldErrors", "violations");
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_.\\[\\]-]{0,59}");
  private static final Pattern STATUS = Pattern.compile("\\d{3}");
  private static final int MAX_SUMMARY_ENTRIES = 20;
  private static final String OMITTED = "[body omitted]";

  private LogMasker() {}

  /**
   * Summarises an upstream JSON error body as its numeric status plus the code and field name of
   * each entry in its error list. Only single-token values are kept, and rejected values and
   * messages are never read, because they can echo personal data. A body that is not JSON, or has
   * nothing that qualifies, is reported as omitted.
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
      addIfMatches(node, "status", STATUS, entries);
      IDENTIFIER_KEYS.forEach(key -> addIfMatches(node, key, IDENTIFIER, entries));
      CONTAINER_KEYS.forEach(
          key -> {
            JsonNode child = node.get(key);
            if (child != null) {
              collectSafeEntries(child, entries);
            }
          });
    } else if (node.isArray()) {
      node.forEach(child -> collectSafeEntries(child, entries));
    }
  }

  private static void addIfMatches(
      JsonNode object, String key, Pattern allowed, List<String> entries) {
    JsonNode value = object.get(key);
    if (value != null && value.isValueNode() && allowed.matcher(value.asString()).matches()) {
      entries.add(key + "=" + value.asString());
    }
  }

  /**
   * Removes the query string and fragment from a URL, which can carry identifiers or tokens.
   *
   * @param url the URL, which may be null
   * @return the URL without its query string or fragment, or an empty string for null
   */
  public static String withoutQuery(String url) {
    return url == null ? "" : URL_QUERY_AND_FRAGMENT.matcher(url).replaceAll("");
  }
}
