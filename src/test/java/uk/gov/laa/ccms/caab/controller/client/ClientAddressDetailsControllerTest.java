package uk.gov.laa.ccms.caab.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

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
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
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

  @Mock private LookupService lookupService;

  @Mock private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock private FindAddressValidator findAddressValidator;

  @Mock private AddressService addressService;

  @InjectMocks private ClientAddressDetailsController clientAddressDetailsController;

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
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    this.mockMvc
        .perform(
            get("/application/client/details/address")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostFindAddress_NoAddresses() throws Exception {
    addressDetails.setPostcode("SW1A 1AA");
    when(addressService.getAddresses(anyString()))
        .thenReturn(new ResultsDisplay<AddressResultRowDisplay>());

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "find_address")
                .param("postcode", "SW1A 1AA")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address/search"));
  }

  @Test
  void testClientDetailsAddressPostFindAddress_WithAddresses() throws Exception {
    addressDetails.setPostcode("SW1A 1AA");
    ResultsDisplay<AddressResultRowDisplay> addressResults = new ResultsDisplay<>();
    addressResults.setContent(new ArrayList<>());
    addressResults.getContent().add(new AddressResultRowDisplay());

    when(addressService.getAddresses(any())).thenReturn(addressResults);

    when(addressService.filterByHouseNumber(any(), any())).thenReturn(addressResults);

    this.mockMvc
        .perform(
            post("/application/client/details/address")
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
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow = newManualAddressFlow();

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));
  }

  @Test
  void testClientDetailsAddressPostNextNormalizesManualUkPostcode() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setCountry("GBR");
    addressDetails.setPostcode("sa58ab");
    addressDetails.setAddressLine1("Address line 1");
    addressDetails.setCityTown("Town");
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow = newManualAddressFlow();

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", addressDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));

    assertEquals("SA5 8AB", clientFlowFormData.getAddressDetails().getPostcode());
  }

  @Test
  void testClientDetailsAddressPostNextPreservesSearchedPostcode() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setCountry("GBR");
    addressDetails.setPostcode("sa58ab");
    addressDetails.setAddressLine1("Address line 1");
    addressDetails.setCityTown("Town");
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>("create-client");
    addressFlow.setSource(AddressLookupFlowData.Source.SEARCHED);
    AddressResultRowDisplay selected = new AddressResultRowDisplay();
    selected.setUprn("123");
    selected.setPostcode("sa58ab");
    selected.setCountry("GBR");
    addressFlow.setSelectedAddress(selected);
    addressFlow.confirmSelectedAddress();

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", addressDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));

    assertEquals("sa58ab", clientFlowFormData.getAddressDetails().getPostcode());
  }

  @Test
  void clientCannotSavePostedAddressFieldsWithoutEnteringManualMode() throws Exception {
    ClientFormDataAddressDetails postedAddress = new ClientFormDataAddressDetails();
    postedAddress.setCountry("GBR");
    postedAddress.setPostcode("SA5 8AB");
    postedAddress.setAddressLine1("Forged address");
    postedAddress.setCityTown("Town");
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .flashAttr("addressDetails", postedAddress))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"));

    assertNull(clientFlowFormData.getAddressDetails());
  }

  @Test
  void vulnerableClientCannotStartAddressLookup() throws Exception {
    ClientFormDataBasicDetails trustedBasicDetails = new ClientFormDataBasicDetails();
    trustedBasicDetails.setVulnerableClient(true);
    clientFlowFormData.setBasicDetails(trustedBasicDetails);
    ClientFormDataAddressDetails submittedAddress = new ClientFormDataAddressDetails();
    submittedAddress.setPostcode("SA5 8AB");
    submittedAddress.setAddressLine1("Do not accept");

    mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "find_address")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .flashAttr("addressDetails", submittedAddress))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));

    assertNull(clientFlowFormData.getAddressDetails().getAddressLine1());
    org.mockito.Mockito.verifyNoInteractions(addressService);
  }

  @Test
  void confirmationAcceptsOnlyASelectionInTheCurrentFlow() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>("create-client");
    addressFlow.setSelectedAddress(new AddressResultRowDisplay());

    this.mockMvc
        .perform(
            post("/application/client/details/address/confirm")
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, addressFlow.getStep());
  }

  @Test
  void clientNextFromUnconfirmedSelectionReturnsToConfirmation() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>("create-client");
    AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
    selectedAddress.setUprn("123");
    addressFlow.setSelectedAddress(selectedAddress);

    mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", new ClientFormDataAddressDetails()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address/confirm"));
  }

  @Test
  void clientAddressTransitionsRedirectWhenTheFlowHasExpired() throws Exception {
    mockMvc
        .perform(get("/application/client/details/address/confirm"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    mockMvc
        .perform(post("/application/client/details/address/confirm"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    mockMvc
        .perform(get("/application/client/details/address/search-again"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    mockMvc
        .perform(
            get("/application/client/details/address/manual")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }

  @Test
  void testClientDetailsAddressPostValidationError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue("country", "required.country", "Please complete 'Country'.");
              return null;
            })
        .when(clientAddressDetailsValidator)
        .validateForAddressComponent(any(), any());

    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, newManualAddressFlow())
                .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostSearchError() throws Exception {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue("country", "required.country", "Please complete 'Country'.");
              return null;
            })
        .when(findAddressValidator)
        .validate(any(), any());

    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "find_address")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  private AddressLookupFlowData<ClientFormDataAddressDetails> newManualAddressFlow() {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>("create-client");
    addressFlow.enterManualEntry(new ClientFormDataAddressDetails());
    return addressFlow;
  }
}
