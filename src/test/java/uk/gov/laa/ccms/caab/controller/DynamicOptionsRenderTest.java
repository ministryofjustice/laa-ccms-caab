package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.LinkedHashMap;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.constants.PriorAuthorityGroup;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/** Renders the dynamic option fields that offer a list of values. */
class DynamicOptionsRenderTest {

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

  private String render(final String path) throws Exception {
    return mockMvc
        .perform(get(path))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
        .replaceAll("\\s+", " ");
  }

  @Test
  @DisplayName("Provider request LOV options render as a bound autocomplete with a display value")
  void providerRequestLovOption() throws Exception {
    assertThat(render("/test/dynamic-options/providerRequest"))
        .contains(
            "<input type=\"hidden\" id=\"dynamicOptions[REL].fieldValueDisplayValue\""
                + " name=\"dynamicOptions[REL].fieldValueDisplayValue\" value=\"Sibling\">")
        .contains(
            "<label class=\"govuk-label\" for=\"dynamicOptions[REL].fieldValue\">"
                + "Relationship (optional)</label>")
        .contains("<span class=\"govuk-visually-hidden\">Error:</span> Select a relationship")
        .contains("id=\"dynamicOptions[REL].fieldValue\" name=\"dynamicOptions[REL].fieldValue\"")
        .contains("data-display-value-id=\"dynamicOptions[REL].fieldValueDisplayValue\"")
        .contains("<option value=\"SIB\" selected>Sibling</option>")
        .contains(
            "id=\"dynamicOptions[NOTE].fieldValue\" name=\"dynamicOptions[NOTE].fieldValue\"");
  }

  @Test
  @DisplayName("Prior authority LOV options render as a bound autocomplete with a display value")
  void priorAuthorityLovOption() throws Exception {
    assertThat(render("/test/dynamic-options/priorAuthority"))
        .contains(
            "<input type=\"hidden\" id=\"dynamicOptions[EXP].fieldValueDisplayValue\""
                + " name=\"dynamicOptions[EXP].fieldValueDisplayValue\" value=\"Bee\">")
        .contains(
            "<label class=\"govuk-label\" for=\"dynamicOptions[EXP].fieldValue\">"
                + "Expert type (optional)</label>")
        .contains("<span class=\"govuk-visually-hidden\">Error:</span> Select an expert type")
        .contains("id=\"dynamicOptions[EXP].fieldValue\" name=\"dynamicOptions[EXP].fieldValue\"")
        .contains("data-display-value-id=\"dynamicOptions[EXP].fieldValueDisplayValue\"")
        .contains("<option value=\"B\" selected>Bee</option>")
        .contains("id=\"dynamicOptions[HRS].fieldValue\"");
  }

  private static DynamicOptionFormData opt(
      String code, String type, boolean mandatory, String desc, String value, String display) {
    DynamicOptionFormData o = new DynamicOptionFormData();
    o.setCode(code);
    o.setFieldType(type);
    o.setMandatory(mandatory);
    o.setFieldDescription(desc);
    o.setFieldValue(value);
    o.setFieldValueDisplayValue(display);
    return o;
  }

  @Controller
  static class HarnessController {

    @GetMapping("/test/dynamic-options/providerRequest")
    public String providerRequest(final Model model) {
      ProviderRequestDetailsFormData form = new ProviderRequestDetailsFormData();
      form.getDynamicOptions().put("REL", opt("REL", null, false, null, "SIB", "Sibling"));
      form.getDynamicOptions().put("NOTE", opt("NOTE", null, true, null, "hello", null));
      BindingResult br = new BeanPropertyBindingResult(form, "providerRequestDetails");
      br.rejectValue("dynamicOptions[REL].fieldValue", "req", "Select a relationship");
      model.addAttribute("providerRequestDetails", form);
      model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "providerRequestDetails", br);
      model.addAttribute(
          "providerRequestDynamicForm",
          Map.of(
              "dataItems",
              List.of(
                  Map.of(
                      "code",
                      "REL",
                      "type",
                      "LOV",
                      "label",
                      "Relationship",
                      "mandatoryFlag",
                      false),
                  Map.of("code", "NOTE", "type", "FTS", "label", "Note", "mandatoryFlag", true))));
      model.addAttribute(
          "REL",
          List.of(
              Map.of("code", "PAR", "description", "Parent"),
              Map.of("code", "SIB", "description", "Sibling")));
      model.addAttribute("page", "providerRequest");
      return "test/dynamic-options-harness";
    }

    @GetMapping("/test/dynamic-options/priorAuthority")
    public String priorAuthority(final Model model) {
      PriorAuthorityDetailsFormData form = new PriorAuthorityDetailsFormData();
      DynamicOptionFormData exp = opt("EXP", "LOV", false, "Expert type", "B", "Bee");
      DynamicOptionFormData hrs = opt("HRS", "FTS", true, "Hours", "3", null);
      form.getDynamicOptions().put("EXP", exp);
      form.getDynamicOptions().put("HRS", hrs);
      BindingResult br = new BeanPropertyBindingResult(form, "priorAuthorityDetails");
      br.rejectValue("dynamicOptions[EXP].fieldValue", "req", "Select an expert type");
      Map<PriorAuthorityGroup, List<DynamicOptionFormData>> grouped = new LinkedHashMap<>();
      grouped.put(PriorAuthorityGroup.OTHER, List.of(exp, hrs));
      model.addAttribute("priorAuthorityDetails", form);
      model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "priorAuthorityDetails", br);
      model.addAttribute("groupedDynamicOptions", grouped);
      model.addAttribute(
          "EXP",
          List.of(
              Map.of("code", "A", "description", "Ay"), Map.of("code", "B", "description", "Bee")));
      model.addAttribute("page", "priorAuthority");
      return "test/dynamic-options-harness";
    }
  }
}
