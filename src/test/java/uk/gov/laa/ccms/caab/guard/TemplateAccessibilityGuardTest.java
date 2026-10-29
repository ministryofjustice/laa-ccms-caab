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
 *
 * <p>The structural checks work on the rendered parent: a {@code th:block} leaves nothing behind
 * when the page is rendered, so its children belong to whatever contains it.
 */
class TemplateAccessibilityGuardTest {

  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  private static final Pattern TAG =
      Pattern.compile("<(/?)([a-zA-Z][\\w:-]*)\\b[^>]*?(/?)>", Pattern.DOTALL);

  private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("\\sclass\\s*=\\s*\"([^\"]*)\"");

  private static final Set<String> VOID_ELEMENTS =
      Set.of(
          "br", "hr", "img", "input", "meta", "link", "wbr", "col", "area", "base", "source",
          "track", "embed");

  /** What a list may directly contain once {@code th:block} wrappers are looked through. */
  private static final Set<String> LIST_CHILDREN = Set.of("li", "script", "template");

  private static final String TRANSPARENT = "th:block";

  @Test
  @DisplayName("Every summary list is a definition list, and its rows sit inside it")
  void summaryListsAreDefinitionListsHoldingTheirRows() throws IOException {
    List<String> violations = new ArrayList<>();
    forEachTemplate((name, content) -> violations.addAll(summaryListViolations(name, content)));

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("A ul or ol holds only list items, looking through blocks that render to nothing")
  void listsHoldOnlyListItems() throws IOException {
    List<String> violations = new ArrayList<>();
    forEachTemplate((name, content) -> violations.addAll(listViolations(name, content)));

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

  @Test
  @DisplayName("The list check rejects a div in a ul, with or without a th:block around it")
  void listCheckSeesThroughThBlocks() {
    assertThat(listViolations("t", "<ul><li>a</li><li>b</li></ul>")).isEmpty();
    assertThat(
            listViolations("t", "<ul><th:block th:each=\"x : ${xs}\"><li>a</li></th:block></ul>"))
        .isEmpty();
    assertThat(listViolations("t", "<ul><div th:if=\"${x}\"><li>a</li></div></ul>")).hasSize(1);
    assertThat(
            listViolations("t", "<ul><th:block th:each=\"x : ${xs}\"><div>a</div></th:block></ul>"))
        .hasSize(1);
    assertThat(listViolations("t", "<ol><li><ul><li>inner</li></ul></li></ol>")).isEmpty();
  }

  @Test
  @DisplayName(
      "The summary list check rejects a div, a row outside the list and a list closed early")
  void summaryListCheckFindsEachMistake() {
    String row = "<div class=\"govuk-summary-list__row\"><dt>k</dt><dd>v</dd></div>";

    assertThat(
            summaryListViolations("t", "<dl class=\"govuk-summary-list\">" + row + row + "</dl>"))
        .isEmpty();
    assertThat(
            summaryListViolations(
                "t",
                "<dl class=\"govuk-summary-list\"><th:block th:each=\"i : ${is}\">"
                    + row
                    + "</th:block></dl>"))
        .isEmpty();
    assertThat(summaryListViolations("t", "<div class=\"govuk-summary-list\">" + row + "</div>"))
        .isNotEmpty();
    assertThat(summaryListViolations("t", "<div class=\"card\">" + row + "</div>")).hasSize(1);
    String closedEarly =
        "<dl class=\"govuk-summary-list\"><div class=\"govuk-summary-list__row\"><dt>k</dt>"
            + "<dd>v</dd></dl>"
            + row
            + row
            + "</div>";
    assertThat(summaryListViolations("t", closedEarly)).hasSizeGreaterThanOrEqualTo(3);
    assertThat(
            summaryListViolations(
                "t",
                "<div class=\"govuk-summary-list__row\" th:fragment=\"row(a)\"><dt>k</dt></div>"))
        .isEmpty();
  }

  private static List<String> summaryListViolations(String name, String content) {
    List<String> violations = new ArrayList<>();
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
        popThrough(open, element);
        continue;
      }
      if (hasClass(tag.group(0), "govuk-summary-list") && !element.equals("dl")) {
        violations.add(
            "%s line %d: <%s class=\"govuk-summary-list\"> should be <dl>"
                .formatted(name, lineOf(content, tag.start()), element));
      }
      // A row defined as a fragment has no list around it here: the page that uses it supplies
      // one.
      boolean isFragment = tag.group(0).contains("th:fragment");
      if (!isFragment && hasClass(tag.group(0), "govuk-summary-list__row")) {
        String parent = renderedParent(open);
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
    return violations;
  }

  private static List<String> listViolations(String name, String content) {
    List<String> violations = new ArrayList<>();
    Matcher tag = TAG.matcher(content);
    Deque<String> open = new ArrayDeque<>();
    while (tag.find()) {
      String element = tag.group(2).toLowerCase();
      boolean closing = !tag.group(1).isEmpty();
      boolean selfClosing = !tag.group(3).isEmpty() || VOID_ELEMENTS.contains(element);
      if (closing) {
        popThrough(open, element);
        continue;
      }
      String parent = renderedParent(open);
      if (!element.equals(TRANSPARENT)
          && (parent.equals("ul") || parent.equals("ol"))
          && !LIST_CHILDREN.contains(element)) {
        violations.add(
            "%s line %d: <%s> directly inside <%s>"
                .formatted(name, lineOf(content, tag.start()), element, parent));
      }
      if (!selfClosing) {
        open.push(element);
      }
    }
    return violations;
  }

  /** The nearest open element that is still there once the page is rendered. */
  private static String renderedParent(Deque<String> open) {
    return open.stream()
        .filter(element -> !element.equals(TRANSPARENT))
        .findFirst()
        .orElse("nothing");
  }

  private static void popThrough(Deque<String> open, String element) {
    while (!open.isEmpty() && !open.peek().equals(element)) {
      open.pop();
    }
    if (!open.isEmpty()) {
      open.pop();
    }
  }

  private static boolean hasClass(String tag, String className) {
    Matcher classes = CLASS_ATTRIBUTE.matcher(tag);
    return classes.find() && List.of(classes.group(1).trim().split("\\s+")).contains(className);
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
