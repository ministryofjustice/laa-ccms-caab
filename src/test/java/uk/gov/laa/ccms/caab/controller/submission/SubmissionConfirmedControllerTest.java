package uk.gov.laa.ccms.caab.controller.submission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;

@ExtendWith(MockitoExtension.class)
class SubmissionConfirmedControllerTest {

  @InjectMocks private SubmissionConfirmedController submissionConfirmedController;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    // The active case advice is registered so the case context banner behaves as it does in the
    // running app: it is populated from the shared session attribute, not by this controller.
    mockMvc =
        MockMvcBuilders.standaloneSetup(submissionConfirmedController)
            .setControllerAdvice(new ActiveCaseModelAdvice())
            .setConversionService(getConversionService())
            .build();
  }

  private static final ActiveCase activeCase =
      ActiveCase.builder().caseReferenceNumber("300000123456").build();

  @Test
  void testSubmissionsConfirmed() throws Exception {
    mockMvc
        .perform(get("/application/testType/confirmed"))
        .andExpect(status().isOk())
        .andExpect(view().name("submissions/submissionConfirmed"));
  }

  @Test
  @DisplayName("A general enquiry confirmation hides a case banner left in the session")
  void testSubmissionsConfirmed_generalEnquiryHidesStaleCaseBanner() throws Exception {
    // A general enquiry is redirected here with no case reference, but the user visited a case
    // earlier in the session so ACTIVE_CASE is still set.
    mockMvc
        .perform(
            get("/application/provider-request/confirmed").sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE))
        // The shared session attribute must survive for the amendment journey.
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase));
  }

  @Test
  @DisplayName("A case query confirmation keeps the banner for the case it was raised against")
  void testSubmissionsConfirmed_caseQueryKeepsMatchingCaseBanner() throws Exception {
    mockMvc
        .perform(
            get("/application/provider-request/confirmed")
                .param("caseReferenceNumber", "300000123456")
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute(ACTIVE_CASE, activeCase));
  }

  @Test
  @DisplayName("A case query confirmation hides a banner belonging to a different case")
  void testSubmissionsConfirmed_caseQueryHidesBannerForDifferentCase() throws Exception {
    mockMvc
        .perform(
            get("/application/provider-request/confirmed")
                .param("caseReferenceNumber", "123456789012")
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE));
  }

  @Test
  @DisplayName("Other submission types keep the case banner, as they do not carry a case reference")
  void testSubmissionsConfirmed_otherSubmissionTypesKeepCaseBanner() throws Exception {
    mockMvc
        .perform(get("/application/submit-case/confirmed").sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute(ACTIVE_CASE, activeCase));
  }
}
