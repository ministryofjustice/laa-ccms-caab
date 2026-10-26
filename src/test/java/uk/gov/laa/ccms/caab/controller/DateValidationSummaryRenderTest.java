package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.util.ThymeleafTestUtils.createViewResolver;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.TimeRecoveryFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.data.model.UserDetail;

class DateValidationSummaryRenderTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc =
        standaloneSetup(new HarnessController()).setViewResolvers(createViewResolver()).build();
  }

  @ParameterizedTest
  @EnumSource(Page.class)
  void dateErrorsLinkToEditableInputsWithAssociatedLabelsHintsAndErrors(Page page)
      throws Exception {
    final String html = render(page, true);
    final String summary = capture("(<ul[^>]*id=\"error-summary-list\"[^>]*>.*?</ul>)", html);

    for (String field : page.fields) {
      final String input =
          capture("(<input\\b[^>]*name=\"" + Pattern.quote(field) + "\"[^>]*>)", html);
      final String id = attribute("id", input);
      final String message = "Invalid date for " + field;
      assertThat(summary).contains("href=\"#" + id + "\"").contains(message);
      assertThat(html.split("id=\"" + Pattern.quote(id) + "\"", -1)).hasSize(2);
      assertThat(input).doesNotContain("type=\"hidden\"").doesNotContain("disabled");
      assertThat(attribute("value", input)).isEqualTo("abc123");
      assertThat(html).containsPattern("<label\\b[^>]*for=\"" + Pattern.quote(id) + "\"");
      final List<String> descriptions = List.of(attribute("aria-describedby", input).split("\\s+"));
      assertThat(descriptions).contains(id + "-hint", id + "-error");
      assertThat(html).contains("id=\"" + id + "-hint\"");
      final String error =
          capture("(<p\\b[^>]*id=\"" + Pattern.quote(id) + "-error\"[^>]*>.*?</p>)", html);
      assertThat(HtmlUtils.htmlUnescape(error)).contains(message);
    }
  }

  @ParameterizedTest
  @EnumSource(Page.class)
  void validDatesRenderWithoutSummaryOrInlineErrors(Page page) throws Exception {
    final String html = render(page, false);

    assertThat(html)
        .doesNotContain("id=\"error-summary-list\"")
        .doesNotContain("class=\"govuk-error-message\"");
    for (String field : page.fields) {
      final String input =
          capture("(<input\\b[^>]*name=\"" + Pattern.quote(field) + "\"[^>]*>)", html);
      assertThat(attribute("value", input)).isEqualTo("04/03/2024");
    }
  }

  private String render(Page page, boolean errors) throws Exception {
    return mockMvc
        .perform(
            get("/test/date-summary")
                .param("page", page.name())
                .param("errors", Boolean.toString(errors)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static String attribute(String name, String tag) {
    return HtmlUtils.htmlUnescape(capture("\\b" + name + "=\"([^\"]*)\"", tag));
  }

  private static String capture(String regex, String html) {
    final Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(html);
    assertThat(matcher.find()).as("rendered markup matching %s", regex).isTrue();
    return matcher.group(1);
  }

  enum Page {
    CLIENT_SEARCH(
        "application/application-client-search",
        "clientSearchCriteria",
        ClientSearchCriteria::new,
        "dateOfBirth"),
    OPPONENT(
        "application/opponents/opponents-individual-create",
        "opponent",
        IndividualOpponentFormData::new,
        "dateOfBirth"),
    DECEASED(
        "application/sections/client-deceased-details",
        "deceasedDetails",
        ClientFormDataDeceasedDetails::new,
        "dateOfDeath"),
    NOTIFICATIONS(
        "notifications/actions-and-notifications-search",
        "notificationSearchCriteria",
        NotificationSearchCriteria::new,
        "notificationFromDate",
        "notificationToDate"),
    FINANCIAL(
        "application/financial-award",
        "financialAward",
        FinancialAwardFormData::new,
        "dateOfOrder",
        "orderServedDate"),
    LAND(
        "application/land-property",
        "landAward",
        LandAwardFormData::new,
        "dateOfOrder",
        "valuationDate"),
    OUTCOME(
        "application/record-proceeding-outcome",
        "proceedingOutcome",
        ProceedingOutcomeFormData::new,
        "dateOfFinalWork"),
    OTHER_ASSET(
        "application/other-asset-award",
        "otherAssetAward",
        OtherAssetAwardFormData::new,
        "dateOfOrder",
        "valuationDate"),
    RECOVERY(
        "application/time-related-recovery",
        "timeRecovery",
        TimeRecoveryFormData::new,
        "effectiveDate");

    final String template;
    final String formName;
    final Supplier<Object> form;
    final List<String> fields;

    Page(String template, String formName, Supplier<Object> form, String... fields) {
      this.template = template;
      this.formName = formName;
      this.form = form;
      this.fields = List.of(fields);
    }
  }

  @Controller
  static class HarnessController {
    @GetMapping("/test/date-summary")
    String render(@RequestParam Page page, @RequestParam boolean errors, Model model) {
      final Object form = page.form.get();
      final BeanWrapper bean = new BeanWrapperImpl(form);
      final BindingResult binding = new BeanPropertyBindingResult(form, page.formName);
      for (String field : page.fields) {
        bean.setPropertyValue(field, errors ? "abc123" : "04/03/2024");
        if (errors) {
          binding.rejectValue(field, "invalid.format", "Invalid date for " + field);
        }
      }
      model.addAttribute(page.formName, form);
      model.addAttribute(BindingResult.MODEL_KEY_PREFIX + page.formName, binding);
      model.addAttribute("caseContext", CaseContext.APPLICATION);
      model.addAttribute("user", new UserDetail().functions(List.of()));
      model.addAttribute("cspNonce", "test-nonce");
      model.addAttribute("researchPanelLink", "/research");
      model.addAttribute("usersUnavailable", false);
      model.addAttribute("feeEarnersUnavailable", false);
      model.addAttribute("awardPath", "land");
      model.addAttribute("awardId", 1);
      model.addAttribute(
          "award", Map.of("awardType", "Land", "description", "Property", "awardAmount", "100"));
      model.addAttribute("proceedingIndex", 0);
      for (String name :
          List.of(
              "genders",
              "uniqueIdentifierTypes",
              "contactTitles",
              "relationshipsToCase",
              "relationshipsToClient",
              "countries",
              "legalAidedOptions",
              "users",
              "feeEarners",
              "interimAwardOptions",
              "awardedByOptions",
              "valuationBasisOptions",
              "recoveryOptions",
              "landRegistrationOptions",
              "stageEnds",
              "resolutionMethods",
              "results",
              "alternativeDisputeResolutions",
              "widerBenefitsOptions")) {
        model.addAttribute(name, List.of());
      }
      return page.template;
    }
  }
}
