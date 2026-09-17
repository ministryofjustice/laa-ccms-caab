package uk.gov.laa.ccms.caab.controller.submission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
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
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.data.model.UserDetail;

class SubmissionInProgressTemplateRenderTest {

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
  void clientSubmissionRendersDefaultStatusUrlWhenCustomUrlIsAbsent() {
    final String html =
        render(Map.of("caseContext", CaseContext.APPLICATION, "submissionType", "client-create"));

    assertThat(html).contains("href=\"/application/client-create\"");
  }

  @Test
  void undertakingSubmissionRendersCustomStatusUrl() {
    final String html =
        render(
            Map.of(
                "caseContext",
                CaseContext.AMENDMENTS,
                "submissionType",
                "submit-case",
                "submissionStatusUrl",
                "/amendments/submit-case/undertaking"));

    assertThat(html).contains("href=\"/amendments/submit-case/undertaking\"");
  }

  private String render(final Map<String, Object> model) {
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

    return engine.process(
        "submissions/submissionInProgress", new WebContext(exchange, Locale.UK, variables));
  }
}
