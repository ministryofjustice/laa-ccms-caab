package uk.gov.laa.ccms.caab.controller.application.client;

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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class ClientContactDetailsControllerTest {

  @Mock
  private CommonLookupService commonLookupService;

  @Mock
  private ClientContactDetailsValidator clientContactDetailsValidator;

  @InjectMocks
  private ClientContactDetailsController clientContactDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail correspondenceMethodLookupDetail;
  private CommonLookupDetail correspondenceLanguageLookupDetail;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientContactDetailsController).build();

    correspondenceMethodLookupDetail = new CommonLookupDetail();
    correspondenceMethodLookupDetail.addContentItem(new CommonLookupValueDetail());
    correspondenceLanguageLookupDetail = new CommonLookupDetail();
    correspondenceLanguageLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testClientDetailsContact() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    when(commonLookupService.getCorrespondenceMethods()).thenReturn(
        Mono.just(correspondenceMethodLookupDetail));
    when(commonLookupService.getCorrespondenceLanguagess()).thenReturn(
        Mono.just(correspondenceLanguageLookupDetail));

    this.mockMvc.perform(get("/application/client/details/contact")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/contact-client-details"))
        .andExpect(model().attributeExists("correspondenceMethods", "correspondenceLanguages"));

  }
  @Test
  void testClientDetailsContactPost() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    mockMvc.perform(post("/application/client/details/contact")
            .sessionAttr(CLIENT_DETAILS, clientDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }


  @Test
  void testClientDetailsContactPostValidationError() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTelephoneHome("0123456789");
    clientDetails.setPasswordReminder("test");
    clientDetails.setCorrespondenceMethod("test");

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("password", "required.password", "Please complete 'Password'.");
      return null;
    }).when(clientContactDetailsValidator).validate(any(), any());

    when(commonLookupService.getCorrespondenceMethods()).thenReturn(
        Mono.just(correspondenceMethodLookupDetail));
    when(commonLookupService.getCorrespondenceLanguagess()).thenReturn(
        Mono.just(correspondenceLanguageLookupDetail));

    this.mockMvc.perform(post("/application/client/details/contact")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/contact-client-details"))
        .andExpect(model().attributeExists("correspondenceMethods", "correspondenceLanguages"));
  }
}
