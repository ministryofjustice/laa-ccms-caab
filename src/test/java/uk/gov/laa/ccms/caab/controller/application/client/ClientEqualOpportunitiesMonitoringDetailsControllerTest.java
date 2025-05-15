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
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import uk.gov.laa.ccms.caab.controller.client.ClientEqualOpportunitiesMonitoringDetailsController;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;


@ExtendWith(MockitoExtension.class)
public class ClientEqualOpportunitiesMonitoringDetailsControllerTest {

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  @InjectMocks
  private ClientEqualOpportunitiesMonitoringDetailsController controller;

  private MockMvc mockMvc;

  private CommonLookupDetail ethnicityLookupDetail;
  private CommonLookupDetail disabilityLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataMonitoringDetails monitoringDetails;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);

    monitoringDetails = new ClientFormDataMonitoringDetails();

    ethnicityLookupDetail = new CommonLookupDetail();
    ethnicityLookupDetail.addContentItem(new CommonLookupValueDetail());
    disabilityLookupDetail = new CommonLookupDetail();
    disabilityLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  public void testClientEqualOpportunitiesMonitoringGet() throws Exception {
    when(lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN)).thenReturn(
        Mono.just(ethnicityLookupDetail));

    when(lookupService.getCommonValues(COMMON_VALUE_DISABILITY)).thenReturn(
        Mono.just(disabilityLookupDetail));

    mockMvc.perform(get("/application/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/equal-opportunities-monitoring-client-details"))
        .andExpect(model().attributeExists("ethnicOrigins", "disabilities"));
  }

  @Test
  public void testClientEqualOpportunitiesMonitoringPostValidationError() throws Exception {

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("ethnicOrigin", "required.ethnicOrigin", "Please complete 'Ethnic monitoring'.");
      return null;
    }).when(validator).validate(any(), any());

    when(lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN)).thenReturn(
        Mono.just(ethnicityLookupDetail));

    when(lookupService.getCommonValues(COMMON_VALUE_DISABILITY)).thenReturn(
        Mono.just(disabilityLookupDetail));

    mockMvc.perform(post("/application/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/equal-opportunities-monitoring-client-details"))
        .andExpect(model().attributeExists("ethnicOrigins", "disabilities"));
  }

  @Test
  public void testClientEqualOpportunitiesMonitoringPost() throws Exception {
    monitoringDetails.setDisability("TEST");
    monitoringDetails.setEthnicOrigin("TEST");

    mockMvc.perform(post("/application/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/summary"));
  }

  @Test
  @DisplayName("Client Equal Opportunities Monitoring NoValidationErrors MaxLengthsNotExceeded if characters <= 2000")
  void clientEqualOpportunitiesMonitoringPostNoValidationErrorsMaxLengthsNotExceeded()
      throws Exception {
    monitoringDetails.setSpecialConsiderations(RandomStringUtils.insecure().nextAlphabetic(2000));

    mockMvc.perform(post("/application/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/summary"));
  }

  @Test
  @DisplayName("Client Equal Opportunities Monitoring Validation Errors MaxLengthsExceeded if characters > 2000")
  public void clientEqualOpportunitiesMonitoringPostValidationErrorsMaxLengthsExceeded() throws Exception {
    monitoringDetails.setSpecialConsiderations(RandomStringUtils.insecure().nextAlphabetic(2001));

    when(lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN)).thenReturn(
        Mono.just(ethnicityLookupDetail));

    when(lookupService.getCommonValues(COMMON_VALUE_DISABILITY)).thenReturn(
        Mono.just(disabilityLookupDetail));

    mockMvc.perform(post("/application/client/details/equal-opportunities-monitoring")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("monitoringDetails", monitoringDetails))
        .andDo(print())
        .andExpect(model().attributeHasFieldErrors("monitoringDetails", "specialConsiderations"))
        .andExpect(view().name("application/client/equal-opportunities-monitoring-client-details"))
        .andExpect(model().attributeExists("ethnicOrigins", "disabilities"));
  }
}
