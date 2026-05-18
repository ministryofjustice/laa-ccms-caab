package uk.gov.laa.ccms.caab.controller.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class CaseSubmissionControllerTest {

  private MockMvc mockMvc;

  @Mock private ApplicationService applicationService;

  @Mock private SubmissionConstants submissionConstants;

  @InjectMocks private CaseSubmissionController caseSubmissionController;

  private ClientSubmissionsInProgressController clientSubmissionsInProgressController;

  @Mock private HttpSession session;

  private ActiveCase activeCase;

  private static final UserDetail userDetail = buildUserDetail();

  @Mock private Model model;

  @Mock private ClientService clientService;

  @BeforeEach
  void setUp() {
    clientSubmissionsInProgressController =
        new ClientSubmissionsInProgressController(submissionConstants, clientService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                caseSubmissionController, clientSubmissionsInProgressController)
            .setConversionService(getConversionService())
            .setControllerAdvice(new ActiveCaseModelAdvice())
            .build();

    activeCase =
        ActiveCase.builder()
            .providerId(1)
            .applicationId(1)
            .caseReferenceNumber("abc123")
            .clientReferenceNumber("xyz123")
            .build();
  }

  @Test
  @DisplayName("Test addCaseSubmission - Case confirmed")
  void testAddCaseSubmission_CaseConfirmed() throws Exception {

    final TransactionStatus mockStatus = new TransactionStatus();
    mockStatus.setReferenceNumber("ref123");
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    mockMvc
        .perform(
            get("/application/submit-case")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
                .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/%s/confirmed".formatted(SUBMISSION_SUBMIT_CASE)));

    verify(applicationService, times(1)).getCaseStatus(anyString());
  }

  @Test
  @DisplayName("Test addCaseSubmission - Amendment confirmed")
  void testAddCaseSubmission_AmendmentConfirmed() throws Exception {
    final String refNumber = "ref123";
    final TransactionStatus mockStatus = new TransactionStatus();
    mockStatus.setReferenceNumber(refNumber);
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    final ApplicationDetail mockCase = new ApplicationDetail();
    mockCase.setCaseReferenceNumber(refNumber);
    when(applicationService.getCase(anyString(), anyLong(), anyString())).thenReturn(mockCase);

    mockMvc
        .perform(
            get("/amendments/submit-case")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
                .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/%s/confirmed".formatted(SUBMISSION_SUBMIT_CASE)))
        .andExpect(request().sessionAttribute(CASE, mockCase))
        .andExpect(request().sessionAttribute(CASE_REFERENCE_NUMBER, refNumber))
        .andExpect(request().sessionAttributeDoesNotExist(ACTIVE_CASE))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_SUMMARY))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_DETAILS))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_COSTS))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_FORM_DATA));

    verify(applicationService, times(1)).getCaseStatus(anyString());
    verify(applicationService, times(1)).getCase(anyString(), anyLong(), anyString());
  }

  @Test
  @DisplayName("Test addCaseSubmission - Case not confirmed, poll continues")
  void testAddCaseSubmission_CaseNotConfirmed() throws Exception {
    final TransactionStatus mockStatus = new TransactionStatus(); // No reference number set
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    mockMvc
        .perform(
            get("/application/submit-case")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
                .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"))
        .andExpect(model().attribute("caseContext", CaseContext.APPLICATION));

    verify(applicationService, times(1)).getCaseStatus(anyString());
    verify(session, times(0)).removeAttribute(SUBMISSION_TRANSACTION_ID);
  }

  @Test
  @DisplayName("Test addCaseSubmission - Amendment not confirmed, poll continues")
  void testAddCaseSubmission_AmendmentNotConfirmed() throws Exception {
    final TransactionStatus mockStatus = new TransactionStatus(); // No reference number set
    when(applicationService.getCaseStatus(anyString())).thenReturn(Mono.just(mockStatus));

    mockMvc
        .perform(
            get("/amendments/submit-case")
                .sessionAttr(SUBMISSION_TRANSACTION_ID, "transaction123")
                .sessionAttr(USER_DETAILS, userDetail)
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionInProgress"))
        .andExpect(model().attribute("caseContext", CaseContext.AMENDMENTS))
        .andExpect(model().attribute(ACTIVE_CASE, activeCase));

    verify(applicationService, times(1)).getCaseStatus(anyString());
    verify(session, times(0)).removeAttribute(SUBMISSION_TRANSACTION_ID);
  }

  @Test
  @DisplayName("Test clientUpdateSubmitted - Removes session attributes and redirects to home")
  void testClientUpdateSubmitted() throws Exception {
    mockMvc
        .perform(post("/application/submit-case/confirmed").sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"))
        .andExpect(
            request()
                .sessionAttributeDoesNotExist(
                    APPLICATION,
                    APPLICATION_DETAILS,
                    APPLICATION_SUMMARY,
                    APPLICATION_COSTS,
                    APPLICATION_FORM_DATA,
                    CASE,
                    ACTIVE_CASE,
                    APPLICATION_ID));
  }

  @Test
  @DisplayName(
      "Test clientUpdateSubmitted - Removes session attributes and redirects to case overview")
  void testCaseSubmittedRedirectToCaseOverview() throws Exception {
    mockMvc
        .perform(post("/amendments/submit-case/confirmed").sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/case/overview"))
        .andExpect(
            request()
                .sessionAttributeDoesNotExist(
                    APPLICATION,
                    APPLICATION_DETAILS,
                    APPLICATION_SUMMARY,
                    APPLICATION_COSTS,
                    APPLICATION_FORM_DATA,
                    ACTIVE_CASE,
                    CASE));
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count within limit")
  void testViewIncludingPollCount_WithinLimit() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(1);
    when(submissionConstants.getMaxPollCount()).thenReturn(5);

    final String view =
        caseSubmissionController.viewIncludingPollCount(session, CaseContext.APPLICATION, model);

    assertEquals("submissions/submissionInProgress", view);
    verify(session, times(1)).setAttribute(SUBMISSION_POLL_COUNT, 2);
    verify(model, times(1)).addAttribute("caseContext", CaseContext.APPLICATION);
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count exceeded")
  void testViewIncludingPollCount_ExceededLimit() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(5);
    when(submissionConstants.getMaxPollCount()).thenReturn(5);

    final String view =
        caseSubmissionController.viewIncludingPollCount(session, CaseContext.APPLICATION, model);

    assertEquals("redirect:/application/%s/failed".formatted(SUBMISSION_SUBMIT_CASE), view);
  }

  @Test
  @DisplayName("Test viewIncludingPollCount - Poll count starts from zero")
  void testViewIncludingPollCount_StartFromZero() {
    when(session.getAttribute(SUBMISSION_POLL_COUNT)).thenReturn(null);

    final String view =
        caseSubmissionController.viewIncludingPollCount(session, CaseContext.APPLICATION, model);

    assertEquals("submissions/submissionInProgress", view);
    verify(session, times(1)).setAttribute(SUBMISSION_POLL_COUNT, 1);
    verify(model, times(1)).addAttribute("caseContext", CaseContext.APPLICATION);
  }

  @Test
  @DisplayName("Test submissionFailed - Application context - Submit Case")
  void testSubmissionFailed_Application_SubmitCase() throws Exception {
    mockMvc
        .perform(post("/application/submit-case/failed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/case-details"));
  }

  @Test
  @DisplayName("Test submissionFailed - Amendment context")
  void testSubmissionFailed_Amendment() throws Exception {
    mockMvc
        .perform(post("/amendments/submit-case/failed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/case/overview"));
  }

  @Test
  @DisplayName("Test submissionsFailed GET")
  void testSubmissionsFailed() throws Exception {
    mockMvc
        .perform(get("/application/submit-case/failed"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionFailed"));
  }
}
