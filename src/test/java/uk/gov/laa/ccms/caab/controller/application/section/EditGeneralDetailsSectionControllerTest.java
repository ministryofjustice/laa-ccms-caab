package uk.gov.laa.ccms.caab.controller.application.section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_LINK_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.application.section.EditGeneralDetailsSectionController.CASE_RESULTS_PAGE;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.LinkedCaseValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
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
  private ProviderService providerService;
  @Mock
  private LookupService lookupService;

  @Mock
  private CaseSearchCriteriaValidator searchCriteriaValidator;

  @Mock
  private  FindAddressValidator findAddressValidator;

  @Mock
  private AddressSearchValidator addressSearchValidator;

  @Mock
  private LinkedCaseValidator linkedCaseValidator;

  @Mock
  private CorrespondenceAddressValidator correspondenceAddressValidator;

  @Mock
  private EbsApplicationMapper applicationMapper;

  @Mock
  private ResultDisplayMapper resultDisplayMapper;

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
    when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .thenReturn(Mono.just(mockCommonLookupDetail));

    when(applicationService.getCorrespondenceAddressFormData(applicationId)).thenReturn(addressFormData);

    this.mockMvc.perform(get("/application/sections/correspondence-address")
            .sessionAttr(APPLICATION_ID, applicationId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/correspondence-address-details"))
        .andExpect(model().attribute("addressDetails", addressFormData));

    verify(applicationService, times(1)).getCorrespondenceAddressFormData(applicationId);
  }



  @Test
  public void testCorrespondenceDetailsGet_withSessionData() throws Exception {
    final String applicationId = "123";
    final AddressFormData addressFormData = new AddressFormData();

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(get("/application/sections/correspondence-address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr("addressDetails", addressFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/correspondence-address-details"))
        .andExpect(model().attribute("addressDetails", addressFormData));

    verify(applicationService, never()).getCorrespondenceAddressFormData(applicationId);
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_next() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();

    this.mockMvc.perform(post("/application/sections/correspondence-address")
            .param("action", "update")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/sections/linked-cases"));

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
    when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .thenReturn(Mono.just(mockCommonLookupDetail));

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("preferredAddress", "required.preferredAddress", "Please select an Preferred address.");
      return null;
    }).when(correspondenceAddressValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/sections/correspondence-address")
            .param("action", "update")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(view().name("application/sections/correspondence-address-details"));

    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
    verify(addressService, never()).getAddresses(any());
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_findAddress_successful() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();
    final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<>();

    addressSearchResults.setContent(Collections.singletonList(new AddressResultRowDisplay()));

    when(addressService.getAddresses(addressDetails.getPostcode())).thenReturn(addressSearchResults);

    this.mockMvc.perform(post("/application/sections/correspondence-address")
            .param("action", "find_address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/sections/correspondence-address/search"));

    verify(addressService, times(1)).getAddresses(any());
    verify(addressService, times(1)).filterByHouseNumber(any(), any());
    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
  }

  @Test
  public void testUpdateCorrespondenceDetailsPost_findAddress_noResults() throws Exception {
    final String applicationId = "123";
    final UserDetail user = new UserDetail();
    final AddressFormData addressDetails = new AddressFormData();
    final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<>();

    when(addressService.getAddresses(addressDetails.getPostcode())).thenReturn(addressSearchResults);

    when(lookupService.getCountries())
        .thenReturn(Mono.just(mockCommonLookupDetail));
    when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(post("/application/sections/correspondence-address")
            .param("action", "find_address")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("addressDetails", addressDetails))
        .andDo(print())
        .andExpect(view().name("application/sections/correspondence-address-details"));

    verify(addressService, times(1)).getAddresses(any());
    verify(addressService, never()).filterByHouseNumber(any(), any());
    verify(applicationService, never()).updateCorrespondenceAddress(applicationId, addressDetails, user);
  }

  @Test
  public void testCorrespondenceAddressSearchGet() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

    this.mockMvc.perform(get("/application/sections/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/address-search-results"))
        .andExpect(model().attribute("formAction", "application/sections/correspondence-address/search"))
        .andExpect(model().attribute("addressSearchResults", results));
  }

  @Test
  public void testCorrespondenceAddressSearchPost_successful() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

    this.mockMvc.perform(post("/application/sections/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
            .sessionAttr("addressDetails", new AddressFormData()))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections/correspondence-address"));

    verify(addressService, times(1)).filterAndUpdateAddressFormData(any(), any(), any());

  }

  @Test
  public void testCorrespondenceAddressSearchPost_handlesValidationError() throws Exception {
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject( "required.uprn", "Please select an address.");
      return null;
    }).when(addressSearchValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/sections/correspondence-address/search")
            .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
            .sessionAttr("addressDetails", new AddressFormData()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/address-search-results"))
        .andExpect(model().attribute("formAction", "application/sections/correspondence-address/search"))
        .andExpect(model().attribute("addressSearchResults", results));

    verify(addressService, never()).filterAndUpdateAddressFormData(any(), any(), any());

  }

  @Test
  public void testLinkedCasesGet() throws Exception {
    final String applicationId = "123";
    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();

    when(applicationService.getLinkedCases(applicationId)).thenReturn(linkedCases);

    this.mockMvc.perform(get("/application/sections/linked-cases")
            .sessionAttr(APPLICATION_ID, applicationId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-summary"))
        .andExpect(model().attribute("linkedCases", linkedCases));

    verify(applicationService, times(1)).getLinkedCases(applicationId);
  }

  @Test
  public void testRemoveLinkedCaseGet() throws Exception {
    final Integer linkedCaseId = 1;
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();
    linkedCase.setId(linkedCaseId);

    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();
    linkedCases.setContent(Collections.singletonList(linkedCase));

    this.mockMvc.perform(get("/application/sections/linked-cases/{linked-case-id}/remove", linkedCaseId)
            .sessionAttr("linkedCases", linkedCases))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-remove"))
        .andExpect(model().attribute("linkedCase", linkedCase));
  }

  @Test
  public void testRemoveLinkedCasePost() throws Exception {
    final String applicationId = "123";
    final String linkedCaseId = "1";
    final UserDetail user = new UserDetail();

    this.mockMvc.perform(post("/application/sections/linked-cases/{linked-case-id}/remove", linkedCaseId)
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections/linked-cases"));

    verify(applicationService, times(1)).removeLinkedCase(linkedCaseId, user);
  }

  @Test
  public void testConfirmLinkedCaseGet() throws Exception {
    final Integer linkedCaseId = 1;
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();
    linkedCase.setId(linkedCaseId);

    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();
    linkedCases.setContent(Collections.singletonList(linkedCase));

    when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE)).thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(get("/application/sections/linked-cases/{linked-case-id}/confirm", linkedCaseId)
            .sessionAttr("linkedCases", linkedCases))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-confirm"))
        .andExpect(model().attribute("currentLinkedCase", linkedCase));
  }

  @Test
  public void testConfirmLinkedCasePost() throws Exception {
    final String applicationId = "123";
    final String linkedCaseId = "1";
    final UserDetail user = new UserDetail();
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

    this.mockMvc.perform(post("/application/sections/linked-cases/{linked-case-id}/confirm", linkedCaseId)
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("linkedCase", linkedCase))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections/linked-cases"));

    verify(applicationService, times(1)).updateLinkedCase(linkedCaseId, linkedCase, user);
  }

  @Test
  public void testConfirmLinkedCasePost_validationError() throws Exception {
    final String applicationId = "123";
    final String linkedCaseId = "1";
    final UserDetail user = new UserDetail();
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

    when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE)).thenReturn(Mono.just(mockCommonLookupDetail));

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject("error.code", "Error message");
      return null;
    }).when(linkedCaseValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/sections/linked-cases/{linked-case-id}/confirm", linkedCaseId)
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("linkedCase", linkedCase))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-confirm"));

    verify(applicationService, never()).updateLinkedCase(linkedCaseId, linkedCase, user);
  }

  @Test
  public void testLinkedCasesSearchGet() throws Exception {
    final ProviderDetail mockProviderDetail = new ProviderDetail();
    final CaseStatusLookupDetail mockCaseStatusValues = new CaseStatusLookupDetail();

    when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS)).thenReturn(Mono.just(mockCommonLookupDetail));
    when(providerService.getProvider(any())).thenReturn(Mono.just(mockProviderDetail));
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(mockCaseStatusValues));

    this.mockMvc.perform(get("/application/sections/linked-cases/search")
            .sessionAttr(USER_DETAILS, buildUserDetail()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-search"));
  }

  @Test
  public void testLinkedCasesSearchPost_validationError() throws Exception {

    final ProviderDetail mockProviderDetail = new ProviderDetail();
    final CaseStatusLookupDetail mockCaseStatusValues = new CaseStatusLookupDetail();

    when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS)).thenReturn(Mono.just(mockCommonLookupDetail));
    when(providerService.getProvider(any())).thenReturn(Mono.just(mockProviderDetail));
    when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(mockCaseStatusValues));

    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.reject("error.code", "Error message");
      return null;
    }).when(searchCriteriaValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/sections/linked-cases/search")
            .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
            .sessionAttr(USER_DETAILS, buildUserDetail())
            .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
            .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-search"));
  }

  @Test
  public void testLinkedCasesSearchPost_emptySearchResults() throws Exception {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    when(applicationService.getCases(any(), any())).thenReturn(Collections.emptyList());

    this.mockMvc.perform(post("/application/sections/linked-cases/search")
            .sessionAttr(ACTIVE_CASE,  ActiveCase.builder().build())
            .sessionAttr(USER_DETAILS, buildUserDetail())
            .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
            .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-search-no-results"));
  }

  @Test
  public void testLinkedCasesSearchPost_tooManyResults() throws Exception {
    // Arrange
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();

    // Mock the applicationService to throw TooManyResultsException
    when(applicationService.getCases(any(), any()))
        .thenThrow(new TooManyResultsException("test"));

    // Act & Assert
    this.mockMvc.perform(post("/application/sections/linked-cases/search")
            .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
            .sessionAttr(USER_DETAILS, buildUserDetail())
            .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
            .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-search-too-many-results"));
  }

  @Test
  public void testLinkedCasesSearchResults() throws Exception {
    final int page = 0;
    final int size = 10;
    final List<BaseApplicationDetail> caseSearchResults = Arrays.asList(new BaseApplicationDetail(), new BaseApplicationDetail());
    final ApplicationDetails linkedCaseSearchResults = new ApplicationDetails();

    when(applicationMapper.toApplicationDetails(any())).thenReturn(linkedCaseSearchResults);

    this.mockMvc.perform(get("/application/sections/linked-cases/search/results")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size))
            .flashAttr(CASE_SEARCH_RESULTS, caseSearchResults))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-search-results"))
        .andExpect(model().attribute(CASE_RESULTS_PAGE, linkedCaseSearchResults));

    verify(applicationMapper, times(1)).toApplicationDetails(any());
  }

  @Test
  public void testAddLinkedCaseGet_Success() throws Exception {
    final String caseReferenceId = "123456789";
    final ApplicationDetails linkedCaseSearchResults = new ApplicationDetails();
    final List<BaseApplicationDetail> applications = new ArrayList<>();
    final BaseApplicationDetail baseApplication = new BaseApplicationDetail();
    baseApplication.setCaseReferenceNumber(caseReferenceId);
    applications.add(baseApplication);
    linkedCaseSearchResults.setContent(applications);
    final LinkedCaseResultRowDisplay linkedCaseResultRowDisplay = new LinkedCaseResultRowDisplay();

    when(resultDisplayMapper.toLinkedCaseResultRowDisplay(baseApplication)).thenReturn(linkedCaseResultRowDisplay);
    when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE)).thenReturn(Mono.just(mockCommonLookupDetail));

    this.mockMvc.perform(get("/application/sections/linked-cases/{case-reference-id}/add", caseReferenceId)
            .sessionAttr(CASE_RESULTS_PAGE, linkedCaseSearchResults))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-add"))
        .andExpect(model().attribute("currentLinkedCase", linkedCaseResultRowDisplay));

    verify(resultDisplayMapper, times(1)).toLinkedCaseResultRowDisplay(baseApplication);
  }

  @Test
  public void testAddLinkedCasePost_Success() throws Exception {
    final String applicationId = "app123";
    final UserDetail user = new UserDetail();
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

    this.mockMvc.perform(post("/application/sections/linked-cases/add")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("currentLinkedCase", linkedCase))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections/linked-cases"));

    verify(linkedCaseValidator, times(1)).validate(eq(linkedCase), any(BindingResult.class));
    verify(applicationService, times(1)).addLinkedCase(eq(applicationId), eq(linkedCase), eq(user));
  }

  @Test
  public void testAddLinkedCasePost_ValidationError() throws Exception {
    final String applicationId = "app123";
    final UserDetail user = new UserDetail();
    final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

    doAnswer(invocation -> {
      final BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("relationToCase", "required.relationToCase",
          "Please complete 'How is this application / case related to your application?'.");
      return null;
    }).when(linkedCaseValidator).validate(eq(linkedCase), any(BindingResult.class));

    when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE)).thenReturn(Mono.just(mockCommonLookupDetail));

    // When & Then
    this.mockMvc.perform(post("/application/sections/linked-cases/add")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(USER_DETAILS, user)
            .flashAttr("currentLinkedCase", linkedCase))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-linked-case-add"))
        .andExpect(model().attributeHasFieldErrors("currentLinkedCase", "relationToCase"));

    verify(applicationService, never()).addLinkedCase(anyString(), any(LinkedCaseResultRowDisplay.class), any(UserDetail.class));
  }


}