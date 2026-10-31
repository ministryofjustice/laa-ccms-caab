package uk.gov.laa.ccms.caab.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = InputRejectionLoggerWiringTest.Config.class)
class InputRejectionLoggerWiringTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SecurityEventLogger securityEventLogger;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    reset(securityEventLogger);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void logsARejectedParameterAndStillReturnsTheNormalBadRequest() throws Exception {
    mockMvc.perform(get("/probe").param("id", "abc")).andExpect(status().isBadRequest());

    verify(securityEventLogger)
        .inputRejected(eq("GET"), eq("/probe"), any(TypeMismatchException.class));
  }

  @Test
  void logsNothingForAValidRequest() throws Exception {
    mockMvc.perform(get("/probe").param("id", "1")).andExpect(status().isOk());

    verifyNoInteractions(securityEventLogger);
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @Bean
    SecurityEventLogger securityEventLogger() {
      return mock(SecurityEventLogger.class);
    }

    @Bean
    InputRejectionLogger inputRejectionLogger(SecurityEventLogger securityEventLogger) {
      return new InputRejectionLogger(securityEventLogger);
    }

    @Bean
    ProbeController probeController() {
      return new ProbeController();
    }
  }

  @Controller
  static class ProbeController {

    @GetMapping("/probe")
    @ResponseBody
    String probe(@RequestParam("id") int id) {
      return "ok " + id;
    }
  }
}
