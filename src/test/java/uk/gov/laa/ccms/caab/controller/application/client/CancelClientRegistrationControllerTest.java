package uk.gov.laa.ccms.caab.controller.application.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class CancelClientRegistrationControllerTest {

  @InjectMocks
  private CancelClientRegistrationController cancelClientRegistrationController;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(cancelClientRegistrationController).build();
  }

  @Test
  public void testClientDetailsCancelGet() throws Exception {
    mockMvc.perform(get("/application/client/details/cancel"))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/cancel-client"));
  }

  @Test
  public void testClientDetailsCancelPost() throws Exception {
    mockMvc.perform(post("/application/client/details/cancel"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/search"));
  }
}