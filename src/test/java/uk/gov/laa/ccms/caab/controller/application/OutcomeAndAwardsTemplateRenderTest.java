package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.bean.PreCertificateAndLegalHelpCostsFormData;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("Outcome and awards template render tests")
class OutcomeAndAwardsTemplateRenderTest {

  private static SpringTemplateEngine engine;

  @ParameterizedTest
  @MethodSource("deletionAwards")
  void deleteLinkAndConfirmationIdentifyAwardTypeWithoutDisplayingDetails(BaseAwardDetail award) {
    final String type = award.getAwardType();
    final String path = "/case/outcome-and-awards/awards/" + type + "/8/delete";
    assertThat(render(List.of(award))).contains("href=\"" + path + "\"");
    final String html =
        render(
            List.of(award), true, List.of(), Map.of(), Map.of(), "application/delete-award", award);

    assertThat(html)
        .contains("Delete " + type + " award")
        .contains("this " + type + " award")
        .contains("action=\"" + path + "\"")
        .contains("method=\"post\"")
        .contains("id=\"confirmDeleteAward\"")
        .contains("id=\"cancelDeleteAward\"")
        .containsPattern("id=\"cancelDeleteAward\"\\s+href=\"/case/outcome-and-awards\"")
        .doesNotContain(
            "null", "deleteAwardSummary", "12/02/2026", "Selected award", "£123.45", "£999.99");
  }

  @Test
  void readOnlySummaryDoesNotLinkToDeletion() {
    assertThat(
            render(
                List.of(new FinancialAwardDetail().id(8).awardType("DAMAGE")),
                false,
                List.of(),
                Map.of(),
                Map.of()))
        .doesNotContain("/awards/DAMAGE/8/delete");
  }

  @Test
  void confirmationOnlyRequiresAwardTypeAndId() {
    final FinancialAwardDetail award = new FinancialAwardDetail().id(8).awardType("DAMAGE");
    final String html =
        render(
            List.of(award), true, List.of(), Map.of(), Map.of(), "application/delete-award", award);

    assertThat(html).contains("Delete DAMAGE award").doesNotContain("null");
  }

  private static Stream<BaseAwardDetail> deletionAwards() {
    return Stream.<BaseAwardDetail>of(
            new CostAwardDetail()
                .awardType("COST")
                .preCertificateLscCost(new BigDecimal("100.00"))
                .certificateCostMarket(new BigDecimal("23.45"))
                .awardAmount(new BigDecimal("999.99")),
            new FinancialAwardDetail().awardType("DAMAGE").awardAmount(new BigDecimal("123.45")),
            new LandAwardDetail()
                .awardType("LAND")
                .valuationAmount(new BigDecimal("123.45"))
                .awardAmount(new BigDecimal("999.99")),
            new OtherAssetAwardDetail()
                .awardType("ASSET")
                .valuationAmount(new BigDecimal("123.45"))
                .awardAmount(new BigDecimal("999.99")))
        .peek(
            award -> {
              award.setId(8);
              award.setDescription("Selected award");
              award.setDateOfOrder(
                  Date.from(
                      java.time.LocalDate.of(2026, 2, 12)
                          .atStartOfDay(java.time.ZoneId.systemDefault())
                          .toInstant()));
            });
  }

