package uk.gov.laa.ccms.caab.controller.client;

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
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class EditClientAddressDetailsControllerTest {

  @Mock private LookupService lookupService;

  @Mock private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock private FindAddressValidator findAddressValidator;

  @Mock private AddressService addressService;

  @InjectMocks private EditClientAddressDetailsController editClientAddressDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail countryLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataAddressDetails addressDetails;

  private ClientFormDataBasicDetails basicDetails;

  @BeforeEach
  void setup() {
    mockMvc =
        standaloneSetup(editClientAddressDetailsController)
            .setConversionService(getConversionService())
            .build();

    basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);

    addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setVulnerableClient(false);

    clientFlowFormData = new ClientFlowFormData(ACTION_EDIT);
    clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());
    clientFlowFormData.setAddressDetails(addressDetails);

    countryLookupDetail = new CommonLookupDetail();
    countryLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Nested
  @DisplayName("Application tests")
  class ApplicationTests {

    @Test
    void testEditClientDetailsAddress() throws Exception {
      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              get("/application/sections/client/details/address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }

    @Test
    void testEditClientDetailsAddressPostFindAddress_NoAddresses() throws Exception {
      when(addressService.getAddresses(any()))
          .thenReturn(new ResultsDisplay<AddressResultRowDisplay>());

      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              post("/application/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"));
    }

    @Test
    void testEditClientDetailsAddressPostFindAddress_WithAddresses() throws Exception {
      ResultsDisplay<AddressResultRowDisplay> addressResults = new ResultsDisplay<>();
      addressResults.setContent(new ArrayList<>());
      addressResults.getContent().add(new AddressResultRowDisplay());

      when(addressService.getAddresses(any())).thenReturn(addressResults);

      when(addressService.filterByHouseNumber(any(), any())).thenReturn(addressResults);

      mockMvc
          .perform(
              post("/application/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/application/sections/client/details/address/search"));
    }

    @Test
    void testEditClientDetailsAddressPostNext() throws Exception {
      ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

      mockMvc
          .perform(
              post("/application/sections/client/details/address")
                  .param("action", "next")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/application/sections/client/details/summary"));
    }

    @Test
    void testEditClientDetailsAddressPostValidationError() throws Exception {
      ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue("country", "required.country", "Please complete 'Country'.");
                return null;
              })
          .when(clientAddressDetailsValidator)
          .validate(any(), any());

      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              post("/application/sections/client/details/address")
                  .param("action", "next")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }

    @Test
    void testEditClientDetailsAddressPostSearchError() throws Exception {
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

      mockMvc
          .perform(
              post("/application/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }
  }

  @Nested
  @DisplayName("Amendments tests")
  class AmendmentsTests {

    @Test
    void testEditClientDetailsAddress() throws Exception {
      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              get("/amendments/sections/client/details/address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }

    @Test
    void testEditClientDetailsAddressPostFindAddress_NoAddresses() throws Exception {
      when(addressService.getAddresses(any()))
          .thenReturn(new ResultsDisplay<AddressResultRowDisplay>());

      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              post("/amendments/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"));
    }

    @Test
    void testEditClientDetailsAddressPostFindAddress_WithAddresses() throws Exception {
      ResultsDisplay<AddressResultRowDisplay> addressResults = new ResultsDisplay<>();
      addressResults.setContent(new ArrayList<>());
      addressResults.getContent().add(new AddressResultRowDisplay());

      when(addressService.getAddresses(any())).thenReturn(addressResults);

      when(addressService.filterByHouseNumber(any(), any())).thenReturn(addressResults);

      mockMvc
          .perform(
              post("/amendments/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/amendments/sections/client/details/address/search"));
    }

    @Test
    void testEditClientDetailsAddressPostNext() throws Exception {
      ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

      mockMvc
          .perform(
              post("/amendments/sections/client/details/address")
                  .param("action", "next")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/amendments/sections/client/details/summary"));
    }

    @Test
    void testEditClientDetailsAddressPostValidationError() throws Exception {
      ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue("country", "required.country", "Please complete 'Country'.");
                return null;
              })
          .when(clientAddressDetailsValidator)
          .validate(any(), any());

      when(lookupService.getCountries()).thenReturn(Mono.just(countryLookupDetail));

      mockMvc
          .perform(
              post("/amendments/sections/client/details/address")
                  .param("action", "next")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }

    @Test
    void testEditClientDetailsAddressPostSearchError() throws Exception {
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

      mockMvc
          .perform(
              post("/amendments/sections/client/details/address")
                  .param("action", "find_address")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-address-details"))
          .andExpect(model().attributeExists("countries"));
    }
  }
}
