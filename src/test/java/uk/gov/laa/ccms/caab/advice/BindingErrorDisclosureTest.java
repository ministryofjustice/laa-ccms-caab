package uk.gov.laa.ccms.caab.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;

class BindingErrorDisclosureTest {

  @ParameterizedTest
  @ValueSource(strings = {"feeEarnerId", "supervisorId"})
  void conversionErrorsAreSafeInSummaryAndInlineErrors(final String field) {
    final WebDataBinder binder = binder(new ApplicationFormData());
    binder.bind(new MutablePropertyValues(Map.of(field, "AUDIT_DIAGNOSTIC")));

    assertSafeError(binder.getBindingResult(), field);
  }

  @ParameterizedTest
  @ValueSource(strings = {"accepted", "amount", "failure"})
  void otherBindingFailuresHaveSafeDefaults(final String field) {
    final WebDataBinder binder = binder(new TestForm());
    binder.bind(new MutablePropertyValues(Map.of(field, "AUDIT_DIAGNOSTIC")));

    assertSafeError(binder.getBindingResult(), field);
  }

  @Test
  void validValuesAndBusinessValidationMessagesArePreserved() {
    final WebDataBinder binder = binder(new ApplicationFormData());
    binder.bind(new MutablePropertyValues(Map.of("feeEarnerId", "123")));
    assertThat(binder.getBindingResult().hasErrors()).isFalse();
    assertThat(((ApplicationFormData) binder.getTarget()).getFeeEarnerId()).isEqualTo(123);

    binder
        .getBindingResult()
        .rejectValue("feeEarnerId", "required.feeEarnerId", "Choose a fee earner.");
    assertThat(render(binder.getBindingResult(), "feeEarnerId")).contains("Choose a fee earner.");
  }

  private WebDataBinder binder(final Object target) {
    final WebDataBinder binder = new WebDataBinder(target, "form");
    new GlobalBinderAdvice(new BaselineTextValidator()).initBinder(binder);
    return binder;
  }

  private void assertSafeError(final BindingResult result, final String field) {
    assertThat(result.getFieldError(field)).isNotNull();
    assertThat(result.getFieldError(field).isBindingFailure()).isTrue();
    assertThat(result.getFieldError(field).getDefaultMessage()).isEqualTo("Enter a valid value.");
    assertThat(render(result, field))
        .contains("href=\"#" + field + "\"", "id=\"" + field + "-error\"")
        .containsPattern(
            "(?s)id=\"" + field + "-error\"[^>]*>\\s*Enter a valid value\\.\\s*</span>")
        .doesNotContain("java.lang", "Failed to convert", "For input string");
  }

  private String render(final BindingResult result, final String field) {
    final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    final ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
    templates.setPrefix("templates/");
    templates.setSuffix(".html");
    templates.setCheckExistence(true);
    templates.setOrder(1);
    final StringTemplateResolver inline = new StringTemplateResolver();
    inline.setOrder(2);
    final SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.addTemplateResolver(templates);
    engine.addTemplateResolver(inline);
    engine.setTemplateEngineMessageSource(messages);

    final MockServletContext servletContext = new MockServletContext();
    try (GenericWebApplicationContext applicationContext = new GenericWebApplicationContext()) {
      applicationContext.setServletContext(servletContext);
      applicationContext.getBeanFactory().registerSingleton("messageSource", messages);
      applicationContext.refresh();
      servletContext.setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
      final MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
      final MockHttpServletResponse response = new MockHttpServletResponse();
      final Map<String, Object> model = new HashMap<>(result.getModel());
      model.put("field", field);
      final RequestContext requestContext =
          new RequestContext(request, response, servletContext, model);
      model.put(
          "thymeleafRequestContext",
          new SpringWebMvcThymeleafRequestContext(requestContext, request));
      final WebContext context =
          new WebContext(
              JakartaServletWebApplication.buildApplication(servletContext)
                  .buildExchange(request, response),
              Locale.UK,
              model);
      return engine.process(
          """
          <form th:object="${form}">
            <div th:replace="~{partials/error-summary :: error-summary}"></div>
            <div th:replace="~{partials/forms :: textInput(fieldId=${field},
                label='Value', inputClass='')}"></div>
          </form>
          """,
          context);
    }
  }

  public static class TestForm {
    private Boolean accepted;
    private BigDecimal amount;

    public Boolean getAccepted() {
      return accepted;
    }

    public void setAccepted(final Boolean accepted) {
      this.accepted = accepted;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(final BigDecimal amount) {
      this.amount = amount;
    }

    public String getFailure() {
      return "";
    }

    public void setFailure(final String failure) {
      throw new IllegalArgumentException("AUDIT_DIAGNOSTIC");
    }
  }
}
