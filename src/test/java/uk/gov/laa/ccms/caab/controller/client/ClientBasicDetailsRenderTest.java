package uk.gov.laa.ccms.caab.controller.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;

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
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/**
 * Renders the client basic details fragment through a real Thymeleaf engine so that the read-only
 * country of origin - which is not editable when amending a client - can be asserted on.
 */
class ClientBasicDetailsRenderTest {

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
  @DisplayName("Country of origin shows the description, not the reference code, when editing")
  void countryOfOriginShowsDescriptionWhenEditing() throws Exception {
    mockMvc
        .perform(get("/test/basic-details").param("action", ACTION_EDIT))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">United Kingdom<")))
        .andExpect(content().string(not(containsString(">GBR<"))))
        // The code must still be posted back, so the hidden input keeps it.
        .andExpect(content().string(containsString("name=\"countryOfOrigin\" value=\"GBR\"")));
  }

  @Test
  @DisplayName("Country of origin falls back to the code when no description is available")
  void countryOfOriginFallsBackToCode() throws Exception {
    mockMvc
        .perform(get("/test/basic-details").param("action", ACTION_EDIT).param("resolved", "false"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">GBR<")));
  }

  @Test
  @DisplayName("Country of origin is an editable dropdown when creating a client")
  void countryOfOriginIsDropdownWhenCreating() throws Exception {
    mockMvc
        .perform(get("/test/basic-details").param("action", ACTION_CREATE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<select class=\"govuk-select\"")))
        .andExpect(content().string(containsString("id=\"countryOfOrigin\"")))
        .andExpect(content().string(containsString(">United Kingdom<")));
  }

  /** Serves the render harness template with the model the basic details fragment expects. */
  @Controller
  static class HarnessController {

    @GetMapping("/test/basic-details")
    public String basicDetails(
        @RequestParam final String action,
        @RequestParam(defaultValue = "true") final boolean resolved,
        final Model model) {
      final ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
      basicDetails.setClientFlowFormAction(action);
      basicDetails.setCountryOfOrigin("GBR");

      model.addAttribute("basicDetails", basicDetails);
      if (resolved) {
        model.addAttribute("countryOfOriginDisplayValue", "United Kingdom");
      }
      model.addAttribute("titles", lookup("MR", "Mr").getContent());
      model.addAttribute("countries", lookup("GBR", "United Kingdom").getContent());
      model.addAttribute("genders", lookup("M", "Male").getContent());
      model.addAttribute("maritalStatusList", lookup("S", "Single").getContent());
      return "test/basic-details-harness";
    }

    private CommonLookupDetail lookup(final String code, final String description) {
      return new CommonLookupDetail()
          .addContentItem(new CommonLookupValueDetail().code(code).description(description));
    }
  }
}
