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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/**
 * Renders the client contact details fragment through a real Thymeleaf engine so that the generated
 * markup - in particular the GOV.UK password input - can be asserted on.
 */
class ClientContactDetailsRenderTest {

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
  @DisplayName("Password field renders as a GDS password input with a show toggle")
  void passwordFieldUsesGdsPasswordInput() throws Exception {
    mockMvc
        .perform(get("/test/contact-details").param("action", ACTION_CREATE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-module=\"govuk-password-input\"")))
        .andExpect(content().string(containsString("govuk-js-password-input-input")))
        .andExpect(content().string(containsString("govuk-js-password-input-toggle")))
        .andExpect(content().string(containsString("id=\"password\"")))
        .andExpect(content().string(containsString("type=\"password\"")))
        .andExpect(content().string(containsString("aria-controls=\"password\"")))
        .andExpect(content().string(containsString("aria-label=\"Show password\"")))
        // Creation flow, so password managers should be told this is a new password.
        .andExpect(content().string(containsString("autocomplete=\"new-password\"")));
  }

  @Test
  @DisplayName("Password value is retained so it survives a validation error re-render")
  void passwordValueIsRetained() throws Exception {
    mockMvc
        .perform(get("/test/contact-details").param("action", ACTION_CREATE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"s3cret\"")));
  }

  @Test
  @DisplayName("Password field is not rendered when editing an existing client")
  void passwordFieldHiddenWhenEditing() throws Exception {
    mockMvc
        .perform(get("/test/contact-details").param("action", ACTION_EDIT))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("data-module=\"govuk-password-input\""))));
  }

  @Test
  @DisplayName("Password field shows the error styling and message when validation fails")
  void passwordFieldRendersErrorState() throws Exception {
    mockMvc
        .perform(get("/test/contact-details").param("action", ACTION_CREATE).param("error", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("govuk-form-group--error")))
        .andExpect(content().string(containsString("govuk-input--error")))
        .andExpect(content().string(containsString("id=\"password-error\"")))
        .andExpect(content().string(containsString("aria-describedby=\"password-error\"")))
        .andExpect(content().string(containsString("Enter a password")))
        // The value must survive the re-render so the user does not have to retype it.
        .andExpect(content().string(containsString("value=\"s3cret\"")));
  }

  /** Serves the render harness template with the model the contact fragment expects. */
  @Controller
  static class HarnessController {

    @GetMapping("/test/contact-details")
    public String contactDetails(
        @RequestParam final String action,
        @RequestParam(defaultValue = "false") final boolean error,
        final Model model) {
      final ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
      contactDetails.setClientFlowFormAction(action);
      contactDetails.setVulnerableClient(false);
      contactDetails.setPassword("s3cret");
      contactDetails.setPasswordReminder("first pet");

      model.addAttribute("contactDetails", contactDetails);
      if (error) {
        final BindingResult bindingResult =
            new BeanPropertyBindingResult(contactDetails, "contactDetails");
        // Deliberately free of characters that HTML-escaping would rewrite, so the test can
        // assert on the readable message. The production wording is covered by the validator test.
        bindingResult.rejectValue("password", "required.password", "Enter a password");
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "contactDetails", bindingResult);
      }
      model.addAttribute("correspondenceMethods", lookup().getContent());
      model.addAttribute("correspondenceLanguages", lookup().getContent());
      return "test/password-input-harness";
    }

    private CommonLookupDetail lookup() {
      return new CommonLookupDetail()
          .addContentItem(new CommonLookupValueDetail().code("A").description("A"));
    }
  }
}
