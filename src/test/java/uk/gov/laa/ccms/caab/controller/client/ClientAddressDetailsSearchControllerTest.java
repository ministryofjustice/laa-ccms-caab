package uk.gov.laa.ccms.caab.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

@ExtendWith(MockitoExtension.class)
public class ClientAddressDetailsSearchControllerTest {

  @Mock private AddressService addressService;

  @Mock private AddressSearchValidator addressSearchValidator;

  @InjectMocks private ClientAddressDetailsSearchController clientAddressDetailsSearchController;

  private MockMvc mockMvc;

  private AddressSearchFormData addressSearch;
  private ResultsDisplay<AddressResultRowDisplay> searchResults;
  private AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow;
  private ClientFlowFormData clientFlowFormData;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientAddressDetailsSearchController).build();

    addressSearch = new AddressSearchFormData();
    addressSearch.setUprn("123");
    searchResults = new ResultsDisplay<>();
    AddressResultRowDisplay result = new AddressResultRowDisplay();
    result.setUprn("123");
    searchResults.setContent(new ArrayList<>(List.of(result)));
    addressFlow = new AddressLookupFlowData<>("create-client");
    addressFlow.setSearchResults(searchResults);
    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    final ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);
    clientFlowFormData.setBasicDetails(basicDetails);
  }

  @Test
  void testClientDetailsAddressSearch() throws Exception {

    mockMvc
        .perform(
            get("/application/client/details/address/search")
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressSearch", addressSearch))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("addressSearch"));
  }

  @Test
  void addressSearchRedirectsWhenTheFlowHasExpired() throws Exception {
    mockMvc
        .perform(get("/application/client/details/address/search"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"search", "client", "empty"})
  void selectionPopulatesAddressDataBeforeReturningToTheForm(final String existingData)
      throws Exception {
    final ClientFormDataAddressDetails previousDetails = new ClientFormDataAddressDetails();
    previousDetails.setPostcode("search terms");
    previousDetails.setVulnerableClient(true);
    previousDetails.setClientFlowFormAction("untrusted");
    if ("search".equals(existingData)) {
      addressFlow.setAddressDetails(previousDetails);
    } else if ("client".equals(existingData)) {
      clientFlowFormData.setAddressDetails(previousDetails);
    }
    final AddressResultRowDisplay selected = searchResults.getContent().getFirst();
    selected.setHouseNameNumber("10");
    selected.setAddressLine1("10 Example Street");
    selected.setAddressLine2("Example district");
    selected.setCityTown("Swansea");
    selected.setCounty("West Glamorgan");
    selected.setPostcode("SA5 8AB");
    selected.setCountry("GBR");
    when(addressService.getSelectedAddress(any(), any())).thenReturn(selected);
    final AddressService mappingService =
        new AddressService(null, Mappers.getMapper(ClientAddressResultDisplayMapper.class), null);
    doAnswer(
            invocation -> {
              mappingService.updateClientFormDataAddressDetails(
                  invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(addressService)
        .updateClientFormDataAddressDetails(any(), any());

    // Reposting an old results page must not replace the accepted address.
    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(
              post("/application/client/details/address/search")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                  .param("uprn", attempt == 0 ? "123" : "forged"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/application/client/details/address"));
    }

    final ClientFormDataAddressDetails accepted = addressFlow.getAddressDetails();
    assertSame(accepted, clientFlowFormData.getAddressDetails());
    if (!"empty".equals(existingData)) {
      assertSame(previousDetails, accepted);
    }
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, addressFlow.getStep());
    assertEquals(AddressLookupFlowData.Source.SEARCHED, addressFlow.getSource());
    assertSame(selected, addressFlow.getSelectedAddress());
    assertEquals("10", accepted.getHouseNameNumber());
    assertEquals("10 Example Street", accepted.getAddressLine1());
    assertEquals("Example district", accepted.getAddressLine2());
    assertEquals("Swansea", accepted.getCityTown());
    assertEquals("West Glamorgan", accepted.getCounty());
    assertEquals("SA5 8AB", accepted.getPostcode());
    assertEquals("GBR", accepted.getCountry());
    assertEquals(false, accepted.getNoFixedAbode());
    assertEquals(false, accepted.getVulnerableClient());
    assertEquals(ACTION_CREATE, accepted.getClientFlowFormAction());
    assertNull(accepted.getAddressSearch().getUprn());
  }

  @Test
  void selectionDoesNotConsumeResultsWhenTheClientFlowHasExpired() throws Exception {
    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .param("uprn", "123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    assertEquals(AddressLookupFlowData.Step.RESULTS, addressFlow.getStep());
    assertNull(addressFlow.getSelectedAddress());
    verifyNoInteractions(addressService, addressSearchValidator);
  }

  @ParameterizedTest
  @EnumSource(
      value = AddressLookupFlowData.Step.class,
      names = "RESULTS",
      mode = EnumSource.Mode.EXCLUDE)
  void selectionOutsideTheResultsStepDoesNotPopulateTheClient(final AddressLookupFlowData.Step step)
      throws Exception {
    addressFlow.setStep(step);
    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .param("uprn", "123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));

    assertNull(clientFlowFormData.getAddressDetails());
    assertNull(addressFlow.getSelectedAddress());
    verifyNoInteractions(addressService, addressSearchValidator);
  }

  @Test
  void unresolvedSelectionDoesNotPopulateTheClient() throws Exception {
    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .param("uprn", "123"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("addressSearch", "uprn"));

    assertEquals(AddressLookupFlowData.Step.RESULTS, addressFlow.getStep());
    assertNull(clientFlowFormData.getAddressDetails());
    assertNull(addressFlow.getSelectedAddress());
  }

  @Test
  void testClientDetailsAddressSearchPost_ValidationError() throws Exception {

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[2];
              errors.rejectValue("uprn", "required.uprn", "Please complete 'UPRN'.");
              return null;
            })
        .when(addressSearchValidator)
        .validateSelection(any(), any(), any());

    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(CREATE_CLIENT_ADDRESS_FLOW, addressFlow)
                .flashAttr("addressSearch", addressSearch))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("addressSearch", ADDRESS_SEARCH_RESULTS));
  }
}
