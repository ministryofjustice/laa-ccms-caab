package uk.gov.laa.ccms.caab.controller.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
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
  private ClientFlowFormData clientFlowFormData;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientAddressDetailsSearchController).build();

    addressSearch = new AddressSearchFormData();
    searchResults = new ResultsDisplay<>();
    searchResults.setContent(new ArrayList<>());
    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
  }

  @Test
  void testClientDetailsAddressSearch() throws Exception {

    mockMvc
        .perform(
            get("/application/client/details/address/search")
                .sessionAttr(ADDRESS_SEARCH_RESULTS, searchResults)
                .flashAttr("addressSearch", addressSearch))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("addressSearch"));
  }

  @Test
  void testClientDetailsAddressSearchPost() throws Exception {

    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(ADDRESS_SEARCH_RESULTS, searchResults)
                .flashAttr("addressSearch", addressSearch))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }

  @Test
  void testClientDetailsAddressSearchPost_ValidationError() throws Exception {

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue("uprn", "required.uprn", "Please complete 'UPRN'.");
              return null;
            })
        .when(addressSearchValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/application/client/details/address/search")
                .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                .sessionAttr(ADDRESS_SEARCH_RESULTS, searchResults)
                .flashAttr("addressSearch", addressSearch))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("addressSearch", ADDRESS_SEARCH_RESULTS));
  }
}
