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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressSearchValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class ClientAddressDetailsControllerTest {

  @Mock
  private CommonLookupService commonLookupService;

  @Mock
  private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock
  private ClientAddressSearchValidator clientAddressSearchValidator;

  @InjectMocks
  private ClientAddressDetailsController clientAddressDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail countryLookupDetail;

  @BeforeEach
  public void setup() {

    mockMvc = standaloneSetup(clientAddressDetailsController).build();
    countryLookupDetail = new CommonLookupDetail();
    countryLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testClientDetailsAddress() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(get("/application/client/details/address")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostFindAddress() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "find_address")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/address/search"));
  }

  @Test
  void testClientDetailsAddressPostNext() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "next")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/equal-opportunities-monitoring"));
  }

  @Test
  void testClientDetailsAddressPostValidationError() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(clientAddressDetailsValidator).validate(any(), any());

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "next")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

  @Test
  void testClientDetailsAddressPostSearchError() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("country", "required.country", "Please complete 'Country'.");
      return null;
    }).when(clientAddressSearchValidator).validate(any(), any());

    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));

    this.mockMvc.perform(post("/application/client/details/address")
            .param("action", "find_address")
            .flashAttr(CLIENT_DETAILS, clientDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/address-client-details"))
        .andExpect(model().attributeExists("countries"));
  }

}