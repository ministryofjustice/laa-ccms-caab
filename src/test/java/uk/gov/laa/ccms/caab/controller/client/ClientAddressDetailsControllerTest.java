package uk.gov.laa.ccms.caab.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.config.CreateClientRegistrationInterceptor;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
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
    addressDetails.setCountry("TAMPERED");
    addressDetails.setPostcode("TAMPERED");
    addressDetails.setAddressLine1("Posted address");
    addressDetails.setCityTown("Posted town");
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressFlow.setSource(AddressLookupFlowData.Source.SEARCHED);
    AddressResultRowDisplay selected = new AddressResultRowDisplay();
    selected.setUprn("123");
    selected.setPostcode("sa58ab");
    selected.setCountry("GBR");
    selected.setHouseNameNumber("10");
    selected.setAddressLine1("Canonical line 1");
    selected.setAddressLine2("Canonical line 2");
    selected.setCityTown("Canonical town");
    selected.setCounty("Canonical county");
    addressFlow.setSelectedAddress(selected);
    addressFlow.confirmSelectedAddress();
    final ClientAddressResultDisplayMapper mapper =
        Mappers.getMapper(ClientAddressResultDisplayMapper.class);
    doAnswer(
            invocation -> {
              mapper.updateClientFormDataAddressDetails(
                  invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(addressService)
        .updateClientFormDataAddressDetails(any(), any());

    this.mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", addressDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));

    assertEquals("GBR", clientFlowFormData.getAddressDetails().getCountry());
    assertEquals("10", clientFlowFormData.getAddressDetails().getHouseNameNumber());
    assertEquals("sa58ab", clientFlowFormData.getAddressDetails().getPostcode());
    assertEquals("Canonical line 1", clientFlowFormData.getAddressDetails().getAddressLine1());
    assertEquals("Canonical line 2", clientFlowFormData.getAddressDetails().getAddressLine2());
    assertEquals("Canonical town", clientFlowFormData.getAddressDetails().getCityTown());
    assertEquals("Canonical county", clientFlowFormData.getAddressDetails().getCounty());
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
  void clientNextCannotBypassTheResultsSubmission() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
    selectedAddress.setUprn("123");
    addressFlow.setSelectedAddress(selectedAddress);
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    mockMvc
        .perform(
            post("/application/client/details/address")
                .param("action", "next")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressDetails", new ClientFormDataAddressDetails()))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("addressDetails"))
        .andExpect(view().name("application/client/address-client-details"));

    assertNull(clientFlowFormData.getAddressDetails());
  }

  @Test
  void clientConfirmationEndpointsAreRemoved() throws Exception {
    mockMvc
        .perform(get("/application/client/details/address/confirm"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(post("/application/client/details/address/confirm"))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @EnumSource(
      value = AddressLookupFlowData.Step.class,
      names = {"SELECTED_ADDRESS", "CONFIRMED_ADDRESS", "MANUAL_ENTRY"})
  void searchAgainRestoresOriginalTermsInAFreshForm(AddressLookupFlowData.Step initialStep)
      throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressFlow.beginSearch("sw1a1aa", " 12 ");
    addressDetails.setPostcode("SA5 8AB");
    addressDetails.setHouseNameNumber("12 Example House");
    addressDetails.setCountry("GBR");
    addressDetails.setAddressLine1("Example Street");
    addressDetails.setAddressLine2("Example District");
    addressDetails.setCityTown("Example Town");
    addressDetails.setCounty("Example County");
    addressDetails.setAddressSearch(new AddressSearchFormData());
    clientFlowFormData.setAddressDetails(addressDetails);
    clientFlowFormData.getBasicDetails().setVulnerableClient(false);
    addressFlow.setAddressDetails(addressDetails);
    AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
    selectedAddress.setUprn("123");
    ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();
    results.setContent(List.of(selectedAddress));
    addressFlow.setSearchResults(results);
    if (initialStep == AddressLookupFlowData.Step.MANUAL_ENTRY) {
      addressFlow.enterManualEntry(addressDetails);
    } else {
      addressFlow.setSelectedAddress(selectedAddress);
      if (initialStep == AddressLookupFlowData.Step.CONFIRMED_ADDRESS) {
        addressFlow.confirmSelectedAddress();
      }
    }
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    mockMvc
        .perform(get("/application/client/details/address/search-again").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    ClientFormDataAddressDetails searchDetails = addressFlow.getAddressDetails();
    assertNotSame(addressDetails, searchDetails);
    assertEquals("sw1a1aa", searchDetails.getPostcode());
    assertEquals(" 12 ", searchDetails.getHouseNameNumber());
    assertNull(searchDetails.getCountry());
    assertNull(searchDetails.getAddressLine1());
    assertNull(searchDetails.getAddressLine2());
    assertNull(searchDetails.getCityTown());
    assertNull(searchDetails.getCounty());
    assertNull(searchDetails.getAddressSearch());
    assertEquals(AddressLookupFlowData.Step.SEARCH, addressFlow.getStep());
    assertEquals(AddressLookupFlowData.Source.UNKNOWN, addressFlow.getSource());
    assertNull(addressFlow.getSearchResults());
    assertNull(addressFlow.getSelectedAddress());
    assertEquals("sw1a1aa", addressFlow.getOriginalPostcode());
    assertEquals(" 12 ", addressFlow.getOriginalBuildingSearch());

    mockMvc
        .perform(get("/application/client/details/address").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attribute("addressDetails", searchDetails));

    assertEquals(false, searchDetails.getVulnerableClient());
    assertEquals(ACTION_CREATE, searchDetails.getClientFlowFormAction());
    assertSame(addressDetails, clientFlowFormData.getAddressDetails());
    assertEquals("SA5 8AB", addressDetails.getPostcode());
    assertEquals("12 Example House", addressDetails.getHouseNameNumber());
    assertEquals("Example Street", addressDetails.getAddressLine1());
    verifyNoInteractions(addressService);
  }

  @Test
  void searchAgainWithoutAPreviousQueryStartsWithBlankSearchFields() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressDetails.setPostcode("SA5 8AB");
    addressDetails.setHouseNameNumber("12 Example House");
    addressFlow.enterManualEntry(addressDetails);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    mockMvc
        .perform(get("/application/client/details/address/search-again").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    ClientFormDataAddressDetails searchDetails = addressFlow.getAddressDetails();
    assertNotSame(addressDetails, searchDetails);
    assertNull(searchDetails.getPostcode());
    assertNull(searchDetails.getHouseNameNumber());
    mockMvc
        .perform(get("/application/client/details/address").session(session))
        .andExpect(status().isOk())
        .andExpect(model().attribute("addressDetails", searchDetails));
    verifyNoInteractions(addressService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void searchAgainPreservesAnOmittedBuildingTerm(String buildingSearch) throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressFlow.beginSearch("SW1A 1AA", buildingSearch);
    addressDetails.setHouseNameNumber("12 Example House");
    addressFlow.enterManualEntry(addressDetails);

    mockMvc
        .perform(
            get("/application/client/details/address/search-again")
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    assertEquals("SW1A 1AA", addressFlow.getAddressDetails().getPostcode());
    assertEquals(buildingSearch, addressFlow.getAddressDetails().getHouseNameNumber());
    verifyNoInteractions(addressService);
  }

  @Test
  void revisedSearchTermsAreUsedForTheNextLookupAndReturnToSearch() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressFlow.beginSearch("SW1A 1AA", "10");
    addressFlow.setAddressDetails(addressDetails);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();
    results.setContent(List.of(new AddressResultRowDisplay()));
    ResultsDisplay<AddressResultRowDisplay> filteredResults = new ResultsDisplay<>();
    when(addressService.getAddresses("sa58ab")).thenReturn(results);
    when(addressService.filterByHouseNumber("15", results)).thenReturn(filteredResults);
    when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

    mockMvc
        .perform(get("/application/client/details/address/search-again").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    mockMvc
        .perform(
            post("/application/client/details/address")
                .session(session)
                .param("action", "find_address")
                .param("postcode", "sa58ab")
                .param("houseNameNumber", "15"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address/search"));

    verify(addressService).getAddresses("sa58ab");
    verify(addressService).filterByHouseNumber("15", results);
    assertEquals("sa58ab", addressFlow.getOriginalPostcode());
    assertEquals("15", addressFlow.getOriginalBuildingSearch());
    assertSame(filteredResults, addressFlow.getSearchResults());

    mockMvc
        .perform(get("/application/client/details/address/search-again").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    ClientFormDataAddressDetails searchDetails = addressFlow.getAddressDetails();
    assertEquals("sa58ab", searchDetails.getPostcode());
    assertEquals("15", searchDetails.getHouseNameNumber());
    mockMvc
        .perform(get("/application/client/details/address").session(session))
        .andExpect(status().isOk())
        .andExpect(model().attribute("addressDetails", searchDetails));
  }

  @Test
  void clientAddressTransitionsRedirectWhenTheFlowHasExpired() throws Exception {
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
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    addressFlow.enterManualEntry(new ClientFormDataAddressDetails());
    return addressFlow;
  }
}
