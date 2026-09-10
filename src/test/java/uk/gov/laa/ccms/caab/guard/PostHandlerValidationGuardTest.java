package uk.gov.laa.ccms.caab.guard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fails the build when a POST handler binds user input without validating it.
 *
 * <p>The runtime baseline in {@code GlobalBinderAdvice} only reaches handlers whose model attribute
 * carries {@code @Validated}, because that is how Spring gates binder-registered validators. This
 * guard is the other half: it stops the set of handlers outside that net from growing, so the gap
 * can only close.
 */
class PostHandlerValidationGuardTest {

  private static final Path CONTROLLERS = Path.of("src/main/java/uk/gov/laa/ccms/caab/controller");

  private static final Pattern MODEL_ATTRIBUTE =
      Pattern.compile(
          // Tolerates a fully-qualified parameter type, which would otherwise slip past the guard.
          "@ModelAttribute\\s*(?:\\([^)]*\\))?\\s*(?:final\\s+)?(?:[a-z_][\\w.]*\\.)?([A-Z]\\w+)");

  /**
   * Handlers that bind a bean but genuinely have no user-entered text to check - they carry a
   * session bean forward across a confirmation step. Reviewed as part of the input validation
   * spike. Entries should only ever be removed.
   */
  private static final Set<String> ACCEPTED_WITHOUT_VALIDATION =
      Set.of(
          "ClientSearchResultsController#/application/client/results",
          "ClientSummaryController#/application/client/details/summary");

  @Test
  @DisplayName("every POST handler binding a form bean validates it")
  void postHandlersBindingFormBeansAreValidated() throws IOException {
    final List<String> violations = new ArrayList<>();

    try (Stream<Path> sources = Files.walk(CONTROLLERS)) {
      for (Path source :
          sources
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith("Controller.java"))
              .toList()) {

        final String content = Files.readString(source, StandardCharsets.UTF_8);
        final String controller = source.getFileName().toString().replace(".java", "");

        // A validator held as a field is the codebase's usual way of validating through a shared
        // private helper, which a per-method scan cannot see.
        final boolean classHoldsValidator =
            Pattern.compile("private final \\w*Validator ").matcher(content).find();

        for (String handler : splitOnPostMappings(content)) {
          final int bodyStart = handler.indexOf('{');
          if (bodyStart < 0) {
            continue;
          }
          final String signature = handler.substring(0, bodyStart);
          final String body = extractBody(handler, bodyStart);

          if (!bindsFormBean(signature)) {
            continue;
          }

          final boolean validated =
              signature.contains("@Validated")
                  || signature.contains("@Valid ")
                  || body.contains(".validate(")
                  || classHoldsValidator;

          final String key = controller + "#" + firstMappingValue(handler);
          if (!validated && !ACCEPTED_WITHOUT_VALIDATION.contains(key)) {
            violations.add("  " + key);
          }
        }
      }
    }

    assertTrue(
        violations.isEmpty(),
        """
        These POST handlers bind a form bean but never validate it, so whatever is submitted is \
        taken as-is:

        %s

        Add @Validated to the @ModelAttribute parameter, or call the relevant validator. \
        @Validated also brings the field into the GlobalBinderAdvice baseline. If a handler \
        genuinely carries no user-entered text, add it to ACCEPTED_WITHOUT_VALIDATION with a \
        reason."""
            .formatted(String.join("\n", violations)));
  }

  private static List<String> splitOnPostMappings(final String content) {
    final List<String> handlers = new ArrayList<>();
    final String[] parts = content.split("@PostMapping");
    for (int i = 1; i < parts.length; i++) {
      handlers.add(parts[i]);
    }
    return handlers;
  }

  /** Brace-matches the handler body so a nested block does not end it early. */
  private static String extractBody(final String handler, final int bodyStart) {
    int depth = 0;
    for (int i = bodyStart; i < handler.length(); i++) {
      final char character = handler.charAt(i);
      if (character == '{') {
        depth++;
      } else if (character == '}') {
        depth--;
        if (depth == 0) {
          return handler.substring(bodyStart, i + 1);
        }
      }
    }
    return handler.substring(bodyStart);
  }

  private static boolean bindsFormBean(final String signature) {
    final Matcher matcher = MODEL_ATTRIBUTE.matcher(signature);
    while (matcher.find()) {
      final String type = matcher.group(1);
      if (!Set.of("String", "Integer", "Boolean", "Long").contains(type)) {
        return true;
      }
    }
    return false;
  }

  private static String firstMappingValue(final String handler) {
    final Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(handler);
    return matcher.find() ? matcher.group(1) : "?";
  }
}
