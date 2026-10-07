package uk.gov.laa.ccms.caab.controller.application;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/** Renders the case search form fragment through a real Thymeleaf engine. */
class CaseSearchFormRenderTest {

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
  @DisplayName("Search selects use plain labels, not the page heading parameter")
  void searchSelectsUsePlainLabels() throws Exception {
    mockMvc
        .perform(get("/test/case-search"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<label class=\"govuk-label\" for=\"feeEarnerId\">Fee earner</label>")))
        .andExpect(
            content()
                .string(
                    containsString("<label class=\"govuk-label\" for=\"officeId\">Office</label>")))
        .andExpect(content().string(not(containsString("govuk-label-wrapper"))));
  }

  /** Serves the case search harness with the model the form fragment expects. */
  @Controller
  static class HarnessController {

    @GetMapping("/test/case-search")
    public String caseSearch(final Model model) {
      model.addAttribute("caseSearchCriteria", new CaseSearchCriteria());
      model.addAttribute("feeEarners", List.of(Map.of("id", 1, "name", "Jane Smith")));
      model.addAttribute("offices", List.of(Map.of("id", 1, "name", "Bristol")));
      model.addAttribute("statuses", List.of(Map.of("code", "OPEN", "description", "Open")));
      return "test/case-search-harness";
    }
  }
}
