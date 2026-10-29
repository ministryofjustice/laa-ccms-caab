package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;
import uk.gov.laa.ccms.caab.model.sections.LiablePartyDisplay;

class LiablePartiesTemplateRenderTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    final var context = new GenericWebApplicationContext(new MockServletContext());
    context.refresh();
    final var resolver = new SpringResourceTemplateResolver();
    resolver.setApplicationContext(context);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    final var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    final var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    final var viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(engine);
    viewResolver.setCharacterEncoding("UTF-8");
    mockMvc = standaloneSetup(new HarnessController()).setViewResolvers(viewResolver).build();
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award, COST, Liable Parties, recovery, false, cost-award/7",
    "financial-award, DAMAGE, Liable Parties, recovery, false, financial-award/7",
    "land, LAND, Interested Parties, overview, false, land-property/7",
    "asset, ASSET, Interested Parties, overview, false, asset/7",
    "land, LAND, Interested Parties, overview, true, land/7/time-related-recovery",
    "asset, ASSET, Interested Parties, overview, true, asset/7/time-related-recovery"
  })
  void rendersAwardContextAndNavigation(
      String path,
      String type,
      String heading,
      String destination,
      boolean timeRelatedRecovery,
      String backPath)
      throws Exception {
    final String backUrl = "/case/outcome-and-awards/" + backPath;
    final String html = render(path, false, timeRelatedRecovery, backUrl);
    assertThat(html)
        .contains(">" + heading + "</h1>", ">" + type + "</dd>")
        .doesNotContain(">Outcome and awards</h1>", "<h2", "no-parties-message")
        .contains("Award Amount or Value", "1,234.50")
        .contains("Name</th>", "Party Type</th>", "Relationship to Case</th>", "Address</th>")
        .contains("href=\"/case/outcome-and-awards/" + path + "/7/liable-parties/add\"");
    assertSingleAddPartyButtonAboveNavigation(html);
    final String next =
        "recovery".equals(destination)
            ? "/case/outcome-and-awards/" + path + "/7/recovery"
            : "/case/outcome-and-awards";
    assertThat(html).containsPattern("href=\"" + next + "\"[^>]*>Next</a>");
    assertThat(html)
        .containsPattern(
            "class=\"govuk-back-link\"[^>]*href=\"/case/outcome-and-awards\"[^>]*>"
                + "Cancel and return to outcome and awards</a>")
        .containsPattern("class=\"govuk-link\"[^>]*href=\"" + backUrl + "\"[^>]*>Back</a>")
        .doesNotContain("<input", "<form");
  }

  @Test
  void rendersPartiesWithScopedLinksAndEscapesData() throws Exception {
    final String html = render("land", false);
    assertThat(html)
        .contains("Ms Jane Smith", "Acme &amp; Co", "Owner", "1 High Street AB1 2CD")
        .contains("href=\"/case/outcome-and-awards/land/7/liable-parties/11/individual\"")
        .contains("href=\"/case/outcome-and-awards/land/7/liable-parties/12/organisation\"")
        .contains("href=\"/case/outcome-and-awards/land/7/liable-parties/101/remove\"")
        .contains("href=\"/case/outcome-and-awards/land/7/liable-parties/102/remove\"")
        .contains("<span>Imported party</span>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        .doesNotContain("<script>alert(1)</script>", "/13/individual");
  }

  @ParameterizedTest
  @CsvSource({"cost-award", "financial-award", "land", "asset"})
  void emptyListShowsInsetMessageInsteadOfTableAndRetainsActions(String path) throws Exception {
    final String html = render(path, true);
    assertThat(html)
        .containsPattern(
            "class=\"govuk-inset-text\"[^>]*id=\"no-parties-message\"[^>]*>"
                + "No parties have been added.</div>")
        .contains(">Add Party</a>", ">Next</a>", ">Back</a>")
        .doesNotContain("<table", "<thead", "Name</th>", "Party Type</th>", "None</td>");
    assertThat(html.indexOf("id=\"no-parties-message\""))
        .isLessThan(html.indexOf("id=\"addPartyButton\""));
    assertSingleAddPartyButtonAboveNavigation(html);
  }

  @Test
  void rendersPartyListSelectionScreen() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/party-list").param("path", "land").param("empty", "false"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html)
        .contains("Party list")
        .contains("Add selected parties")
        .contains("Individual or Organisation not in the list")
        .contains(
            "If an individual or organisation is not in the list, you could add a new individual or organisation to the case")
        .contains("Add Organisation")
        .contains("Add Individual")
        .contains("govuk-checkboxes")
        .contains(">Select</th>")
        .contains("aria-label=\"Select Ms Jane Smith\"")
        .contains("class=\"govuk-checkboxes__label\"")
        .doesNotContain("Sheffield City Council")
        .containsPattern(
            "href=\"/case/outcome-and-awards/land/7/liable-parties\"[^>]*>Cancel and return to liable / interested parties</a>")
        .contains("name=\"selectedPartyIds\"");
  }

  private void assertSingleAddPartyButtonAboveNavigation(String html) {
    assertThat(html.split(">Add Party</a>", -1)).hasSize(2);
    assertThat(html)
        .containsPattern(
            "class=\"govuk-button govuk-button--secondary\"[^>]*"
                + "data-module=\"govuk-button\"[^>]*role=\"button\"[^>]*"
                + "id=\"addPartyButton\"[^>]*>Add Party</a>");
    assertThat(html.indexOf("id=\"addPartyButton\"")).isGreaterThan(html.indexOf("</table>"));
    assertThat(html.indexOf("id=\"addPartyButton\"")).isLessThan(html.indexOf(">Next</a>"));
  }

  private String render(String path, boolean empty) throws Exception {
    return render(
        path,
        empty,
        false,
        "/case/outcome-and-awards/" + ("land".equals(path) ? "land-property" : path) + "/7");
  }

  private String render(String path, boolean empty, boolean timeRelatedRecovery, String backUrl)
      throws Exception {
    return mockMvc
        .perform(
            get("/test/liable-parties")
                .param("path", path)
                .param("empty", "" + empty)
                .param("timeRelatedRecovery", "" + timeRelatedRecovery)
                .param("backUrl", backUrl))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Controller
  static class HarnessController {
    @GetMapping("/test/liable-parties")
    String display(
        @RequestParam String path,
        @RequestParam boolean empty,
        @RequestParam boolean timeRelatedRecovery,
        @RequestParam String backUrl,
        final Model model) {
      final String type =
          switch (path) {
            case "cost-award" -> "COST";
            case "financial-award" -> "DAMAGE";
            case "land" -> "LAND";
            default -> "ASSET";
          };
      final List<LiablePartyDisplay> parties =
          empty
              ? List.of()
              : List.of(
                  new LiablePartyDisplay(
                      101,
                      11,
                      "Ms Jane Smith",
                      "Individual",
                      "Owner",
                      "1 High Street AB1 2CD",
                      true,
                      false),
                  new LiablePartyDisplay(
                      102, 12, "Acme & Co", "Organisation", "Owner", "", true, true),
                  new LiablePartyDisplay(
                      103,
                      13,
                      "Imported party",
                      "Individual",
                      "",
                      "<script>alert(1)</script>",
                      false,
                      false));
      model.addAttribute(
          "award",
          new LiablePartiesDisplay(
              type,
              "Award",
              new BigDecimal("1234.50"),
              "COST".equals(type) || "DAMAGE".equals(type),
              timeRelatedRecovery,
              parties));
      model.addAttribute("awardPath", path);
      model.addAttribute("awardId", 7);
      model.addAttribute("backUrl", backUrl);
      return "test/liable-parties-harness";
    }

    @GetMapping("/test/party-list")
    String displayPartyList(
        @RequestParam String path, @RequestParam boolean empty, final Model model) {
      final List<LiablePartyDisplay> parties =
          empty
              ? List.of()
              : List.of(
                  new LiablePartyDisplay(
                      101,
                      11,
                      "Ms Jane Smith",
                      "Individual",
                      "Owner",
                      "1 High Street AB1 2CD",
                      true,
                      false),
                  new LiablePartyDisplay(
                      102, 12, "Acme & Co", "Organisation", "Owner", "", true, true));
      model.addAttribute("parties", parties);
      model.addAttribute("awardPath", path);
      model.addAttribute("awardId", 7);
      model.addAttribute("user", java.util.Map.of("functions", java.util.List.of()));
      return "application/party-list-screen";
    }
  }
}
