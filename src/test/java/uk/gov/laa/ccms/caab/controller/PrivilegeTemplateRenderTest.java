package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.config.UserRoleProvider;
import uk.gov.laa.ccms.caab.util.UserRoleUtil;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Renders a page through a real Thymeleaf engine with the Spring Security dialect, so the {@code
 * sec:authorize} checks against {@code @roles} and the layout's missing-roles message are exercised
 * against the user's granted authorities.
 */
@DisplayName("Privilege-restricted template rendering")
class PrivilegeTemplateRenderTest {

  private static SpringTemplateEngine engine;
  private static MockServletContext servletContext;
  private static GenericWebApplicationContext applicationContext;

  @BeforeAll
  static void setUp() {
    servletContext = new MockServletContext();
    applicationContext = new GenericWebApplicationContext(servletContext);
    applicationContext.registerBean("roles", UserRoleProvider.class);
    applicationContext.registerBean(FilterInvocationExpressionHandler.class);
    applicationContext.refresh();
    servletContext.setAttribute(
        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

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
    engine.addDialect(new SpringSecurityDialect());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private String renderHome(final String... authorities) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", null, authorities));

    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>();
    variables.put(
        ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
        new ThymeleafEvaluationContext(applicationContext, new DefaultConversionService()));
    variables.put("cspNonce", "test-nonce");
    variables.put("userRoleUtil", new UserRoleUtil());
    variables.put(
        "user",
        new UserDetail()
            .functions(Arrays.asList(authorities))
            .provider(new BaseProvider().name("Test Provider"))
            .firms(List.of()));
    variables.put("showNotifications", true);
    variables.put("actionsMsg", "3 Outstanding Actions (none overdue)");
    variables.put("notificationsMsg", "View Notifications (2 outstanding)");

    return engine.process("home", new WebContext(exchange, Locale.UK, variables));
  }

  @Test
  @DisplayName("Actions render as links and buttons when the user holds their roles")
  void rendersActionsWhenAuthorised() {
    final String html = renderHome("YCA", "NOT", "CA", "PRNC");

    assertThat(html)
        .contains("id=\"new-application-link\"")
        .contains("href=\"/application/new\"")
        .contains("id=\"create-general-request-link\"")
        .contains("href=\"/notifications/search?notification_type=A\"")
        .contains("href=\"/notifications/search?notification_type=N\"")
        .doesNotContain("You cannot perform the following actions");
  }

  @Test
  @DisplayName("Missing roles hide buttons, reduce links to text and are listed in the layout")
  void restrictsActionsWhenUnauthorised() {
    final String html = renderHome("YCA");

    assertThat(html)
        .doesNotContain("new-application-link")
        .doesNotContain("create-general-request-link")
        .doesNotContain("notification_type=")
        .contains("<span>3 Outstanding Actions (none overdue)</span>")
        .contains("You cannot perform the following actions")
        .contains("view notifications, ")
        .contains("create applications, ")
        .contains("create provider requests.");
  }

  @Test
  @DisplayName("Each restricted action is checked against its own role")
  void checksEachRoleIndependently() {
    final String html = renderHome("YCA", "NOT", "CA");

    assertThat(html)
        .contains("new-application-link")
        .contains("notification_type=A")
        .doesNotContain("create-general-request-link")
        .contains("create provider requests.");
  }

  /** Stands in for the handler Spring Security's web configuration registers at runtime. */
  static class FilterInvocationExpressionHandler
      extends AbstractSecurityExpressionHandler<FilterInvocation> {

    @Override
    protected SecurityExpressionOperations createSecurityExpressionRoot(
        final Authentication authentication, final FilterInvocation invocation) {
      return new SecurityExpressionRoot<>(() -> authentication, invocation) {};
    }
  }
}
