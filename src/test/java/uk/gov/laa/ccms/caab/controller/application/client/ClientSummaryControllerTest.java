package uk.gov.laa.ccms.caab.controller.application.client;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

@ExtendWith(MockitoExtension.class)
public class ClientSummaryControllerTest {

  @Mock
  private ClientService clientService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientBasicDetailsValidator basicValidator;

  @Mock
  private ClientContactDetailsValidator contactValidator;

  @Mock
  private ClientAddressDetailsValidator addressValidator;

  @Mock
  private ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator;

  @Mock
  private ClientDetailMapper clientDetailsMapper;

  @InjectMocks
  private ClientSummaryController clientSummaryController;

  private MockMvc mockMvc;

  private CommonLookupValueDetail titleLookupValueDetail;
  private CommonLookupValueDetail countryLookupValueDetail;
  private CommonLookupValueDetail genderLookupValueDetail;
  private CommonLookupValueDetail maritalStatusLookupValueDetail;
  private CommonLookupValueDetail ethnicityLookupValueDetail;
  private CommonLookupValueDetail disabilityLookupValueDetail;
  private CommonLookupValueDetail correspondenceMethodLookupValueDetail;
  private CommonLookupValueDetail correspondenceLanguageLookupValueDetail;

  private ClientFlowFormData clientFlowFormData;

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId");

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(clientSummaryController).build();

    clientFlowFormData = new ClientFlowFormData("create");
    clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());
    clientFlowFormData.setContactDetails(new ClientFormDataContactDetails());
    clientFlowFormData.setAddressDetails(new ClientFormDataAddressDetails());
    clientFlowFormData.setMonitoringDetails(new ClientFormDataMonitoringDetails());

    titleLookupValueDetail = new CommonLookupValueDetail();
    countryLookupValueDetail = new CommonLookupValueDetail();
    genderLookupValueDetail = new CommonLookupValueDetail();
    maritalStatusLookupValueDetail = new CommonLookupValueDetail();
    ethnicityLookupValueDetail = new CommonLookupValueDetail();
    disabilityLookupValueDetail = new CommonLookupValueDetail();
    correspondenceMethodLookupValueDetail = new CommonLookupValueDetail();
    correspondenceLanguageLookupValueDetail = new CommonLookupValueDetail();
  }

  @Test
  void testClientDetailsSummary_Get() throws Exception {

    when(lookupService.getContactTitle(any())).thenReturn(
        Mono.just(titleLookupValueDetail));
    when(lookupService.getCountry(any())).thenReturn(
        Mono.just(countryLookupValueDetail));
    when(lookupService.getGender(any())).thenReturn(
        Mono.just(genderLookupValueDetail));
    when(lookupService.getMaritalStatus(any())).thenReturn(
        Mono.just(maritalStatusLookupValueDetail));
    when(lookupService.getEthnicOrigin(any())).thenReturn(
        Mono.just(ethnicityLookupValueDetail));
    when(lookupService.getDisability(any())).thenReturn(
        Mono.just(disabilityLookupValueDetail));
    when(lookupService.getCorrespondenceMethod(any())).thenReturn(
        Mono.just(correspondenceMethodLookupValueDetail));

    mockMvc.perform(get("/application/client/details/summary")
            .flashAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/client-summary-details"));

    verify(lookupService, atLeastOnce()).getContactTitle(any());
    verify(lookupService, atLeastOnce()).getCountry(any());
    verify(lookupService, atLeastOnce()).getGender(any());
    verify(lookupService, atLeastOnce()).getMaritalStatus(any());
    verify(lookupService, atLeastOnce()).getEthnicOrigin(any());
    verify(lookupService, atLeastOnce()).getDisability(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceMethod(any());
    verify(lookupService, never()).getCorrespondenceLanguage(any());
  }

  @Test
  void testClientDetailsSummary_Get_withCorrespondenceLanguage() throws Exception {
    clientFlowFormData.getContactDetails().setCorrespondenceLanguage("TEST");

    when(lookupService.getContactTitle(any())).thenReturn(
        Mono.just(titleLookupValueDetail));
    when(lookupService.getCountry(any())).thenReturn(
        Mono.just(countryLookupValueDetail));
    when(lookupService.getGender(any())).thenReturn(
        Mono.just(genderLookupValueDetail));
    when(lookupService.getMaritalStatus(any())).thenReturn(
        Mono.just(maritalStatusLookupValueDetail));
    when(lookupService.getEthnicOrigin(any())).thenReturn(
        Mono.just(ethnicityLookupValueDetail));
    when(lookupService.getDisability(any())).thenReturn(
        Mono.just(disabilityLookupValueDetail));
    when(lookupService.getCorrespondenceMethod(any())).thenReturn(
        Mono.just(correspondenceMethodLookupValueDetail));

    when(lookupService.getCorrespondenceLanguage(any())).thenReturn(
        Mono.just(correspondenceLanguageLookupValueDetail));

    mockMvc.perform(get("/application/client/details/summary")
            .flashAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/client-summary-details"));

    verify(lookupService, atLeastOnce()).getContactTitle(any());
    verify(lookupService, atLeastOnce()).getCountry(any());
    verify(lookupService, atLeastOnce()).getGender(any());
    verify(lookupService, atLeastOnce()).getMaritalStatus(any());
    verify(lookupService, atLeastOnce()).getEthnicOrigin(any());
    verify(lookupService, atLeastOnce()).getDisability(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceMethod(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceLanguage(any());
  }

  @Test
  void testClientDetailsSummary_Post() throws Exception {
    ClientFlowFormData clientFlowFormData = new ClientFlowFormData("create");

    when(clientService.createClient(any(), any())).thenReturn(
        Mono.just(new ClientTransactionResponse()));

    mockMvc.perform(post("/application/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .flashAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/client-create"));

    verify(basicValidator).validate(any(), any());
    verify(contactValidator).validate(any(), any());
    verify(addressValidator).validate(any(), any());
    verify(opportunitiesValidator).validate(any(), any());

    verify(clientService).createClient(any(), any());
  }
}
