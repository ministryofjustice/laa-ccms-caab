package uk.gov.laa.ccms.caab.controller.submission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubmissionConfirmedControllerTest {

  @InjectMocks
  private SubmissionConfirmedController submissionConfirmedController;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(submissionConfirmedController)
        .setConversionService(getConversionService()).build();
  }

  @Test
  void testSubmissionsConfirmed() throws Exception {
    mockMvc.perform(get("/application/testType/confirmed"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionConfirmed"));
  }
}
