package uk.gov.laa.ccms.caab.controller.application.section;

import static org.assertj.core.api.Assertions.assertThat;

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
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.model.BooleanDisplayValue;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("Proceedings template render tests")
class ProceedingsTemplateRenderTest {

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
  @DisplayName("Proceedings confirm renders read-only delegated function checkboxes for SUBDP")
  void proceedingsConfirmRendersReadonlyDelegatedFunctionCheckboxesForSubdp() {
    final String html =
        render(
            "application/proceedings-confirm",
            Map.of(
                "caseContext",
                CaseContext.APPLICATION,
                "proceedingFlow",
                proceedingFlow(),
                "scopeLimitations",
                List.of(
                    scopeLimitation(1, "Emergency scope", "Emergency wording", true),
                    scopeLimitation(2, "Standard scope", "Standard wording", false)),
                "isSubstantiveDevolvedPowersApp",
                true));

    assertThat(html).contains("Delegated functions apply");
    assertThat(inputTag(html, "delegated-functions-0"))
        .contains("checked=\"checked\"")
        .contains("disabled=\"disabled\"");
    assertThat(inputTag(html, "delegated-functions-1"))
        .doesNotContain("checked=\"checked\"")
        .contains("disabled=\"disabled\"");
  }

  @Test
  @DisplayName("Proceedings confirm hides delegated functions column for non-SUBDP")
  void proceedingsConfirmHidesDelegatedFunctionsColumnForNonSubdp() {
    final String html =
        render(
            "application/proceedings-confirm",
            Map.of(
                "caseContext",
                CaseContext.APPLICATION,
                "proceedingFlow",
                proceedingFlow(),
                "scopeLimitations",
                List.of(scopeLimitation(1, "Standard scope", "Standard wording", false)),
                "isSubstantiveDevolvedPowersApp",
                false));

    assertThat(html).doesNotContain("Delegated functions apply");
    assertThat(html).doesNotContain("delegated-functions-0");
  }

  @Test
  @DisplayName("Proceedings confirm hides remove action when only one scope limitation exists")
  void proceedingsConfirmHidesRemoveActionWhenOnlyOneScopeLimitationExists() {
    final String html =
        render(
            "application/proceedings-confirm",
            Map.of(
                "caseContext",
                CaseContext.APPLICATION,
                "proceedingFlow",
                proceedingFlow(),
                "scopeLimitations",
                List.of(scopeLimitation(1, "Only scope", "Only wording", true)),
                "isSubstantiveDevolvedPowersApp",
                true));

    assertThat(html).doesNotContain("/application/proceedings/scope-limitations/0/remove");
  }

  @Test
  @DisplayName("Proceedings summary hides edit actions when updates are not allowed")
  void proceedingsSummaryHidesEditActionsWhenUpdatesAreNotAllowed() {
    final String html =
        render(
            "application/proceedings-summary",
            Map.of(
                "caseContext", CaseContext.APPLICATION,
                "proceeding", proceeding(),
                "updateProceedingAllowed", false,
                "isOriginalProceeding", false,
                "isSubstantiveDevolvedPowersApp", true));

    assertThat(html).doesNotContain("edit-proceeding-matter-type-link");
    assertThat(html).doesNotContain("edit-scope-limitations-link");
    assertThat(inputTag(html, "delegated-functions-1"))
        .contains("checked=\"checked\"")
        .contains("disabled=\"disabled\"");
    assertThat(inputTag(html, "delegated-functions-2"))
        .doesNotContain("checked=\"checked\"")
        .contains("disabled=\"disabled\"");
  }

  private String render(final String template, final Map<String, Object> model) {
    final MockServletContext servletContext = new MockServletContext();
    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>(model);
    variables.put("cspNonce", "test-nonce");
    variables.put("researchPanelLink", "/research");
    variables.put("user", new UserDetail().functions(List.of()));

    return engine.process(template, new WebContext(exchange, Locale.UK, variables));
  }

  private static ProceedingFlowFormData proceedingFlow() {
    final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData("add");
    proceedingFlow.getMatterTypeDetails().setMatterTypeDisplayValue("Matter type");
    proceedingFlow.getProceedingDetails().setProceedingTypeDisplayValue("Proceeding type");
    proceedingFlow.getProceedingDetails().setProceedingDescription("Proceeding description");
    proceedingFlow.getFurtherDetails().setClientInvolvementTypeDisplayValue("Client involvement");
    proceedingFlow.getFurtherDetails().setLevelOfServiceDisplayValue("Full representation");
    return proceedingFlow;
  }

  private static ProceedingDetail proceeding() {
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setMatterType(new StringDisplayValue().displayValue("Matter type"));
    proceeding.setProceedingType(new StringDisplayValue().displayValue("Proceeding type"));
    proceeding.setDescription("Proceeding description");
    proceeding.setClientInvolvement(new StringDisplayValue().displayValue("Client involvement"));
    proceeding.setLevelOfService(new StringDisplayValue().displayValue("Full representation"));
    proceeding.setTypeOfOrder(new StringDisplayValue());
    proceeding.setScopeLimitations(
        List.of(
            scopeLimitation(1, "Emergency scope", "Emergency wording", true),
            scopeLimitation(2, "Standard scope", "Standard wording", false)));
    return proceeding;
  }

  private static ScopeLimitationDetail scopeLimitation(
      final int id,
      final String code,
      final String wording,
      final boolean delegatedFunctionsApply) {
    return new ScopeLimitationDetail()
        .id(id)
        .scopeLimitation(new StringDisplayValue().id(code).displayValue(code))
        .scopeLimitationWording(wording)
        .delegatedFuncApplyInd(new BooleanDisplayValue().flag(delegatedFunctionsApply));
  }

  private static String inputTag(final String html, final String id) {
    final String marker = "id=\"" + id + "\"";
    final int idIndex = html.indexOf(marker);
    assertThat(idIndex).isGreaterThanOrEqualTo(0);
    final int start = html.lastIndexOf("<input", idIndex);
    final int end = html.indexOf('>', idIndex);
    return html.substring(start, end + 1);
  }
}
