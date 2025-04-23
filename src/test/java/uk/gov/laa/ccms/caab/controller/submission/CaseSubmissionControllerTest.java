package uk.gov.laa.ccms.caab.controller.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CASE;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
class CaseSubmissionControllerTest {

  private MockMvc mockMvc;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private SubmissionConstants submissionConstants;

  @InjectMocks
  private CaseSubmissionController caseSubmissionController;

  @Mock
  private HttpSession session;

  private ActiveCase activeCase;

  private static final UserDetail userDetail = buildUserDetail();

  @Mock
  private Model model;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(caseSubmissionController).build();

    activeCase = ActiveCase.builder()
        .providerId(1)
        .applicationId(1)
        .caseReferenceNumber("abc123")
        .clientReferenceNumber("xyz123").build();
  }

  @Test
  @DisplayName("Test addCaseSubmission - Case confirmed")
  void addCaseSubmissionCaseConfirmed() throws Exception {

    final TransactionStatus mockStatus = new TransactionStatus();
    mockStatus.setReferenceNumber("ref123");
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    mockMvc.perform(get("/submissions/case-create")
            .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
            .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/%s/confirmed".formatted(SUBMISSION_CREATE_CASE)));

    verify(applicationService, times(1)).getCaseStatus(anyString());
  }

  @Test
  @DisplayName("Test addCaseSubmission - Case not confirmed, poll continues")
  void addCaseSubmissionCaseNotConfirmed() throws Exception {
    final TransactionStatus mockStatus = new TransactionStatus(); // No reference number set
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    mockMvc.perform(get("/submissions/case-create")
            .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
            .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"));

    verify(applicationService, times(1)).getCaseStatus(anyString());
    verify(session, times(0)).removeAttribute(SUBMISSION_TRANSACTION_ID);
  }

  @Test
  @DisplayName("Test clientUpdateSubmitted - Removes active case and redirects to home")
  void clientUpdateSubmitted() throws Exception {
    mockMvc.perform(post("/submissions/case-create/confirmed")
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"));
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count within limit")
  void viewIncludingPollCountWithinLimit() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(1);
    when(submissionConstants.getMaxPollCount()).thenReturn(5);

    final String view = caseSubmissionController.viewIncludingPollCount(session);

    assertEquals("submissions/submissionInProgress", view);
    verify(session, times(1)).setAttribute(SUBMISSION_POLL_COUNT, 2);
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count exceeded")
  void viewIncludingPollCountExceededLimit() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(5);
    when(submissionConstants.getMaxPollCount()).thenReturn(5);

    final String view = caseSubmissionController.viewIncludingPollCount(session);

    assertEquals("redirect:/submissions/%s/failed".formatted(SUBMISSION_CREATE_CASE), view);
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count starts from zero")
  void viewIncludingPollCountStartFromZero() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(null);

    final String view = caseSubmissionController.viewIncludingPollCount(session);

    assertEquals("submissions/submissionInProgress", view);
    verify(session, times(1)).setAttribute(SUBMISSION_POLL_COUNT, 1);
  }
}
