package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("Outcome and awards template render tests")
class OutcomeAndAwardsTemplateRenderTest {

  private static SpringTemplateEngine engine;

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

  private String render(final List<OtherAssetAwardDetail> awards) {
    final MockServletContext servletContext = new MockServletContext();
    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>();
    variables.put("awards", awards);
    variables.put("proceedings", List.of());
    variables.put("resolvedOutcomes", Map.of());
    variables.put("clearableOutcomes", Map.of());
    variables.put("documents", List.of());
    variables.put("outcomeDocumentActionAllowed", false);
    variables.put(
        "preCertificateAndLegalHelpCostsSummary", new PreCertificateAndLegalHelpCostsFormData());
    variables.put("cspNonce", "test-nonce");
    variables.put("researchPanelLink", "/research");
    variables.put("user", new UserDetail().functions(List.of()));

    return engine.process(
        "application/outcome-and-awards", new WebContext(exchange, Locale.UK, variables));
  }
}
