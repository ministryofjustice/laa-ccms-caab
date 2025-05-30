package uk.gov.laa.ccms.caab.controller.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
@DisplayName("StartApplicationController test")
class StartApplicationControllerTest {

  private MockMvc mockMvc;

  @InjectMocks
  private StartApplicationController startApplicationController;

  @BeforeEach
  void setup() {
    mockMvc = standaloneSetup(startApplicationController).build();
  }

  @Test
  @DisplayName("GET: /application/new should create new application and redirect")
  void shouldCreateNewApplication() throws Exception {
    this.mockMvc.perform(get("/application/new"))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/office"));
  }

  @Test
  @DisplayName("GET: /amendments/new should create new application and redirect")
  void shouldCreateNewAmendment() throws Exception {
    this.mockMvc.perform(get("/amendments/new"))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/application-type"));
  }
}