  @BeforeAll
  static void setUpEngine() {
    final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");

    final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messageSource);
  }

  @Test
  @DisplayName("Other asset summary displays valuation amount instead of awarded amount")
  void otherAssetSummaryDisplaysValuationAmount() {
    final OtherAssetAwardDetail asset =
        new OtherAssetAwardDetail()
            .id(8)
            .awardType("ASSET")
            .description("Antique jewellery")
            .valuationAmount(new BigDecimal("123.45"))
            .awardedAmount(new BigDecimal("75.00"));

    final String html = render(List.of(asset));

    assertThat(html).contains("£123.45").doesNotContain("£75.00");
  }

  @Test
  @DisplayName("Pre-certificate and legal help costs summary uses table headers")
  void preCertificateAndLegalHelpCostsSummaryUsesTableHeaders() {
    final String html = render(List.of());

    assertThat(html)
        .contains("id=\"preCertificateAndLegalHelpCostsTable\"")
        .contains("class=\"govuk-table\"")
        .contains("<th scope=\"col\" class=\"govuk-table__header\">Type</th>")
        .contains("<th scope=\"col\" class=\"govuk-table__header\">Pre-certificate amount</th>")
        .contains("<th scope=\"col\" class=\"govuk-table__header\">Legal help amount</th>")
        .contains("id=\"preCertificateAndLegalHelpCostsLink\"")
        .contains("id=\"preCertificateAndLegalHelpCostsPreCertificateAmount\"")
        .contains("id=\"preCertificateAndLegalHelpCostsLegalHelpAmount\"");
  }

  @ParameterizedTest
  @CsvSource({"COST, cost-award", "DAMAGE, financial-award", "LAND, land-property", "ASSET, asset"})
  void retainsAwardDetailsLinksWithoutDirectPartySummaryActions(String type, String path) {
    final BaseAwardDetail award =
        switch (type) {
          case "COST" -> new CostAwardDetail().id(7).awardType(type);
          case "DAMAGE" -> new FinancialAwardDetail().id(7).awardType(type);
          case "LAND" -> new LandAwardDetail().id(7).awardType(type);
          default -> new OtherAssetAwardDetail().id(7).awardType(type);
        };
    assertThat(render(List.of(award)))
        .containsPattern("href=\"/case/outcome-and-awards/" + path + "/7\"[^>]*>" + type + "</a>")
        .contains(">Delete</a>")
        .doesNotContain("/liable-parties", ">Liable Parties</a>", ">Interested Parties</a>");
  }

  @Test
  @DisplayName("Read-only EBS award shows type even when award id is null")
  void readOnlyAwardShowsTypeWhenIdMissing() {
    final OtherAssetAwardDetail asset =
        new OtherAssetAwardDetail()
            .awardType("ASSET")
            .description("Antique jewellery")
            .valuationAmount(new BigDecimal("123.45"));

    final String html = render(List.of(asset), false, List.of(), Map.of(), Map.of());

    assertThat(html).contains("ASSET");
  }

  @Test
  @DisplayName("Read-only case hides proceeding edit and clear links")
  void readOnlyCaseHidesProceedingActionLinks() {
    final ProceedingDetail proceeding =
        new ProceedingDetail()
            .description("Proceeding 1")
            .proceedingCaseId("pc1")
            .availableFunctions(List.of(FunctionConstants.CLEAR_RECORDED_OUTCOME));
    final ProceedingOutcomeDetail clearable =
        new ProceedingOutcomeDetail().id(99).proceedingCaseId("pc1");

    final String html =
        render(List.of(), false, List.of(proceeding), Map.of(), Map.of("pc1", clearable));

    assertThat(html)
        .contains("Proceeding 1")
        .doesNotContain("proceedingOutcomeSummaryLink-0")
        .doesNotContain("clearProceedingOutcomeLink-0");
  }

  private String render(final List<? extends BaseAwardDetail> awards) {
    return render(awards, true, List.of(), Map.of(), Map.of());
  }

  private String render(
      final List<? extends BaseAwardDetail> awards,
      final boolean awardEditAllowed,
      final List<ProceedingDetail> proceedings,
      final Map<String, ProceedingOutcomeDetail> resolvedOutcomes,
      final Map<String, ProceedingOutcomeDetail> clearableOutcomes) {
    return render(
        awards,
        awardEditAllowed,
        proceedings,
        resolvedOutcomes,
        clearableOutcomes,
        "application/outcome-and-awards",
        null);
  }

  private String render(
      final List<? extends BaseAwardDetail> awards,
      final boolean awardEditAllowed,
      final List<ProceedingDetail> proceedings,
      final Map<String, ProceedingOutcomeDetail> resolvedOutcomes,
      final Map<String, ProceedingOutcomeDetail> clearableOutcomes,
      final String template,
      final BaseAwardDetail award) {
    final MockServletContext servletContext = new MockServletContext();
    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>();
    variables.put("awards", awards);
    if (award != null) {
      variables.put("awardType", award.getAwardType());
      variables.put("awardId", award.getId());
    }
    variables.put("awardEditAllowed", awardEditAllowed);
    variables.put("proceedings", proceedings);
    variables.put("resolvedOutcomes", resolvedOutcomes);
    variables.put("clearableOutcomes", clearableOutcomes);
    variables.put("documents", List.of());
    variables.put("outcomeDocumentActionAllowed", false);
    variables.put(
        "preCertificateAndLegalHelpCostsSummary", new PreCertificateAndLegalHelpCostsFormData());
    variables.put("cspNonce", "test-nonce");
    variables.put("researchPanelLink", "/research");
    variables.put("user", new UserDetail().functions(List.of()));

    return engine.process(template, new WebContext(exchange, Locale.UK, variables));
  }
}
