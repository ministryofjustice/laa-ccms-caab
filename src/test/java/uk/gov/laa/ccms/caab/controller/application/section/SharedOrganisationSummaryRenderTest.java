package uk.gov.laa.ccms.caab.controller.application.section;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/**
 * Renders the shared organisation summary card through a real Thymeleaf engine so that the
 * read-only country - which cannot be edited for a shared organisation - can be asserted on.
 */
class SharedOrganisationSummaryRenderTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    final GenericWebApplicationContext applicationContext =
        new GenericWebApplicationContext(new MockServletContext());
    applicationContext.refresh();

    final SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setApplicationContext(applicationContext);
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");

    final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");

    final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    templateEngine.setTemplateEngineMessageSource(messageSource);
    templateEngine.addDialect(new GovUkDialect());
    templateEngine.addDialect(new MojCustomDialect());

    // No application context set here: standaloneSetup injects its own stub.
    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine);
    viewResolver.setCharacterEncoding("UTF-8");

    mockMvc = standaloneSetup(new HarnessController()).setViewResolvers(viewResolver).build();
  }

  @Test
  @DisplayName("Country shows the description, not the reference code")
  void countryShowsDescription() throws Exception {
    mockMvc
        .perform(get("/test/shared-organisation-summary"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">Great Britain<")))
        .andExpect(content().string(not(containsString(">GBR<"))));
  }

  @Test
  @DisplayName("Country falls back to the code when no description is available")
  void countryFallsBackToCode() throws Exception {
    mockMvc
        .perform(get("/test/shared-organisation-summary").param("resolved", "false"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">GBR<")));
  }

  /** Serves the render harness template with the model the summary fragment expects. */
  @Controller
  static class HarnessController {

    @GetMapping("/test/shared-organisation-summary")
    public String sharedOrganisationSummary(
        @RequestParam(defaultValue = "true") final boolean resolved, final Model model) {
      final OrganisationOpponentFormData opponent = new OrganisationOpponentFormData();
      opponent.setOrganisationName("Test Organisation");
      opponent.setCountry("GBR");
      if (resolved) {
        opponent.setCountryDisplayValue("Great Britain");
      }

      model.addAttribute("opponent", opponent);
      return "test/shared-organisation-summary-harness";
    }
  }
}
