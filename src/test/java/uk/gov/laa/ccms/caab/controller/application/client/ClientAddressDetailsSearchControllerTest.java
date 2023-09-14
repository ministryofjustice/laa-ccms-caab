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

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressSearchValidator;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

@ExtendWith(MockitoExtension.class)
public class ClientAddressDetailsSearchControllerTest {

  @Mock
  private AddressService addressService;

  @Mock
  private ClientAddressSearchValidator clientAddressSearchValidator;

  @InjectMocks
  private ClientAddressDetailsSearchController clientAddressDetailsSearchController;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientAddressDetailsSearchController).build();
  }

  @Test
  void testClientDetailsAddressSearch() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    ClientAddressResultsDisplay searchResults = new ClientAddressResultsDisplay();
    searchResults.setContent(new ArrayList<>());

    when(addressService.getAddresses(any())).thenReturn(searchResults);

    when(addressService.filterByHouseNumber(any(), any(ClientAddressResultsDisplay.class)))
        .thenReturn(searchResults);

    mockMvc.perform(get("/application/client/details/address/search")
            .sessionAttr("clientDetails", clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("clientAddressSearchResults"));
  }

  @Test
  void testClientDetailsAddressSearch_NoResults() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    ClientAddressResultsDisplay searchResults = new ClientAddressResultsDisplay();
    searchResults.setContent(null);

    when(addressService.getAddresses(any())).thenReturn(searchResults);

    mockMvc.perform(get("/application/client/details/address/search")
            .sessionAttr("clientDetails", clientDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }

  @Test
  void testClientDetailsAddressSearchPost_Success() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    ClientAddressResultsDisplay searchResults = new ClientAddressResultsDisplay();

    mockMvc.perform(post("/application/client/details/address/search")
            .sessionAttr("clientDetails", clientDetails)
            .sessionAttr("clientAddressSearchResults", searchResults))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address"));
  }

  @Test
  void testClientDetailsAddressSearchPost_ValidationError() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    ClientAddressResultsDisplay searchResults = new ClientAddressResultsDisplay();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("uprn", "required.uprn", "Please complete 'UPRN'.");
      return null;
    }).when(clientAddressSearchValidator).validate(any(), any());

    mockMvc.perform(post("/application/client/details/address/search")
            .sessionAttr("clientDetails", clientDetails)
            .sessionAttr("clientAddressSearchResults", searchResults))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-search-results"))
        .andExpect(model().attributeExists("clientAddressSearchResults"));
  }
}
