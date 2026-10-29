package uk.gov.laa.ccms.caab.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards markup mistakes that an accessibility scan reports as violations: a summary list that is
 * not a definition list, a list holding something other than list items, and a label that points at
 * the wrong input. None of them shows on screen, so none of them is caught by looking.
 */
class TemplateAccessibilityGuardTest {

  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  private static final Pattern CLASS_ATTRIBUTE_TAG =
      Pattern.compile("<([a-zA-Z][\\w:-]*)\\b[^>]*?\\sclass\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL);

  private static final Pattern TAG =
      Pattern.compile("<(/?)([a-zA-Z][\\w:-]*)\\b[^>]*?(/?)>", Pattern.DOTALL);

  private static final Set<String> VOID_ELEMENTS =
      Set.of(
          "br", "hr", "img", "input", "meta", "link", "wbr", "col", "area", "base", "source",
          "track", "embed");

  /** What a list may directly contain; {@code th:block} leaves nothing behind when rendered. */
  private static final Set<String> LIST_CHILDREN = Set.of("li", "th:block", "script", "template");

  @Test
  @DisplayName("A summary list is a definition list, so its keys and values have a list to sit in")
  void summaryListsAreDefinitionLists() throws IOException {
    List<String> violations = new ArrayList<>();
    forEachTemplate(
        (name, content) -> {
          Matcher tag = CLASS_ATTRIBUTE_TAG.matcher(content);
          while (tag.find()) {
            boolean isSummaryList =
                List.of(tag.group(2).trim().split("\\s+")).contains("govuk-summary-list");
            if (isSummaryList && !tag.group(1).equals("dl")) {
              violations.add(
                  "%s line %d: <%s class=\"govuk-summary-list\"> should be <dl>"
                      .formatted(name, lineOf(content, tag.start()), tag.group(1)));
            }
          }
        });

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName(
      "Every summary list row sits inside its definition list, which closes after the last")
  void summaryListRowsSitInsideTheirList() throws IOException {
    List<String> violations = new ArrayList<>();
    forEachTemplate(
        (name, content) -> {
          Matcher tag = TAG.matcher(content);
          Deque<String> open = new ArrayDeque<>();
          while (tag.find()) {
            String element = tag.group(2).toLowerCase();
            boolean closing = !tag.group(1).isEmpty();
            boolean selfClosing = !tag.group(3).isEmpty() || VOID_ELEMENTS.contains(element);
            if (closing) {
              if (element.equals("dl") && !"dl".equals(open.peek())) {
                violations.add(
                    "%s line %d: </dl> while <%s> is still open"
                        .formatted(name, lineOf(content, tag.start()), open.peek()));
              }
              while (!open.isEmpty() && !open.peek().equals(element)) {
                open.pop();
              }
              if (!open.isEmpty()) {
                open.pop();
              }
              continue;
            }
            // A row defined as a fragment has no list around it here: the page that uses it
            // supplies one.
            boolean isFragment = tag.group(0).contains("th:fragment");
            if (!isFragment && hasClass(tag.group(0), "govuk-summary-list__row")) {
              String parent =
                  open.stream().filter(e -> !e.equals("th:block")).findFirst().orElse("nothing");
              if (!parent.equals("dl")) {
                violations.add(
                    "%s line %d: a summary list row is inside <%s>, not <dl>"
                        .formatted(name, lineOf(content, tag.start()), parent));
              }
            }
            if (!selfClosing) {
              open.push(element);
            }
          }
        });

    assertThat(violations).isEmpty();
  }

  private static boolean hasClass(String tag, String className) {
    Matcher classes = Pattern.compile("\\sclass\\s*=\\s*\"([^\"]*)\"").matcher(tag);
    return classes.find() && List.of(classes.group(1).trim().split("\\s+")).contains(className);
  }

  @Test
  @DisplayName("A ul or ol holds only list items, or blocks that render to nothing")
  void listsHoldOnlyListItems() throws IOException {
    List<String> violations = new ArrayList<>();
    forEachTemplate(
        (name, content) -> {
          Matcher tag = TAG.matcher(content);
          Deque<String> open = new ArrayDeque<>();
          while (tag.find()) {
            String element = tag.group(2).toLowerCase();
            boolean closing = !tag.group(1).isEmpty();
            boolean selfClosing = !tag.group(3).isEmpty() || VOID_ELEMENTS.contains(element);
            if (closing) {
              while (!open.isEmpty() && !open.peek().equals(element)) {
                open.pop();
              }
              if (!open.isEmpty()) {
                open.pop();
              }
              continue;
            }
            String parent = open.peek();
            if (("ul".equals(parent) || "ol".equals(parent)) && !LIST_CHILDREN.contains(element)) {
              violations.add(
                  "%s line %d: <%s> directly inside <%s>"
                      .formatted(name, lineOf(content, tag.start()), element, parent));
            }
            if (!selfClosing) {
              open.push(element);
            }
          }
        });

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("Each address result's label is tied to its own radio button")
  void addressResultLabelsPointAtTheirRadios() throws IOException {
    String template = read("application/sections/address-search-results.html");

    Matcher input =
        Pattern.compile("<input[^>]*name=\"uprn\"[^>]*th:id=\"([^\"]*)\"", Pattern.DOTALL)
            .matcher(template);
    Matcher label =
        Pattern.compile("<label[^>]*th:for=\"([^\"]*)\"", Pattern.DOTALL).matcher(template);

    assertThat(input.find()).as("the result radio has a th:id").isTrue();
    assertThat(label.find()).as("the result label has a th:for").isTrue();
    assertThat(label.group(1)).isEqualTo(input.group(1));
  }

  @Test
  @DisplayName("The claim file upload's label is tied to the file input")
  void claimFileUploadLabelPointsAtTheInput() throws IOException {
    String template = read("requests/provider-request-detail.html");

    assertThat(template).contains("<input class=\"govuk-file-upload\" id=\"file\"");
    assertThat(template).containsPattern("<label[^>]*\\sfor=\"file\"");
    assertThat(template).doesNotContain("th:for=\"fileUpload\"");
  }

  private static String read(String relativePath) throws IOException {
    return Files.readString(TEMPLATES.resolve(relativePath), StandardCharsets.UTF_8);
  }

  private static int lineOf(String content, int offset) {
    return (int) content.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
  }

  private interface TemplateVisitor {
    void visit(String relativePath, String content) throws IOException;
  }

  private static void forEachTemplate(TemplateVisitor visitor) throws IOException {
    try (Stream<Path> files = Files.walk(TEMPLATES)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".html")).sorted().toList()) {
        visitor.visit(
            TEMPLATES.relativize(file).toString(), Files.readString(file, StandardCharsets.UTF_8));
      }
    }
  }
}
