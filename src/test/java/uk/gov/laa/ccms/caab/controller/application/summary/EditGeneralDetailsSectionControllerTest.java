package uk.gov.laa.ccms.caab.controller.application.summary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class EditGeneralDetailsSectionControllerTest {

  @Mock
  private ApplicationService applicationService;

  @Mock
  private AddressService addressService;

  @Mock
  private LookupService lookupService;

  @Mock
  private  FindAddressValidator findAddressValidator;

  @Mock
  private AddressSearchValidator addressSearchValidator;

  @Mock
  private CorrespondenceAddressValidator correspondenceAddressValidator;

  @InjectMocks
  private EditGeneralDetailsSectionController editGeneralDetailsSectionController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private CommonLookupDetail mockCommonLookupDetail;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(editGeneralDetailsSectionController).build();
    mockCommonLookupDetail = new CommonLookupDetail();
    mockCommonLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  public void testCorrespondenceDetailsGet() throws Exception {
    final String applicationId = "123";
    final AddressFormData addressFormData = new AddressFormData();

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCaseAddressOptions())
        .thenReturn(Mono.just(mockCommonLookupDetail));

    when(applicationService.getCorrespondenceAddressFormData(applicationId)).thenReturn(addressFormData);

    this.mockMvc.perform(get("/application/summary/correspondence-address")
            .sessionAttr(APPLICATION_ID, applicationId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/correspondence-address-details"))
        .andExpect(model().attribute("addressDetails", addressFormData));

    verify(applicationService, times(1)).getCorrespondenceAddressFormData(applicationId);
  }



  @Test
  public void testCorrespondenceDetailsGet_withSessionData() throws Exception {
    final String applicationId = "123";
    final AddressFormData addressFormData = new AddressFormData();

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCaseAddressOptions())
        .thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(get("/application/summary/correspondence-address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr("addressDetails", addressFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/correspondence-address-details"))
        .andExpect(model().attribute("addressDetails", addressFormData));

    verify(applicationService, never()).getCorrespondenceAddressFormData(applicationId);
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_next() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();

    this.mockMvc.perform(post("/application/summary/correspondence-address")
            .param("action", "update")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/summary/linked-cases"));

    verify(applicationService, times(1)).updateCorrespondenceAddress(applicationId, addressDetails, user);
    verify(addressService, never()).getAddresses(any());
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_next_handlesValidationError() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCaseAddressOptions())
        .thenReturn(Mono.just(mockCommonLookupDetail));

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("preferredAddress", "required.preferredAddress", "Please select an Preferred address.");
      return null;
    }).when(correspondenceAddressValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/summary/correspondence-address")
            .param("action", "update")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(view().name("application/summary/correspondence-address-details"));

    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
    verify(addressService, never()).getAddresses(any());
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_findAddress_successful() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();
    final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<AddressResultRowDisplay>();

    addressSearchResults.setContent(Collections.singletonList(new AddressResultRowDisplay()));

    when(addressService.getAddresses(addressDetails.getPostcode())).thenReturn(addressSearchResults);

    this.mockMvc.perform(post("/application/summary/correspondence-address")
            .param("action", "find_address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/summary/correspondence-address/search"));

    verify(addressService, times(1)).getAddresses(any());
    verify(addressService, times(1)).filterByHouseNumber(any(), any());
    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_findAddress_noResults() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();
    final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<AddressResultRowDisplay>();

    when(addressService.getAddresses(addressDetails.getPostcode())).thenReturn(addressSearchResults);

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCaseAddressOptions())
        .thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(post("/application/summary/correspondence-address")
            .param("action", "find_address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(view().name("application/summary/correspondence-address-details"));

    verify(addressService, times(1)).getAddresses(any());
    verify(addressService, never()).filterByHouseNumber(any(), any());
    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
  }

  @Test
  public void testCorrespondenceAddressSearchGet() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<AddressResultRowDisplay>();

    this.mockMvc.perform(get("/application/summary/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/address-search-results"))
        .andExpect(model().attribute("backLink", "/application/summary/correspondence-address"))
        .andExpect(model().attribute("formAction", "application/summary/correspondence-address/search"))
        .andExpect(model().attribute("addressSearchResults", results));
  }

  @Test
  public void testCorrespondenceAddressSearchPost_successful() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<AddressResultRowDisplay>();

    this.mockMvc.perform(post("/application/summary/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
            .sessionAttr("addressDetails", new AddressFormData()))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary/correspondence-address"));

    verify(addressService, times(1)).filterAndUpdateAddressFormData(any(), any(), any());

  }

  @Test
  public void testCorrespondenceAddressSearchPost_handlesValidationError() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<AddressResultRowDisplay>();

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject( "required.uprn", "Please select an address.");
      return null;
    }).when(addressSearchValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/summary/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
            .sessionAttr("addressDetails", new AddressFormData()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/address-search-results"))
        .andExpect(model().attribute("backLink", "/application/summary/correspondence-address"))
        .andExpect(model().attribute("formAction", "application/summary/correspondence-address/search"))
        .andExpect(model().attribute("addressSearchResults", results));

    verify(addressService, never()).filterAndUpdateAddressFormData(any(), any(), any());

  }



}