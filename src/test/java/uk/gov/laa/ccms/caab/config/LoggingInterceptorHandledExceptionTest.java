package uk.gov.laa.ccms.caab.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.security.InputRejectionLogger;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;

/** Drives a real DispatcherServlet, so the request state is what Spring really leaves behind. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = LoggingInterceptorHandledExceptionTest.Config.class)
class LoggingInterceptorHandledExceptionTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SecurityEventLogger securityEventLogger;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    reset(securityEventLogger);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void aFailureHandledByAnExceptionHandlerThatReturnsAViewIsRecordedAsAFailure() throws Exception {
    var activeCase =
        ActiveCase.builder().caseReferenceNumber("300001").clientReferenceNumber("CL1").build();

    mockMvc
        .perform(get("/case/overview").sessionAttr("activeCase", activeCase))
        .andExpect(status().isOk());

    verify(securityEventLogger)
        .dataAccessed(eq("VIEW"), eq(false), eq("300001"), eq("CL1"), any(), any());
  }

  @Test
  void aRequestThatSucceedsIsStillRecordedAsASuccess() throws Exception {
    var activeCase =
        ActiveCase.builder().caseReferenceNumber("300001").clientReferenceNumber("CL1").build();

    mockMvc
        .perform(get("/case/fine").sessionAttr("activeCase", activeCase))
        .andExpect(status().isOk());

    verify(securityEventLogger)
        .dataAccessed(eq("VIEW"), eq(true), eq("300001"), eq("CL1"), any(), any());
  }

  @Test
  void aNotificationViewIsAuditedByItsIdWithoutAnyCaseInTheSession() throws Exception {
    mockMvc.perform(get("/notifications/98765")).andExpect(status().isOk());

    verify(securityEventLogger)
        .dataAccessed(
            eq("VIEW"),
            eq(true),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            eq(java.util.Map.of("notification_id", "98765")),
            eq("/notifications/{notification_id}"));
  }

  @Configuration
  @EnableWebMvc
  static class Config implements WebMvcConfigurer {

    @Bean
    SecurityEventLogger securityEventLogger() {
      return mock(SecurityEventLogger.class);
    }

    @Bean
    InputRejectionLogger inputRejectionLogger(SecurityEventLogger securityEventLogger) {
      return new InputRejectionLogger(securityEventLogger);
    }

    @Bean
    ErrorViewAdvice errorViewAdvice() {
      return new ErrorViewAdvice();
    }

    @Bean
    FailingController failingController() {
      return new FailingController();
    }

    @Bean
    InternalResourceViewResolver viewResolver() {
      return new InternalResourceViewResolver("/WEB-INF/", ".html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new LoggingInterceptor(securityEventLogger()));
    }
  }

  @Controller
  static class FailingController {
    @GetMapping("/case/overview")
    String overview() {
      throw new IllegalStateException("backend unavailable");
    }

    @GetMapping("/case/fine")
    String fine() {
      return "overview";
    }

    @GetMapping("/notifications/{notification_id}")
    String notification(
        @org.springframework.web.bind.annotation.PathVariable("notification_id") String id) {
      return "overview";
    }
  }

  @ControllerAdvice
  static class ErrorViewAdvice {
    @ExceptionHandler(Exception.class)
    String handle(Exception exception) {
      return "error";
    }
  }
}
