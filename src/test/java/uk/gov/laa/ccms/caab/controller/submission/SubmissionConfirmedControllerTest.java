package uk.gov.laa.ccms.caab.controller.submission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
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

  @InjectMocks private SubmissionConfirmedController submissionConfirmedController;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(submissionConfirmedController)
            .setConversionService(getConversionService())
            .build();
  }

  @Test
  void testSubmissionsConfirmedWhenSubmissionResultMissingRedirects() throws Exception {
    mockMvc
        .perform(get("/application/testType/confirmed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/application/sections"));
  }

  @Test
  void testSubmissionsConfirmedWhenSubmissionResultPresent() throws Exception {
    mockMvc
        .perform(get("/application/testType/confirmed").sessionAttr(SUBMISSION_RESULT, "confirmed"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionConfirmed"));
  }

  @Test
  void testSubmissionsConfirmedForAmendmentsWhenSubmissionResultMissing() throws Exception {
    mockMvc
        .perform(get("/amendments/submit-case/confirmed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/case/overview"));
  }

  @Test
  void testSubmissionsConfirmedWhenSubmissionResultMissingWithCaseProviderRequestSubmissionType()
      throws Exception {
    mockMvc
        .perform(get("/application/submit-case-provider-request/confirmed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/case/overview"));
  }

  @Test
  void testSubmissionsConfirmedWhenSubmissionResultMissingWithSubmitCaseSubmissionType()
      throws Exception {
    mockMvc
        .perform(get("/application/submit-case/confirmed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/home"));
  }

  @Test
  void testSubmissionsConfirmedWhenSubmissionResultMissingWithGeneralProviderRequestSubmissionType()
      throws Exception {
    mockMvc
        .perform(get("/application/submit-general-provider-request/confirmed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/home"));
  }

  @Test
  void testGeneralProviderRequestConfirmedWithMatchingSubmissionId() throws Exception {
    mockMvc
        .perform(
            get("/application/submit-general-provider-request/confirmed")
                .param("submissionId", "request-123")
                .sessionAttr(SUBMISSION_RESULT, "confirmed")
                .sessionAttr(GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID, "request-123"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionConfirmed"))
        .andExpect(model().attribute("submissionId", "request-123"));
  }

  @Test
  void testGeneralProviderRequestConfirmedWithoutSubmissionIdWhenSubmissionActive()
      throws Exception {
    mockMvc
        .perform(
            get("/application/submit-general-provider-request/confirmed")
                .sessionAttr(SUBMISSION_RESULT, "confirmed")
                .sessionAttr(GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID, "request-123"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionConfirmed"));
  }

  @Test
  void testGeneralProviderRequestConfirmedWithMismatchedSubmissionIdRedirects() throws Exception {
    mockMvc
        .perform(
            get("/application/submit-general-provider-request/confirmed")
                .param("submissionId", "request-123")
                .sessionAttr(SUBMISSION_RESULT, "confirmed")
                .sessionAttr(GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID, "request-999"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/alreadySubmitted?returnUrl=/home"));
  }

  @Test
  void testAlreadySubmittedSanitizesExternalReturnUrl() throws Exception {
    mockMvc
        .perform(get("/submissions/alreadySubmitted").param("returnUrl", "https://example.com"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/alreadySubmitted"))
        .andExpect(model().attribute("returnUrl", "/case/overview"))
        .andExpect(model().attribute("returnDestination", "CASE_OVERVIEW"));
  }

  @Test
  void testAlreadySubmittedKeepsInternalReturnUrl() throws Exception {
    mockMvc
        .perform(get("/submissions/alreadySubmitted").param("returnUrl", "/home"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/alreadySubmitted"))
        .andExpect(model().attribute("returnUrl", "/home"))
        .andExpect(model().attribute("returnDestination", "HOME"));
  }

  @Test
  void testAlreadySubmittedKeepsApplicationSectionsReturnUrl() throws Exception {
    mockMvc
        .perform(get("/submissions/alreadySubmitted").param("returnUrl", "/application/sections"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/alreadySubmitted"))
        .andExpect(model().attribute("returnUrl", "/application/sections"))
        .andExpect(model().attribute("returnDestination", "APPLICATION_SECTIONS"));
  }
}
