package uk.gov.laa.ccms.caab.controller.submission;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientCreateSubmissionConfirmedControllerTest {

  @Mock
  private HttpSession httpSession;

  @Mock
  private ApplicationService applicationService;;

  @InjectMocks
  private ClientCreateSubmissionConfirmedController clientCreateSubmissionConfirmedController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(clientCreateSubmissionConfirmedController).build();
  }

  @Test
  void testSubmissionsConfirmed() throws Exception {
    ApplicationFormData mockApplicationFormData = new ApplicationFormData(); // Assuming you have a constructor or a mock object
    UserDetail mockUserDetail = new UserDetail(); // Similarly, create or mock
    ClientDetail mockClientDetail = new ClientDetail();

    when(applicationService.createApplication(eq(mockApplicationFormData), eq(mockClientDetail), eq(mockUserDetail))).thenReturn(Mono.empty());

    mockMvc.perform(
            post("/submissions/client-create/confirmed")
                .sessionAttr(APPLICATION_FORM_DATA, mockApplicationFormData)
                .sessionAttr(USER_DETAILS, mockUserDetail)
                .sessionAttr(CLIENT_INFORMATION, mockClientDetail)
        )
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary"));
  }
}