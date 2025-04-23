package uk.gov.laa.ccms.caab.controller.application.section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
class EditClientEqualOpportunitiesMonitoringDetailsControllerTest {

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  @InjectMocks
  private EditClientEqualOpportunitiesMonitoringDetailsController controller;

  private MockMvc mockMvc;

  private CommonLookupDetail ethnicityLookupDetail;
  private CommonLookupDetail disabilityLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataMonitoringDetails monitoringDetails;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    monitoringDetails = new ClientFormDataMonitoringDetails();

    clientFlowFormData = new ClientFlowFormData(ACTION_EDIT);
    clientFlowFormData.setMonitoringDetails(monitoringDetails);

    ethnicityLookupDetail = new CommonLookupDetail();
    ethnicityLookupDetail.addContentItem(new CommonLookupValueDetail());
    disabilityLookupDetail = new CommonLookupDetail();
    disabilityLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void editClientEqualOpportunitiesMonitoringGet() throws Exception {
    when(lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN)).thenReturn(
        Mono.just(ethnicityLookupDetail));

    when(lookupService.getCommonValues(COMMON_VALUE_DISABILITY)).thenReturn(
        Mono.just(disabilityLookupDetail));

    mockMvc.perform(get("/application/sections/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/client-equal-opportunities-monitoring"))
        .andExpect(model().attributeExists("ethnicOrigins", "disabilities"));
  }

  @Test
  void editClientEqualOpportunitiesMonitoringPostValidationError() throws Exception {

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("ethnicOrigin", "required.ethnicOrigin", "Please complete 'Ethnic monitoring'.");
      return null;
    }).when(validator).validate(any(), any());

    when(lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN)).thenReturn(
        Mono.just(ethnicityLookupDetail));

    when(lookupService.getCommonValues(COMMON_VALUE_DISABILITY)).thenReturn(
        Mono.just(disabilityLookupDetail));

    mockMvc.perform(post("/application/sections/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/client-equal-opportunities-monitoring"))
        .andExpect(model().attributeExists("ethnicOrigins", "disabilities"));
  }

  @Test
  void editClientEqualOpportunitiesMonitoringPost() throws Exception {
    monitoringDetails.setDisability("TEST");
    monitoringDetails.setEthnicOrigin("TEST");

    mockMvc.perform(post("/application/sections/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections/client/details/summary"));
  }

}
