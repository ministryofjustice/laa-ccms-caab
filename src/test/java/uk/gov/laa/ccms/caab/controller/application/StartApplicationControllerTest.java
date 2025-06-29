package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
@DisplayName("StartApplicationController test")
class StartApplicationControllerTest {

  private MockMvcTester mockMvc;

  @InjectMocks private StartApplicationController startApplicationController;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcTester.create(
            standaloneSetup(startApplicationController)
                .setConversionService(getConversionService())
                .build());
  }

  @Test
  @DisplayName("GET: /application/new should create new application and redirect")
  void shouldCreateNewApplication() {
    assertThat(mockMvc.perform(get("/application/new")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/application/office");
  }

  @Test
  @DisplayName("GET: /amendments/new should create new application and redirect")
  void shouldCreateNewAmendment() {
    assertThat(mockMvc.perform(get("/amendments/new")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/application-type");
  }
}
