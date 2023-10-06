package uk.gov.laa.ccms.caab.controller.submission;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.controller.submission.ClientCreateSubmissionInProgressController;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;

@ExtendWith(MockitoExtension.class)
public class ClientCreateSubmissionInProgressControllerTest {

  @Mock
  private ClientService clientService;

  @Mock
  private HttpSession session;

  @InjectMocks
  private ClientCreateSubmissionInProgressController controller;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void testSubmissionsInProgress_withClientReferenceNumber() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    ClientStatus clientStatus = new ClientStatus();
    clientStatus.setClientReferenceNumber("123456");

    ClientDetail clientDetail = new ClientDetail();

    when(clientService.getClientStatus(anyString(), anyString(), anyString())).thenReturn(Mono.just(clientStatus));
    when(clientService.getClient(anyString(), anyString(), anyString())).thenReturn(Mono.just(clientDetail));

    mockMvc.perform(
            get("/submissions/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/client-create/confirmed"));
  }

  @Test
  void testSubmissionsInProgress_withoutClientReferenceNumber() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    ClientStatus clientStatus = new ClientStatus();

    when(clientService.getClientStatus(anyString(), anyString(), anyString())).thenReturn(Mono.just(clientStatus));

    mockMvc.perform(
            get("/submissions/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"));
  }

  @Test
  void testSubmissionsInProgress_withPollingThresholdNotReached() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    ClientStatus clientStatus = new ClientStatus();

    when(clientService.getClientStatus(anyString(), anyString(), anyString())).thenReturn(Mono.just(clientStatus));

    // Set the submission poll count below the threshold (6 in your current logic)
    int submissionPollCount = 3;

    mockMvc.perform(
            get("/submissions/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr("submissionPollCount", submissionPollCount))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"));
  }

  @Test
  void testSubmissionsInProgress_withPollingThresholdReached() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    ClientStatus clientStatus = new ClientStatus();

    when(clientService.getClientStatus(anyString(), anyString(), anyString())).thenReturn(Mono.just(clientStatus));

    // Set the submission poll count at the threshold (6 in your current logic)
    int submissionPollCount = 6;

    mockMvc.perform(
            get("/submissions/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr("submissionPollCount", submissionPollCount))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/client-create/failed"));
  }

}