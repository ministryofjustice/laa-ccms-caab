package uk.gov.laa.ccms.caab.guard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the two template-level rules that the input validation spike identified as regression
 * risks. Both are conventions no compiler enforces, so without a test they survive only as long as
 * everyone remembers them.
 */
class TemplateSecurityGuardTest {

  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  private static final Path FORMS_FRAGMENT = TEMPLATES.resolve("partials/forms.html");

  /** Matches a th:utext attribute, including one broken across lines. */
  private static final Pattern UTEXT =
      Pattern.compile("th:utext\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL);

  /** Matches an input tag, including one broken across lines. */
  private static final Pattern INPUT_TAG = Pattern.compile("<input\\b[^>]*>", Pattern.DOTALL);

  /**
   * Unescaped th:utext expressions reviewed and accepted, keyed by template then by the exact
   * expression. Keying on the expression rather than a line number means any edit to one of these
   * re-triggers the guard. Entries should only ever be removed.
   */
  private static final Map<String, List<String>> ACCEPTED_UTEXT =
      Map.of(
          "feature-unavailable.html",
              List.of("#{feature.unavailable.body.redirect(${puiHomeUrl})}"),
          "submissions/submissionInProgress.html",
              List.of("#{submission.progress.description(${submissionStatusUrl})}"),
          "application/prior-authority-remove.html",
              List.of(
                  "#{proceedings.priorAuthorities.remove.leadParagraph("
                      + "${#strings.toLowerCase(priorAuthority.type.displayValue)})}"),
          "application/billing/enter-undertaking.html",
              List.of("#{billing.enterUndertaking.terms(${statutoryChargeManualUrl})}"),
          "partials/pagination.html",
              List.of(
                  "${sortDirection == 'asc' || sortDirection == '' ? ' &#x25B2;' : ' &#x25BC;'}",
                  "${' &#x25B8;'}"));

  /**
   * Free-text inputs still declared outside {@code partials/forms.html}, with the number each
   * template is allowed. Adding one to a listed template pushes it over its budget and fails, as
   * does introducing one anywhere unlisted. Numbers should only ever go down.
   */
  private static final Map<String, Integer> LEGACY_RAW_INPUT_BUDGET =
      new LinkedHashMap<>(
          Map.of(
              "requests/provider-request-detail.html", 4,
              "application/prior-authority-details.html", 4,
              "application/case-costs.html", 1,
              // The two textareas here still need the shared largeTextInput fragment, which would
              // add a visible GDS character counter - a UX change that wants sign-off first.
              "application/record-proceeding-outcome.html", 2));

  @Test
  @DisplayName("th:utext never renders an interpolated value without escaping it")
  void utextNeverRendersUnescapedExpressions() throws IOException {
    final List<String> violations = new ArrayList<>();

    forEachTemplate(
        (relativePath, content) -> {
          final Matcher matcher = UTEXT.matcher(content);
          while (matcher.find()) {
            final String expression = normalise(matcher.group(1));
            if (!containsInterpolation(expression) || isEscaped(expression)) {
              continue;
            }
            if (ACCEPTED_UTEXT.getOrDefault(relativePath, List.of()).contains(expression)) {
              continue;
            }
            violations.add(
                """
                %s
                    th:utext="%s"
                    Wrap the value in #strings.escapeXml(...), or use th:text."""
                    .formatted(relativePath, expression));
          }
        });

    assertTrue(
        violations.isEmpty(),
        """
        th:utext renders its expression as raw markup. These interpolate a value without escaping \
        it, so anything stored in that value is rendered as HTML:

        %s

        Note that a message-bundle lookup is not automatically safe - the parameter passed into it \
        still reaches the page unescaped."""
            .formatted(String.join("\n\n", violations)));
  }

  @Test
  @DisplayName("free-text inputs are declared through the shared form fragments")
  void freeTextInputsUseTheSharedFragments() throws IOException {
    final Map<String, Integer> counts = new LinkedHashMap<>();

    forEachTemplate(
        (relativePath, content) -> {
          int raw = 0;
          final Matcher matcher = INPUT_TAG.matcher(content);
          while (matcher.find()) {
            if (matcher.group().contains("type=\"text\"")) {
              raw++;
            }
          }
          raw += countOccurrences(content, "<textarea");
          if (raw > 0) {
            counts.put(relativePath, raw);
          }
        });

    final List<String> violations = new ArrayList<>();
    counts.forEach(
        (template, actual) -> {
          final int allowed = LEGACY_RAW_INPUT_BUDGET.getOrDefault(template, 0);
          if (actual > allowed) {
            violations.add(
                "%s declares %d raw text input(s); %d allowed"
                    .formatted(template, actual, allowed));
          }
        });

    assertTrue(
        violations.isEmpty(),
        """
        Text inputs and textareas belong in partials/forms.html, so that a change to the shared \
        fragments reaches every field in the service:

        %s

        Use the textInput or largeTextInput fragment. If a genuinely new exception is unavoidable, \
        raise the budget in LEGACY_RAW_INPUT_BUDGET deliberately rather than by accident."""
            .formatted(String.join("\n", violations)));
  }

  private void forEachTemplate(final TemplateConsumer consumer) throws IOException {
    try (Stream<Path> templates = Files.walk(TEMPLATES)) {
      for (Path template :
          templates
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".html"))
              .filter(path -> !path.equals(FORMS_FRAGMENT))
              .toList()) {
        consumer.accept(
            TEMPLATES.relativize(template).toString().replace('\\', '/'),
            Files.readString(template, StandardCharsets.UTF_8));
      }
    }
  }

  private static boolean containsInterpolation(final String expression) {
    return expression.contains("${");
  }

  /** True when every interpolation in the expression is wrapped in {@code #strings.escapeXml}. */
  private static boolean isEscaped(final String expression) {
    int index = expression.indexOf("${");
    while (index >= 0) {
      if (!expression.startsWith("${#strings.escapeXml(", index)) {
        return false;
      }
      index = expression.indexOf("${", index + 2);
    }
    return true;
  }

  /** Collapses the whitespace a multi-line attribute picks up, so keys stay stable. */
  private static String normalise(final String expression) {
    return expression.replaceAll("\\s+", " ").trim();
  }

  private static int countOccurrences(final String content, final String token) {
    int count = 0;
    int index = content.indexOf(token);
    while (index >= 0) {
      count++;
      index = content.indexOf(token, index + token.length());
    }
    return count;
  }

  @FunctionalInterface
  private interface TemplateConsumer {
    void accept(String relativePath, String content) throws IOException;
  }
}
