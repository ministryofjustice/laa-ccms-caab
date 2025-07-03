package uk.gov.laa.ccms.caab.controller.submission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_CLIENT_NAMES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

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
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
public class ClientSubmissionsInProgressControllerTest {

  @Mock private ClientService clientService;

  @Mock private SubmissionConstants submissionConstants;

  @Mock private HttpSession session;

  @InjectMocks private ClientSubmissionsInProgressController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setConversionService(getConversionService())
            .build();
  }

  @Test
  void testSubmissionsInProgress_withClientReferenceNumber() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    TransactionStatus clientStatus = new TransactionStatus();
    clientStatus.setReferenceNumber("123456");

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));

    mockMvc
        .perform(
            get("/application/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client-create/confirmed"));
  }

  @Test
  void testSubmissionsInProgress_withoutClientReferenceNumber() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    TransactionStatus clientStatus = new TransactionStatus();

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));

    mockMvc
        .perform(
            get("/application/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"));
  }

  @Test
  void testSubmissionsInProgress_withPollingThresholdNotReached() throws Exception {
    final UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    final TransactionStatus clientStatus = new TransactionStatus();

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));

    when(submissionConstants.getMaxPollCount()).thenReturn(6);

    // Set the submission poll count below the threshold (6 in your current logic)
    final int submissionPollCount = 3;

    mockMvc
        .perform(
            get("/application/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(SUBMISSION_POLL_COUNT, submissionPollCount))
        .andExpect(view().name("submissions/submissionInProgress"));
  }

  @Test
  void testSubmissionsInProgress_withPollingThresholdReached() throws Exception {
    UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    TransactionStatus clientStatus = new TransactionStatus();

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));

    // Set the submission poll count at the threshold (6 in your current logic)
    int submissionPollCount = 6;

    mockMvc
        .perform(
            get("/application/client-create")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr("submissionPollCount", submissionPollCount))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client-create/failed"));
  }

  @Test
  void testClientUpdateSubmission_withClientReferenceNumber() throws Exception {
    final UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    final BaseClientDetail baseClient =
        new BaseClientDetail().firstName("testFirstName").surname("testSurname");

    final TransactionStatus clientStatus = new TransactionStatus();
    clientStatus.setReferenceNumber("123456");

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));
    when(clientService.updateClientNames(anyString(), any(), any())).thenReturn(Mono.empty());

    mockMvc
        .perform(
            get("/application/client-update")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(APPLICATION_CLIENT_NAMES, baseClient))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client-update/confirmed"));
  }

  @Test
  void testClientUpdateSubmission_withoutClientReferenceNumber() throws Exception {
    final UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    final BaseClientDetail baseClient =
        new BaseClientDetail().firstName("testFirstName").surname("testSurname");

    final TransactionStatus clientStatus = new TransactionStatus();

    when(clientService.getClientStatus(anyString())).thenReturn(Mono.just(clientStatus));

    mockMvc
        .perform(
            get("/application/client-update")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "123")
                .sessionAttr(USER_DETAILS, user)
                .sessionAttr(APPLICATION_CLIENT_NAMES, baseClient))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"));
  }
}
