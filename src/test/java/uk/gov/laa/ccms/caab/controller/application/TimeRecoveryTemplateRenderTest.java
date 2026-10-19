package uk.gov.laa.ccms.caab.controller.application;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.validators.awards.TimeRecoveryValidator;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/** Renders AW07 with the shared layout and form components. */
class TimeRecoveryTemplateRenderTest {

  private MockMvc mockMvc;
  private CaseOutcomeService caseOutcomeService;

  @BeforeEach
  void setUp() {
    final GenericWebApplicationContext context =
        new GenericWebApplicationContext(new MockServletContext());
    context.refresh();
    final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    resolver.setApplicationContext(context);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    final SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    engine.addDialect(new GovUkDialect());
    engine.addDialect(new MojCustomDialect());
    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(engine);
    viewResolver.setCharacterEncoding("UTF-8");

    caseOutcomeService = org.mockito.Mockito.mock(CaseOutcomeService.class);
    org.mockito.Mockito.when(caseOutcomeService.getLandAward("300000001", 123, 7))
        .thenReturn(
            Optional.of(
                new LandAwardDetail()
                    .id(7)
                    .awardType("LAND")
                    .description("Property")
                    .valuationAmount(new BigDecimal("250000.00"))
                    .recoveryOfAwardTimeRelated(true)));
    org.mockito.Mockito.when(caseOutcomeService.getOtherAssetAward("300000001", 123, 7))
        .thenReturn(
            Optional.of(
                new OtherAssetAwardDetail()
                    .id(7)
                    .awardType("ASSET")
                    .description("Jewellery")
                    .valuationAmount(new BigDecimal("250000.00"))
                    .awardedAmount(new BigDecimal("10000.00"))
                    .recoveryOfAwardTimeRelated(true)));
    mockMvc =
        standaloneSetup(
                new AwardController(
                    null,
                    caseOutcomeService,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new TimeRecoveryValidator()))
            .setViewResolvers(viewResolver)
            .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"land", "asset"})
  void dateErrorSummaryLinksToDatepickerInput(String awardPath) throws Exception {
    mockMvc
        .perform(
            post("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
                .sessionAttr(CASE, new ApplicationDetail().caseReferenceNumber("300000001"))
                .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                .flashAttr("user", new UserDetail().functions(List.of()))
                .param("triggeringEvent", "Sale")
                .param("effectiveDate", "31/02/2026")
                .param("timeRelatedRecoveryDetails", "When sold"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"#effectiveDate\"")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<input(?=[^>]*id=\"effectiveDate\")"
                            + "(?=[^>]*name=\"effectiveDate\")"
                            + "(?=[^>]*value=\"31/02/2026\")[^>]*>.*")));
    org.mockito.Mockito.verify(caseOutcomeService, org.mockito.Mockito.never())
        .upsertTimeRecovery(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @ParameterizedTest
  @CsvSource({"land, LAND, Property, land-property", "asset, ASSET, Jewellery, asset"})
  void rendersReadOnlyAwardValuesAndEditableRecoveryFields(
      String awardPath, String awardType, String description, String parentPath) throws Exception {
    mockMvc
        .perform(
            get("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
                .sessionAttr(CASE, new ApplicationDetail().caseReferenceNumber("300000001"))
                .sessionAttr(USER_DETAILS, ApplicationTestUtils.buildUser())
                .flashAttr("user", new UserDetail().functions(List.of())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Time related recovery")))
        .andExpect(content().string(containsString("250000.00")))
        .andExpect(content().string(containsString(awardType)))
        .andExpect(content().string(containsString(description)))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a class=\"govuk-back-link\"\\s+href=\"/case/outcome-and-awards\">"
                            + "Cancel and return to outcome and awards</a>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a class=\"govuk-link\"\\s+href=\"/case/outcome-and-awards/"
                            + parentPath
                            + "/7\">Back</a>.*")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/case/outcome-and-awards/"
                            + awardPath
                            + "/7/time-related-recovery\"")))
        .andExpect(content().string(containsString("name=\"triggeringEvent\"")))
        .andExpect(content().string(containsString("name=\"effectiveDate\"")))
        .andExpect(content().string(containsString("name=\"timeRelatedRecoveryDetails\"")));
  }
}
