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
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class ClientAddressDetailsControllerTest {

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock
  private FindAddressValidator findAddressValidator;

  @Mock
  private AddressService addressService;

  @InjectMocks
  private ClientAddressDetailsController clientAddressDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail countryLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataAddressDetails addressDetails;

  private ClientFormDataBasicDetails basicDetails;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientAddressDetailsController).build();

    basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);
    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());

    addressDetails = new ClientFormDataAddressDetails();

    countryLookupDetail = new CommonLookupDetail();
    countryLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testClientDetailsAddress() throws Exception {
    when(lookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(get("/application/client/details/address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostFindAddress_NoAddresses() throws Exception {
    when(addressService.getAddresses(any())).thenReturn(
        new ResultsDisplay<AddressResultRowDisplay>());

    when(lookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"));
  }

  @Test
  void testClientDetailsAddressPostFindAddress_WithAddresses() throws Exception {
    ResultsDisplay<AddressResultRowDisplay> addressResults = new ResultsDisplay<AddressResultRowDisplay>();
    addressResults.setContent(new ArrayList<>());
    addressResults.getContent().add(new AddressResultRowDisplay());

    when(addressService.getAddresses(any())).thenReturn(
        addressResults);

    when(addressService.filterByHouseNumber(any(), any())).thenReturn(
        addressResults);

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address/search"));
  }

  @Test
  void testClientDetailsAddressPostNext() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "next")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));
  }

  @Test
  void testClientDetailsAddressPostValidationError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(clientAddressDetailsValidator).validate(any(), any());

    when(lookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "next")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostSearchError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(findAddressValidator).validate(any(), any());

    when(lookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "find_address")
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

}