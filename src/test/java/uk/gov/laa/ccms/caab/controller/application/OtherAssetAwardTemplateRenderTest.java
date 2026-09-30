package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

@DisplayName("Other asset award template render tests")
class OtherAssetAwardTemplateRenderTest {

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

    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine);
    viewResolver.setCharacterEncoding("UTF-8");

    mockMvc = standaloneSetup(new HarnessController()).setViewResolvers(viewResolver).build();
  }

  @Test
  @DisplayName("Award and recovery retains grouped percentage and amount fields")
  void rendersGroupedPercentageAndAmountFields() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/other-asset-award"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html)
        .contains("<section class=\"govuk-form-group\">")
        .contains("<label class=\"govuk-label\" for=\"disputedPercentage\">Disputed</label>")
        .contains("<label class=\"govuk-label\" for=\"awardedPercentage\">Awarded</label>")
        .contains("<label class=\"govuk-label\" for=\"recoveredPercentage\">Recovered</label>")
        .containsPattern(inputPattern("disputedPercentage", "10.25", "6"))
        .containsPattern(inputPattern("awardedPercentage", "20.50", "6"))
        .containsPattern(inputPattern("recoveredPercentage", "30.75", "6"))
        .containsPattern(inputPattern("disputedAmount", "100.00", null))
        .containsPattern(inputPattern("awardedAmount", "200.00", null))
        .containsPattern(inputPattern("recoveredAmount", "300.00", null));
    assertThat(html.split("class=\"award-recovery-or\"", -1)).hasSize(4);
  }

  @Test
  @DisplayName("Award and recovery renders errors for every percentage input")
  void rendersPercentageErrors() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/other-asset-award-errors"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html)
        .containsPattern(errorMessagePattern("disputedPercentage", "Disputed percentage error"))
        .containsPattern(errorMessagePattern("awardedPercentage", "Awarded percentage error"))
        .containsPattern(errorMessagePattern("recoveredPercentage", "Recovered percentage error"))
        .containsPattern(errorInputPattern("disputedPercentage"))
        .containsPattern(errorInputPattern("awardedPercentage"))
        .containsPattern(errorInputPattern("recoveredPercentage"));
  }

  private static String inputPattern(final String id, final String value, final String maxLength) {
    final String maxLengthLookahead =
        maxLength == null ? "" : "(?=[^>]*maxlength=\\\"" + maxLength + "\\\")";
    return "<input(?=[^>]*id=\\\""
        + id
        + "\\\")(?=[^>]*value=\\\""
        + value
        + "\\\")"
        + maxLengthLookahead
        + "(?=[^>]*inputmode=\\\"decimal\\\")[^>]*>";
  }

  private static String errorInputPattern(final String id) {
    return "<input(?=[^>]*id=\\\""
        + id
        + "\\\")(?=[^>]*class=\\\"[^\\\"]*govuk-input--error)"
        + "(?=[^>]*aria-describedby=\\\""
        + id
        + "-error\\\")[^>]*>";
  }

  private static String errorMessagePattern(final String id, final String message) {
    return "<span(?=[^>]*id=\\\"" + id + "-error\\\")[^>]*>" + message + "</span>";
  }

  @Controller
  static class HarnessController {

    @GetMapping("/test/other-asset-award")
    public String otherAssetAward(final Model model) {
      model.addAttribute("otherAssetAward", formData());
      return "test/other-asset-award-harness";
    }

    @GetMapping("/test/other-asset-award-errors")
    public String otherAssetAwardWithErrors(final Model model) {
      final OtherAssetAwardTestForm formData = formData();
      final BindingResult bindingResult =
          new BeanPropertyBindingResult(formData, "otherAssetAward");
      bindingResult.rejectValue(
          "disputedPercentage", "invalid.percentage", "Disputed percentage error");
      bindingResult.rejectValue(
          "awardedPercentage", "invalid.percentage", "Awarded percentage error");
      bindingResult.rejectValue(
          "recoveredPercentage", "invalid.percentage", "Recovered percentage error");

      model.addAttribute("otherAssetAward", formData);
      model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "otherAssetAward", bindingResult);
      return "test/other-asset-award-harness";
    }

    private static OtherAssetAwardTestForm formData() {
      return new OtherAssetAwardTestForm("10.25", "20.50", "30.75", "100.00", "200.00", "300.00");
    }
  }

  static class OtherAssetAwardTestForm {

    private final String disputedPercentage;
    private final String awardedPercentage;
    private final String recoveredPercentage;
    private final String disputedAmount;
    private final String awardedAmount;
    private final String recoveredAmount;

    OtherAssetAwardTestForm(
        final String disputedPercentage,
        final String awardedPercentage,
        final String recoveredPercentage,
        final String disputedAmount,
        final String awardedAmount,
        final String recoveredAmount) {
      this.disputedPercentage = disputedPercentage;
      this.awardedPercentage = awardedPercentage;
      this.recoveredPercentage = recoveredPercentage;
      this.disputedAmount = disputedAmount;
      this.awardedAmount = awardedAmount;
      this.recoveredAmount = recoveredAmount;
    }

    public String getDisputedPercentage() {
      return disputedPercentage;
    }

    public String getAwardedPercentage() {
      return awardedPercentage;
    }

    public String getRecoveredPercentage() {
      return recoveredPercentage;
    }

    public String getDisputedAmount() {
      return disputedAmount;
    }

    public String getAwardedAmount() {
      return awardedAmount;
    }

    public String getRecoveredAmount() {
      return recoveredAmount;
    }
  }
}
