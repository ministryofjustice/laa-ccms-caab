package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CsrfLoggingAccessDeniedHandlerTest.Config.class)
class CsrfLoggingAccessDeniedHandlerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SecurityEventLogger securityEventLogger;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.reset(securityEventLogger);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void aWrongTokenIsLoggedAndStillRefusedWithA403() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute(
        HttpSessionCsrfTokenRepository.class.getName().concat(".CSRF_TOKEN"),
        new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected"));

    mockMvc
        .perform(post("/probe").session(session).param("_csrf", "forged"))
        .andExpect(status().isForbidden());

    verify(securityEventLogger)
        .csrfRejected(
            org.mockito.ArgumentMatchers.eq("POST"),
            org.mockito.ArgumentMatchers.eq("/probe"),
            org.mockito.ArgumentMatchers.any(InvalidCsrfTokenException.class));
  }

  @Test
  void aValidTokenPassesWithoutAnEvent() throws Exception {
    mockMvc.perform(post("/probe").with(csrf())).andExpect(status().isOk());

    verifyNoInteractions(securityEventLogger);
  }

  @Test
  void aMissingSessionStillRedirectsAsBeforeWithoutAnEvent() throws Exception {
    mockMvc
        .perform(post("/probe"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"));

    verifyNoInteractions(securityEventLogger);
  }

  @Test
  void otherAccessDeniedIsNotLoggedAsCsrfAndIsStillHandled() throws Exception {
    var handler = new CsrfLoggingAccessDeniedHandler(securityEventLogger);
    var response = new MockHttpServletResponse();

    handler.handle(
        new MockHttpServletRequest("GET", "/x"), response, new AccessDeniedException("no"));

    assertEquals(403, response.getStatus());
    verifyNoInteractions(securityEventLogger);
  }

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  static class Config {

    @Bean
    SecurityEventLogger securityEventLogger() {
      return mock(SecurityEventLogger.class);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityEventLogger securityEventLogger)
        throws Exception {
      return http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
          .sessionManagement(
              s -> s.invalidSessionStrategy(new SimpleRedirectInvalidSessionStrategy("/home")))
          .exceptionHandling(
              e -> e.accessDeniedHandler(new CsrfLoggingAccessDeniedHandler(securityEventLogger)))
          .build();
    }

    @Bean
    ProbeController probeController() {
      return new ProbeController();
    }
  }

  @Controller
  static class ProbeController {

    @PostMapping("/probe")
    @ResponseBody
    String probe() {
      return "ok";
    }
  }
}
