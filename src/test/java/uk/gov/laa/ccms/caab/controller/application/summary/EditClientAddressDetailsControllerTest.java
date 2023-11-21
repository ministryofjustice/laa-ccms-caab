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

import java.util.ArrayList;
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
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsFindAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.model.ClientAddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
class EditClientAddressDetailsControllerTest {

  @Mock
  private CommonLookupService commonLookupService;

  @Mock
  private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock
  private ClientAddressDetailsFindAddressValidator clientAddressDetailsFindAddressValidator;

  @Mock
  private AddressService addressService;

  @InjectMocks
  private EditClientAddressDetailsController editClientAddressDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail countryLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataAddressDetails addressDetails;

  private ClientFormDataBasicDetails basicDetails;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(editClientAddressDetailsController).build();

    basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);

    addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setVulnerableClient(false);

    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());
    clientFlowFormData.setAddressDetails(addressDetails);

    countryLookupDetail = new CommonLookupDetail();
    countryLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testEditClientDetailsAddress() throws Exception {
    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(get("/application/summary/client/details/address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-address-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testEditClientDetailsAddressPostFindAddress_NoAddresses() throws Exception {
    when(addressService.getAddresses(any())).thenReturn(
        new ClientAddressResultsDisplay());

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/summary/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-address-details"));
  }

  @Test
  void testEditClientDetailsAddressPostFindAddress_WithAddresses() throws Exception {
    ClientAddressResultsDisplay addressResults = new ClientAddressResultsDisplay();
    addressResults.setContent(new ArrayList<>());
    addressResults.getContent().add(new ClientAddressResultRowDisplay());

    when(addressService.getAddresses(any())).thenReturn(
        addressResults);

    when(addressService.filterByHouseNumber(any(), any())).thenReturn(
        addressResults);

    this.mockMvc.perform(post("/application/summary/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary/client/details/address/search"));
  }

  @Test
  void testEditClientDetailsAddressPostNext() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    this.mockMvc.perform(post("/application/summary/client/details/address")
            .param("action", "next")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary/client/details/summary"));
  }

  @Test
  void testEditClientDetailsAddressPostValidationError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(clientAddressDetailsValidator).validate(any(), any());

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/summary/client/details/address")
            .param("action", "next")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-address-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testEditClientDetailsAddressPostSearchError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(clientAddressDetailsFindAddressValidator).validate(any(), any());

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/summary/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/client-address-details"))
        .andExpect(model().attributeExists("countries"));
  }

}