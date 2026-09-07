package uk.gov.laa.ccms.caab.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@DisplayName("Authentication error controller test")
class AuthenticationErrorControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    // A view resolver with a prefix and suffix keeps the resolved view distinct from the request
    // path, which standalone setup would otherwise dispatch back to itself.
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthenticationErrorController())
            .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
            .build();
  }

  @Test
  @DisplayName("Renders the authentication error page")
  void rendersAuthenticationErrorPage() throws Exception {
    mockMvc
        .perform(get("/authentication-error"))
        .andExpect(status().isOk())
        .andExpect(view().name("authentication-error"));
  }
}
