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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.awards.OtherAssetAwardValidator;

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

  @Test
  void rendersPairErrorsOnBothFieldsButOnlyOnceInTheSummary() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/other-asset-award-pair-errors"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final String summary = summaryHtml(html);

    for (final String row : new String[] {"Disputed", "Awarded", "Recovered"}) {
      final String fieldPrefix = row.toLowerCase(java.util.Locale.ROOT);
      final String messagePattern =
          "Enter either a percentage or an amount for [^<]*" + row + "[^<]*, not both\\.";
      assertThat(html)
          .containsPattern(errorMessagePattern(fieldPrefix + "Percentage", messagePattern))
          .containsPattern(errorMessagePattern(fieldPrefix + "Amount", messagePattern))
          .containsPattern(errorInputPattern(fieldPrefix + "Percentage"))
          .containsPattern(errorInputPattern(fieldPrefix + "Amount"));
      assertThat(summary)
          .containsOnlyOnce(row)
          .contains("href=\"#" + fieldPrefix + "Percentage\"")
          .doesNotContain("href=\"#" + fieldPrefix + "Amount\"");
    }
  }

  @Test
  void retainsDistinctFieldAndGlobalErrorsInTheSummary() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/other-asset-award-pair-errors").param("additionalErrors", "true"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(summaryHtml(html))
        .containsOnlyOnce("Awarded amount format error")
        .containsOnlyOnce("Other form error")
        .contains("href=\"#awardedAmount\"");
  }

  @Test
  void leavesDefaultSummaryBehaviourUnchanged() throws Exception {
    final String html =
        mockMvc
            .perform(get("/test/other-asset-award-pair-errors").param("defaultSummary", "true"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    for (final String row : new String[] {"disputed", "awarded", "recovered"}) {
      assertThat(summaryHtml(html))
          .contains("href=\"#" + row + "Percentage\"")
          .contains("href=\"#" + row + "Amount\"");
    }
  }

  private static String summaryHtml(final String html) {
    final int start = html.indexOf("id=\"error-summary-list\"");
    assertThat(start).isGreaterThanOrEqualTo(0);
    return html.substring(start, html.indexOf("</ul>", start));
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

    @GetMapping("/test/other-asset-award-pair-errors")
    public String otherAssetAwardWithPairErrors(
        final Model model,
        @RequestParam(defaultValue = "false") final boolean defaultSummary,
        @RequestParam(defaultValue = "false") final boolean additionalErrors) {
      final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
      form.setAwardType("ASSET");
      form.setAwardCode("OTH_ASSET");
      form.setDescription("Asset");
      form.setDateOfOrder("01/01/2025");
      form.setAwardedBy("COURT");
      form.setValuationAmount("1000.50");
      form.setValuationCriteria("AGREED");
      form.setValuationDate("02/01/2025");
      form.setRecovery("UNKNOWN");
      form.setRecoveryOfAwardTimeRelated(false);
      form.setDisputedPercentage("10.25");
      form.setDisputedAmount("100.00");
      form.setAwardedPercentage("20.50");
      form.setAwardedAmount("200.00");
      form.setRecoveredPercentage("30.75");
      form.setRecoveredAmount("300.00");
      final BindingResult bindingResult = new BeanPropertyBindingResult(form, "otherAssetAward");
      new OtherAssetAwardValidator().validate(form, bindingResult);
      if (additionalErrors) {
        bindingResult.rejectValue(
            "awardedAmount", "invalid.currency", "Awarded amount format error");
        bindingResult.reject("test.global", "Other form error");
      }

      model.addAttribute("otherAssetAward", form);
      model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "otherAssetAward", bindingResult);
      model.addAttribute("defaultSummary", defaultSummary);
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
