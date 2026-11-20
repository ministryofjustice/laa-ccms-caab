package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.util.ThymeleafTestUtils.createViewResolver;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.data.model.UserDetail;

class LandPropertyTemplateRenderTest {

  @ParameterizedTest
  @CsvSource({", ''", "0.00, £0.00", "150000.00, £150000.00"})
  void rendersEquityAsCurrencyOnlyWhenPresent(String equity, String expected) throws Exception {
    final String html =
        standaloneSetup(new HarnessController())
            .setViewResolvers(createViewResolver())
            .build()
            .perform(get("/test/land-property").param("equity", equity))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(HtmlUtils.htmlUnescape(html))
        .containsPattern("<dd\\b[^>]*id=\"equity\"[^>]*>" + Pattern.quote(expected) + "</dd>");
  }

  @Test
  void preservesNonZeroEquityWhenRedisplayingAnUnrelatedValidationError() throws Exception {
    final MockMvc mockMvc =
        standaloneSetup(new HarnessController()).setViewResolvers(createViewResolver()).build();
    final String initialHtml =
        mockMvc
            .perform(get("/test/land-property").param("equity", "150000.00"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final Matcher equityInput =
        Pattern.compile("<input\\b(?=[^>]*type=\"hidden\")(?=[^>]*name=\"equity\")[^>]*>")
            .matcher(initialHtml);
    assertThat(equityInput.find()).isTrue();
    final Matcher equityValue = Pattern.compile("value=\"([^\"]*)\"").matcher(equityInput.group());
    assertThat(equityValue.find()).isTrue();

    final String html =
        mockMvc
            .perform(post("/test/land-property").param("equity", equityValue.group(1)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("landAward", "description"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(HtmlUtils.htmlUnescape(html))
        .containsPattern("<dd\\b[^>]*id=\"equity\"[^>]*>£150000.00</dd>")
        .contains("Enter a description");
  }

  @Controller
  static class HarnessController {
    @GetMapping("/test/land-property")
    String render(@RequestParam(required = false) String equity, Model model) {
      final LandAwardFormData form = new LandAwardFormData();
      form.setEquity(equity);
      model.addAttribute("landAward", form);
      return populateModel(model);
    }

    @PostMapping("/test/land-property")
    String redisplay(
        @ModelAttribute("landAward") LandAwardFormData form,
        BindingResult bindingResult,
        Model model) {
      bindingResult.rejectValue("description", "required", "Enter a description");
      return populateModel(model);
    }

    private String populateModel(Model model) {
      model.addAttribute("caseContext", CaseContext.APPLICATION);
      model.addAttribute("user", new UserDetail().functions(List.of()));
      model.addAttribute("cspNonce", "test-nonce");
      model.addAttribute("researchPanelLink", "/research");
      for (String name :
          List.of(
              "awardedByOptions",
              "valuationBasisOptions",
              "recoveryOptions",
              "landRegistrationOptions")) {
        model.addAttribute(name, List.of());
      }
      return "application/land-property";
    }
  }
}
