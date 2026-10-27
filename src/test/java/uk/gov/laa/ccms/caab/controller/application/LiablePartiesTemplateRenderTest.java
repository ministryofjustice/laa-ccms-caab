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
    "cost-award, COST, Liable Parties, recovery",
    "financial-award, DAMAGE, Liable Parties, recovery",
    "land, LAND, Interested Parties, overview",
    "asset, ASSET, Interested Parties, overview"
  })
  void rendersAwardContextAndNavigation(
      String path, String type, String heading, String destination) throws Exception {
    final String html = render(path, false);
    assertThat(html)
        .contains(">Outcome and awards</h1>", ">" + heading + "</h2>", ">" + type + "</dd>")
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
        .containsPattern("href=\"/case/outcome-and-awards\"[^>]*>Back</a>")
        .containsPattern(
            "href=\"/case/outcome-and-awards\"[^>]*>Cancel and return to outcome and awards</a>")
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

  @Test
  void emptyListShowsNoneAndRetainsAddAndNext() throws Exception {
    final String html = render("financial-award", true);
    assertThat(html).contains("colspan=\"5\">None</td>", ">Add Party</a>", ">Next</a>");
    assertSingleAddPartyButtonAboveNavigation(html);
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
    return mockMvc
        .perform(get("/test/liable-parties").param("path", path).param("empty", "" + empty))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Controller
  static class HarnessController {
    @GetMapping("/test/liable-parties")
    String display(@RequestParam String path, @RequestParam boolean empty, final Model model) {
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
              parties));
      model.addAttribute("awardPath", path);
      model.addAttribute("awardId", 7);
      return "test/liable-parties-harness";
    }
  }
}
