package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.PrivacyNoticeAgreementValidator;
import uk.gov.laa.ccms.caab.constants.SessionConstants;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class PrivacyNoticeAgreementControllerTest {

  @Mock private PrivacyNoticeAgreementValidator privacyNoticeAgreementValidator;

  @InjectMocks private PrivacyNoticeAgreementController privacyNoticeAgreementController;

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(privacyNoticeAgreementController).build();
  }

  @Test
  public void testGetPrivacyNoticeAgreement() throws Exception {
    ApplicationFormData applicationFormData = new ApplicationFormData();
    this.mockMvc
        .perform(
            get("/application/agreement")
                .sessionAttr(SessionConstants.APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/privacy-notice-agreement"))
        .andExpect(model().attribute(SessionConstants.APPLICATION_FORM_DATA, applicationFormData));
  }

  @Test
  public void testPostPrivacyNoticeAgreement_ValidationSuccess_NewClient() throws Exception {
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationCreated(false);
    applicationFormData.setAgreementAccepted(true);

    this.mockMvc
        .perform(
            post("/application/agreement")
                .sessionAttr(SessionConstants.APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/basic"));

    verify(privacyNoticeAgreementValidator, times(1)).validate(any(), any());
  }

  @Test
  public void testPostPrivacyNoticeAgreement_ValidationSuccess_ExistingClient() throws Exception {
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationCreated(true);
    applicationFormData.setAgreementAccepted(true);

    this.mockMvc
        .perform(
            post("/application/agreement")
                .sessionAttr(SessionConstants.APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections"));

    verify(privacyNoticeAgreementValidator, times(1)).validate(any(), any());
  }

  @Test
  public void testPostPrivacyNoticeAgreement_ValidationError() throws Exception {
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setAgreementAccepted(false);

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue(
                  null,
                  "agreement.not.accepted",
                  "Please complete 'I confirm my client (or their representative) has read and agreed to the Privacy Notice'.");
              return null;
            })
        .when(privacyNoticeAgreementValidator)
        .validate(any(), any());

    this.mockMvc
        .perform(
            post("/application/agreement")
                .sessionAttr(SessionConstants.APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/privacy-notice-agreement"));

    verify(privacyNoticeAgreementValidator, times(1)).validate(any(), any());
  }

  @Test
  public void testGetPrivacyNoticeAgreementPrintable() throws Exception {
    this.mockMvc
        .perform(get("/application/agreement/print"))
        .andExpect(status().isOk())
        .andExpect(view().name("application/privacy-notice-agreement-printable"));
  }
}
