package uk.gov.laa.ccms.caab.controller.application.summary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.controller.application.client.ClientContactDetailsController;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class EditClientContactDetailsControllerTest {

  @Mock
  private CommonLookupService commonLookupService;

  @Mock
  private ClientContactDetailsValidator clientContactDetailsValidator;

  @InjectMocks
  private EditClientContactDetailsController editClientContactDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail correspondenceMethodLookupDetail;
  private CommonLookupDetail correspondenceLanguageLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataContactDetails contactDetails;

  private ClientFormDataBasicDetails basicDetails;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(editClientContactDetailsController).build();

    basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);

    contactDetails = new ClientFormDataContactDetails();
    
    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    clientFlowFormData.setBasicDetails(basicDetails);
    clientFlowFormData.setContactDetails(contactDetails);
    
    correspondenceMethodLookupDetail = new CommonLookupDetail();
    correspondenceMethodLookupDetail.addContentItem(new CommonLookupValueDetail());
    correspondenceLanguageLookupDetail = new CommonLookupDetail();
    correspondenceLanguageLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testClientDetailsContact() throws Exception {

    when(commonLookupService.getCorrespondenceMethods()).thenReturn(
        Mono.just(correspondenceMethodLookupDetail));
    when(commonLookupService.getCorrespondenceLanguages()).thenReturn(
        Mono.just(correspondenceLanguageLookupDetail));

    this.mockMvc.perform(get("/application/summary/client/details/contact")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("contactDetails", contactDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-contact-details"))
        .andExpect(model().attributeExists("correspondenceMethods", "correspondenceLanguages"));

  }
  @Test
  void testClientDetailsContactPost() throws Exception {

    mockMvc.perform(post("/application/summary/client/details/contact")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("contactDetails", contactDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary/client/details/summary"));
  }


  @Test
  void testClientDetailsContactPostValidationError() throws Exception {
    contactDetails.setTelephoneHome("0123456789");
    contactDetails.setPasswordReminder("test");
    contactDetails.setCorrespondenceMethod("test");

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("password", "required.password", "Please complete 'Password'.");
      return null;
    }).when(clientContactDetailsValidator).validate(any(), any());

    when(commonLookupService.getCorrespondenceMethods()).thenReturn(
        Mono.just(correspondenceMethodLookupDetail));
    when(commonLookupService.getCorrespondenceLanguages()).thenReturn(
        Mono.just(correspondenceLanguageLookupDetail));

    this.mockMvc.perform(post("/application/summary/client/details/contact")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("contactDetails", contactDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-contact-details"))
        .andExpect(model().attributeExists("correspondenceMethods", "correspondenceLanguages"));
  }
}
