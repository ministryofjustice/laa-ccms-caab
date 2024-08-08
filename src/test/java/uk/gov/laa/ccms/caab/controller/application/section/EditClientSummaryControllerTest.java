package uk.gov.laa.ccms.caab.controller.application.section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
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
import uk.gov.laa.ccms.caab.bean.ActiveCase;
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
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

@ExtendWith(MockitoExtension.class)
class EditClientSummaryControllerTest {

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
  private EditClientSummaryController editClientSummaryController;

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

  private ActiveCase activeCase;

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId");

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(editClientSummaryController).build();

    clientFlowFormData = new ClientFlowFormData("edit");
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

    activeCase = ActiveCase.builder().build();
  }

  @Test
  @DisplayName("Test get client details summary with form data in session")
  void testGetClientDetailsSummary_withFormDataInSession() throws Exception {

    when(lookupService.getClientLookups(any())).thenReturn(
        List.of(
            Pair.of(COMMON_VALUE_CONTACT_TITLE, Mono.just(Optional.of(titleLookupValueDetail))),
            Pair.of(COMMON_VALUE_GENDER, Mono.just(Optional.of(genderLookupValueDetail))),
            Pair.of(COMMON_VALUE_MARITAL_STATUS, Mono.just(Optional.of(maritalStatusLookupValueDetail))),
            Pair.of(COMMON_VALUE_ETHNIC_ORIGIN, Mono.just(Optional.of(ethnicityLookupValueDetail))),
            Pair.of(COMMON_VALUE_DISABILITY, Mono.just(Optional.of(disabilityLookupValueDetail))),
            Pair.of(COMMON_VALUE_CORRESPONDENCE_METHOD, Mono.just(Optional.of(correspondenceMethodLookupValueDetail)))
        )
    );

    when(lookupService.addCommonLookupsToModel(anyList(), any(Model.class)))
        .thenReturn(Mono.just(List.of(
            titleLookupValueDetail,
            genderLookupValueDetail,
            maritalStatusLookupValueDetail,
            ethnicityLookupValueDetail,
            disabilityLookupValueDetail,
            correspondenceMethodLookupValueDetail
        )));

    mockMvc.perform(get("/application/sections/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .sessionAttr(ACTIVE_CASE, activeCase)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/client-summary-details"));

    verify(lookupService, atLeastOnce()).getClientLookups(any());
    verify(lookupService, atLeastOnce()).addCommonLookupsToModel(anyList(), any(Model.class));
    verify(lookupService, never()).getCommonValue(eq(COMMON_VALUE_CORRESPONDENCE_LANGUAGE), any());

    verify(clientService, never()).getClient(any(), any(), any());
    verify(clientDetailsMapper, never()).toClientFlowFormData(any());
  }


  @Test
  @DisplayName("Test get client details summary without form data in session")
  void testGetClientDetailsSummary_withoutFormDataInSession() throws Exception {

    when(lookupService.getClientLookups(any())).thenReturn(
        List.of(
            Pair.of(COMMON_VALUE_CONTACT_TITLE, Mono.just(Optional.of(titleLookupValueDetail))),
            Pair.of(COMMON_VALUE_GENDER, Mono.just(Optional.of(genderLookupValueDetail))),
            Pair.of(COMMON_VALUE_MARITAL_STATUS, Mono.just(Optional.of(maritalStatusLookupValueDetail))),
            Pair.of(COMMON_VALUE_ETHNIC_ORIGIN, Mono.just(Optional.of(ethnicityLookupValueDetail))),
            Pair.of(COMMON_VALUE_DISABILITY, Mono.just(Optional.of(disabilityLookupValueDetail))),
            Pair.of(COMMON_VALUE_CORRESPONDENCE_METHOD, Mono.just(Optional.of(correspondenceMethodLookupValueDetail)))
        )
    );

    when(lookupService.addCommonLookupsToModel(anyList(), any(Model.class)))
        .thenReturn(Mono.just(List.of(
            titleLookupValueDetail,
            genderLookupValueDetail,
            maritalStatusLookupValueDetail,
            ethnicityLookupValueDetail,
            disabilityLookupValueDetail,
            correspondenceMethodLookupValueDetail
        )));

    when(clientService.getClient(any(), any(), any())).thenReturn(Mono.just(new ClientDetail()));

    when(clientDetailsMapper.toClientFlowFormData(any())).thenReturn(clientFlowFormData);

    mockMvc.perform(get("/application/sections/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/client-summary-details"));

    verify(lookupService, atLeastOnce()).getClientLookups(any());
    verify(lookupService, atLeastOnce()).addCommonLookupsToModel(anyList(), any(Model.class));
    verify(lookupService, never()).getCommonValue(eq(COMMON_VALUE_CORRESPONDENCE_LANGUAGE), any());

    verify(clientService, atLeastOnce()).getClient(any(), any(), any());
    verify(clientDetailsMapper, atLeastOnce()).toClientFlowFormData(any());
  }

  @Test
  void testPostClientDetailsSummary() throws Exception {
    final ClientFlowFormData clientFlowFormData = new ClientFlowFormData("edit");

    when(clientService.updateClient(any(), any(), any())).thenReturn(
        Mono.just(new ClientTransactionResponse()));

    mockMvc.perform(post("/application/sections/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .sessionAttr(ACTIVE_CASE, activeCase)
            .flashAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/client-update"));

    verify(basicValidator).validate(any(), any());
    verify(contactValidator).validate(any(), any());
    verify(addressValidator).validate(any(), any());
    verify(opportunitiesValidator).validate(any(), any());

    verify(clientService).updateClient(any(), any(), any());
  }

}