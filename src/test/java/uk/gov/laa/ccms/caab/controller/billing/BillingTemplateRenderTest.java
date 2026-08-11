package uk.gov.laa.ccms.caab.controller.billing;

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
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Smoke tests that render the billing pages through a real Thymeleaf engine, including the shared
 * layout and its partials.
 *
 * <p>The billing controller tests use a standalone MockMvc that only resolves view <em>names</em>,
 * so a page that renders through the layout incorrectly - for example omitting the {@code
 * breadcrumbs} fragment the layout requires - passes there but 500s at runtime. These render the
 * pages so that class of error is caught. The billing pages are {@code pageCategory='cases'}, for
 * which the phase banner's user/active-case blocks are skipped, so no session globals beyond an
 * (empty-function) user are needed.
 *
 * <p>The POA declaration page is not rendered here: its error-summary sits inside a {@code
 * th:object} form, so {@code #fields} needs the full Spring MVC request context that only a real
 * MVC render sets up. Its flow is covered by {@code BillingControllerTest}.
 */
@DisplayName("Billing template render smoke tests")
class BillingTemplateRenderTest {

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

  /**
   * Renders a page through the layout. Throws if the page or the layout cannot be processed, which
   * is what makes this a smoke test.
   */
  private String render(final String template, final Map<String, Object> model) {
    final MockServletContext servletContext = new MockServletContext();
    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>(model);
    // Referenced by the layout's script tags.
    variables.put("cspNonce", "test-nonce");
    // The shared header reads user.functions (via #lists.contains), so supply an empty one.
    variables.put("user", new UserDetail().functions(List.of()));

    return engine.process(template, new WebContext(exchange, Locale.UK, variables));
  }

  @Test
  @DisplayName("POA confirmation renders through the layout")
  void poaConfirmationRenders() {
    final String html =
        render("application/billing/poa-confirmation", Map.of("transactionId", "INV-1"));
    assertThat(html).contains("poa-submission-reference").contains("INV-1");
  }

  @Test
  @DisplayName("POA details renders through the layout with the summary and submit actions")
  void poaDetailsRenders() {
    final Map<String, Object> model = new HashMap<>();
    model.put("assessmentStatus", "Complete");
    model.put("assessmentComplete", true);
    model.put("viewPoaSummary", true);

    final String html = render("application/billing/poa-details", model);
    assertThat(html).contains("poa-summary-link").contains("submit-poa-button");
  }

  @Test
  @DisplayName("POA remove confirmation renders through the layout")
  void poaRemoveRenders() {
    final String html = render("application/billing/poa-remove", Map.of());
    assertThat(html).contains("confirm-remove-poa-button");
  }
}
