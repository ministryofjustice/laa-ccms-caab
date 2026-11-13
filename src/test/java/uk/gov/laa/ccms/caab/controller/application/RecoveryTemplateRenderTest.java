package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.validators.awards.RecoveryValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.RecoveryMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

class RecoveryTemplateRenderTest {
  private MockMvc mvc;
  private final ApplicationDetail ebsCase =
      new ApplicationDetail()
          .caseReferenceNumber("300000001")
          .availableFunctions(List.of(FunctionConstants.OUTCOME_NO_DISCHARGE));

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
    engine.addDialect(new GovUkDialect());
    engine.addDialect(new MojCustomDialect());
    final var views = new ThymeleafViewResolver();
    views.setTemplateEngine(engine);
    views.setCharacterEncoding("UTF-8");
    final var service = mock(CaseOutcomeService.class);
    when(service.getFinancialAward("300000001", 123, 7))
        .thenReturn(
            Optional.of(
                new FinancialAwardDetail()
                    .id(7)
                    .awardType("DAMAGE")
                    .description("Financial award")
                    .awardAmount(new BigDecimal("1000.00"))));
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AwardController(
                    null,
                    service,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new RecoveryValidator(),
                    Mappers.getMapper(RecoveryMapper.class),
                    Mappers.getMapper(LiablePartiesDisplayMapper.class),
                    null))
            .setViewResolvers(views)
            .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"solicitorRecoveryDate", "courtRecoveryDate", "clientRecoveryDate"})
  void rendersDateErrorsWithPreservedInputAndMatchingSummaryAnchor(String field) throws Exception {
    mvc.perform(
            post("/case/outcome-and-awards/financial-award/7/recovery")
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                .flashAttr("user", new UserDetail().functions(List.of()))
                .param("action", "calculate")
                .param(field, "31/02/2026"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"#" + field + "\"")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<input(?=[^>]*id=\""
                            + field
                            + "\")"
                            + "(?=[^>]*name=\""
                            + field
                            + "\")(?=[^>]*value=\"31/02/2026\")[^>]*>.*")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "solicitorRecoveryAmount",
        "solicitorRecoveryDate",
        "solicitorAmountPaidToLsc",
        "courtRecoveryAmount",
        "courtRecoveryDate",
        "courtAmountPaidToLsc",
        "clientRecoveryAmount",
        "clientRecoveryDate",
        "clientAmountPaidToLsc"
      })
  void retainsOneLabelledControlPerFieldAcrossResponsiveLayouts(String field) throws Exception {
    final String html =
        mvc.perform(
                get("/case/outcome-and-awards/financial-award/7/recovery")
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        matchesPattern(
                            "(?s).*<label[^>]*for=\"" + field + "\"[^>]*>[^<]+</label>.*")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(StringUtils.countOccurrencesOf(html, "id=\"" + field + "\"")).isEqualTo(1);
    assertThat(StringUtils.countOccurrencesOf(html, "name=\"" + field + "\"")).isEqualTo(1);
  }

  @Test
  void rendersSharedDateHintAndRetainsIndividualAccessibleHints() throws Exception {
    final String html =
        mvc.perform(
                get("/case/outcome-and-awards/financial-award/7/recovery")
                    .sessionAttr(CASE, ebsCase)
                    .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        matchesPattern(
                            "(?s).*<p class=\"govuk-body recovery-recipients__date-label\">"
                                + "\\s*<span>Date received \\(optional\\)</span>"
                                + "\\s*<span class=\"govuk-hint govuk-!-margin-bottom-0\">"
                                + "For example, 27/03/2007</span>\\s*</p>.*")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(StringUtils.countOccurrencesOf(html, "For example, 27/03/2007")).isEqualTo(4);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "solicitorRecoveryAmount",
        "courtRecoveryAmount",
        "clientRecoveryAmount",
        "solicitorAmountPaidToLsc",
        "courtAmountPaidToLsc",
        "clientAmountPaidToLsc",
        "leaveOfCourtRequiredInd",
        "offeredAmount",
        "offerDetails"
      })
  void rendersAllEditableFieldsAndNavigation(String field) throws Exception {
    mvc.perform(
            get("/case/outcome-and-awards/financial-award/7/recovery")
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                .flashAttr("user", new UserDetail().functions(List.of())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1 class=\"govuk-heading-l\">Recovery</h1>")))
        .andExpect(
            content()
                .string(
                    stringContainsInOrder(
                        List.of(
                            "<div class=\"recovery-recipients\">",
                            "aria-hidden=\"true\"",
                            "Amount recovered (optional)",
                            "Date received (optional)",
                            "Paid to LAA (optional)",
                            "aria-labelledby=\"solicitor-heading\"",
                            "name=\"solicitorRecoveryAmount\"",
                            "name=\"solicitorRecoveryDate\"",
                            "name=\"solicitorAmountPaidToLsc\"",
                            "aria-labelledby=\"court-heading\"",
                            "name=\"courtRecoveryAmount\"",
                            "name=\"courtRecoveryDate\"",
                            "name=\"courtAmountPaidToLsc\"",
                            "aria-labelledby=\"client-heading\"",
                            "name=\"clientRecoveryAmount\"",
                            "name=\"clientRecoveryDate\"",
                            "name=\"clientAmountPaidToLsc\""))))
        .andExpect(
            content()
                .string(not(matchesPattern("(?s).*<h[12][^>]*>Outcome and [Aa]wards</h[12]>.*"))))
        .andExpect(content().string(containsString("name=\"" + field + "\"")))
        .andExpect(content().string(containsString("Financial award")))
        .andExpect(content().string(containsString("£1000.00")))
        .andExpect(
            content()
                .string(
                    stringContainsInOrder(
                        List.of(
                            "name=\"action\" value=\"calculate\"",
                            "name=\"action\" value=\"next\"",
                            "href=\"/case/outcome-and-awards/financial-award/7/liable-parties\""))))
        .andExpect(content().string(containsString("href=\"/case/outcome-and-awards\"")));
  }
}
