package uk.gov.laa.ccms.caab.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.web.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Exercises the production YAML through real MVC error and Actuator response boundaries. */
class ResponseDisclosureTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withInitializer(
              context -> {
                try {
                  new YamlPropertySourceLoader()
                      .load(
                          "production",
                          new FileSystemResource("build/resources/main/application.yml"))
                      .forEach(context.getEnvironment().getPropertySources()::addLast);
                } catch (IOException exception) {
                  throw new IllegalStateException(
                      "Cannot load production configuration", exception);
                }
              })
          .withConfiguration(
              AutoConfigurations.of(
                  JacksonAutoConfiguration.class,
                  HttpMessageConvertersAutoConfiguration.class,
                  DispatcherServletAutoConfiguration.class,
                  WebMvcAutoConfiguration.class,
                  ErrorMvcAutoConfiguration.class,
                  ThymeleafAutoConfiguration.class,
                  MessageSourceAutoConfiguration.class,
                  EndpointAutoConfiguration.class,
                  WebEndpointAutoConfiguration.class,
                  HealthContributorRegistryAutoConfiguration.class,
                  HealthEndpointAutoConfiguration.class,
                  ApplicationAvailabilityAutoConfiguration.class,
                  AvailabilityProbesAutoConfiguration.class,
                  WebMvcEndpointManagementContextConfiguration.class))
          .withUserConfiguration(HealthProbe.class);

  @Test
  void healthAndProbesRemainStatusOnlyWhenHealthyAndFailing() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          final MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
          AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
          AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
          for (String endpoint :
              new String[] {
                "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
              }) {
            mvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
            mvc.perform(get(endpoint).principal(() -> "provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
          }

          context.getBean(AtomicBoolean.class).set(true);
          mvc.perform(get("/actuator/health"))
              .andExpect(status().isServiceUnavailable())
              .andExpect(jsonPath("$.status").value("DOWN"))
              .andExpect(jsonPath("$.components").doesNotExist())
              .andExpect(jsonPath("$.details").doesNotExist())
              .andExpect(jsonPath("$.error").doesNotExist());
          AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
          mvc.perform(get("/actuator/health/readiness"))
              .andExpect(status().isServiceUnavailable())
              .andExpect(content().string("{\"status\":\"OUT_OF_SERVICE\"}"));
          AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
          mvc.perform(get("/actuator/health/liveness"))
              .andExpect(status().isServiceUnavailable())
              .andExpect(content().string("{\"status\":\"DOWN\"}"));
        });
  }

  @Test
  void fallbackJsonAndHtmlIgnoreDiagnosticQueryParameters() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          final MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
          for (MediaType accept :
              new MediaType[] {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML}) {
            final String body =
                mvc.perform(
                        get("/error?trace=true&message=true&errors=true")
                            .with(
                                request -> {
                                  request.setDispatcherType(DispatcherType.ERROR);
                                  return request;
                                })
                            .accept(accept)
                            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/test-failure")
                            .requestAttr(RequestDispatcher.ERROR_MESSAGE, "AUDIT_DIAGNOSTIC")
                            .requestAttr(
                                RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("AUDIT_DIAGNOSTIC")))
                    .andExpect(status().isInternalServerError())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(body)
                .doesNotContain(
                    "AUDIT_DIAGNOSTIC", "IllegalStateException", "java.lang", "\"trace\"");
            if (accept.equals(MediaType.APPLICATION_JSON)) {
              assertThat(body).contains("500", "Internal Server Error");
            } else {
              assertThat(body).contains("Unfortunately, something went wrong.");
            }
          }
          mvc.perform(
                  get("/error?trace=true&message=true&errors=true")
                      .accept(MediaType.APPLICATION_JSON)
                      .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                      .requestAttr(RequestDispatcher.ERROR_MESSAGE, "AUDIT_DIAGNOSTIC"))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").doesNotExist())
              .andExpect(jsonPath("$.exception").doesNotExist())
              .andExpect(jsonPath("$.trace").doesNotExist())
              .andExpect(jsonPath("$.errors").doesNotExist());
        });
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class HealthProbe {
    @Bean
    TemplateModel templateModel() {
      return new TemplateModel();
    }

    @Bean
    AtomicBoolean failed() {
      return new AtomicBoolean();
    }

    @Bean
    HealthIndicator diagnosticHealthIndicator(final AtomicBoolean failed) {
      return () ->
          (failed.get() ? Health.down(new IllegalStateException("AUDIT_DIAGNOSTIC")) : Health.up())
              .withDetail("path", "/synthetic/internal/path")
              .build();
    }

    @ControllerAdvice
    static class TemplateModel {
      @ModelAttribute
      void templateAttributes(final Model model) {
        model.addAttribute("researchPanelLink", "/research");
        model.addAttribute("user", new UserDetail().functions(List.of()));
      }
    }
  }
}
