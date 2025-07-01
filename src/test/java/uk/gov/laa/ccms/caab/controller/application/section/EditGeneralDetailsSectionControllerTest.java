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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_LINK_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.controller.application.section.EditGeneralDetailsSectionController.CASE_RESULTS_PAGE;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
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
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.AmendmentService;
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

  @Mock private ApplicationService applicationService;

  @Mock
  private AmendmentService amendmentService;

  @Mock private AddressService addressService;

  @Mock private ProviderService providerService;
  @Mock private LookupService lookupService;

  @Mock private CaseSearchCriteriaValidator searchCriteriaValidator;

  @Mock private FindAddressValidator findAddressValidator;

  @Mock private AddressSearchValidator addressSearchValidator;

  @Mock private LinkedCaseValidator linkedCaseValidator;

  @Mock private CorrespondenceAddressValidator correspondenceAddressValidator;

  @Mock private EbsApplicationMapper applicationMapper;

  @Mock private ResultDisplayMapper resultDisplayMapper;

  @InjectMocks private EditGeneralDetailsSectionController editGeneralDetailsSectionController;

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  private CommonLookupDetail mockCommonLookupDetail;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(editGeneralDetailsSectionController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setConversionService(getConversionService())
            .build();
    mockCommonLookupDetail = new CommonLookupDetail();
    mockCommonLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Nested
  @DisplayName("GET: /{caseContext}/sections/correspondence-address")
  class GetCorrespondenceAddressTests {

    @Test
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult() throws Exception {
      final String applicationId = "123";
      final AddressFormData addressFormData = new AddressFormData();

      when(lookupService.getCountries()).thenReturn(Mono.just(mockCommonLookupDetail));
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      when(applicationService.getCorrespondenceAddressFormData(applicationId))
          .thenReturn(addressFormData);

      mockMvc
          .perform(
              get("/application/sections/correspondence-address")
                  .sessionAttr(APPLICATION_ID, applicationId))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/correspondence-address-details"))
          .andExpect(model().attribute("addressDetails", addressFormData));

      verify(applicationService, times(1)).getCorrespondenceAddressFormData(applicationId);
    }

    @Test
    @DisplayName("Should populate session data")
    void shouldPopulateSessionData() throws Exception {
      final String applicationId = "123";
      final AddressFormData addressFormData = new AddressFormData();

      when(lookupService.getCountries()).thenReturn(Mono.just(mockCommonLookupDetail));
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              get("/application/sections/correspondence-address")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr("addressDetails", addressFormData))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/correspondence-address-details"))
          .andExpect(model().attribute("addressDetails", addressFormData));

      verify(applicationService, never()).getCorrespondenceAddressFormData(applicationId);
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/application/sections/correspondence-address")
  class PostCorrespondenceAddressTests {

    @Test
    @DisplayName("Should redirect to linked cases")
    void shouldRedirectToLinkedCases() throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final AddressFormData addressDetails = new AddressFormData();
      final ApplicationDetail ebsCase = new ApplicationDetail();

      mockMvc
          .perform(
              post("/application/sections/correspondence-address")
                  .param("action", "update")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(redirectedUrl("/application/sections/linked-cases"));

      verify(applicationService, times(1))
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
      verify(addressService, never()).getAddresses(any());
    }

    @Test
    @DisplayName("Should redirect to amendments submission")
    void shouldRedirectToAmendmentsSubmission() throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final AddressFormData addressDetails = new AddressFormData();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setCaseReferenceNumber("123456789");
      when(amendmentService.submitQuickAmendmentCorrespondenceAddress(addressDetails,
          "123456789", user)).thenReturn("12345");

      mockMvc.perform(post("/amendments/sections/correspondence-address")
              .param("action", "update")
              .sessionAttr(APPLICATION_ID, applicationId)
              .sessionAttr(CASE, ebsCase)
              .sessionAttr(USER_DETAILS, user)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(redirectedUrl("/amendments/%s".formatted(SUBMISSION_SUBMIT_CASE)));

      verify(applicationService, times(1)).updateCorrespondenceAddress(
          applicationId, addressDetails, user);
      verify(addressService, never()).getAddresses(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should redirect to find address screen")
    void shouldRedirectToFindAddressScreen(String caseContext) throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final AddressFormData addressDetails = new AddressFormData();
      final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<>();
      final ApplicationDetail ebsCase = new ApplicationDetail();

      addressSearchResults.setContent(Collections.singletonList(new AddressResultRowDisplay()));

      when(addressService.getAddresses(addressDetails.getPostcode()))
          .thenReturn(addressSearchResults);

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address".formatted(caseContext))
                  .param("action", "find_address")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(
              redirectedUrl("/%s/sections/correspondence-address/search".formatted(caseContext)));

      verify(addressService, times(1)).getAddresses(any());
      verify(addressService, times(1)).filterByHouseNumber(any(), any());
      verify(applicationService, never())
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Find address should redirect back when no results")
    void findAddressShouldRedirectBackWhenNoResults(String caseContext) throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final AddressFormData addressDetails = new AddressFormData();
      final ResultsDisplay<AddressResultRowDisplay> addressSearchResults = new ResultsDisplay<>();
      final ApplicationDetail ebsCase = new ApplicationDetail();

      when(addressService.getAddresses(addressDetails.getPostcode()))
          .thenReturn(addressSearchResults);

      when(lookupService.getCountries()).thenReturn(Mono.just(mockCommonLookupDetail));
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address".formatted(caseContext))
                  .param("action", "find_address")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(view().name("application/sections/correspondence-address-details"));

      verify(addressService, times(1)).getAddresses(any());
      verify(addressService, never()).filterByHouseNumber(any(), any());
      verify(applicationService, never())
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should have validation errors")
    void shouldHaveValidationErrors(String caseContext) throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final AddressFormData addressDetails = new AddressFormData();
      final ApplicationDetail ebsCase = new ApplicationDetail();

      when(lookupService.getCountries()).thenReturn(Mono.just(mockCommonLookupDetail));
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      doAnswer(
              invocation -> {
                final Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue(
                    "preferredAddress",
                    "required.preferredAddress",
                    "Please select an Preferred address.");
                return null;
              })
          .when(correspondenceAddressValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address".formatted(caseContext))
                  .param("action", "update")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(view().name("application/sections/correspondence-address-details"));

      verify(applicationService, never())
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
      verify(addressService, never()).getAddresses(any());
    }

    @Test
    @DisplayName("Should contain no validation errors max lengths not exceeded")
    void correspondenceAddressPostNoValidationErrorsMaxLengthsNotExceeded() throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final AddressFormData addressDetails = new AddressFormData();
      addressDetails.setHouseNameNumber(RandomStringUtils.insecure().nextAlphabetic(35));
      addressDetails.setPostcode(RandomStringUtils.insecure().nextAlphabetic(15));
      addressDetails.setCareOf(RandomStringUtils.insecure().nextAlphabetic(35));
      addressDetails.setAddressLine1(RandomStringUtils.insecure().nextAlphabetic(70));
      addressDetails.setAddressLine2(RandomStringUtils.insecure().nextAlphabetic(35));
      addressDetails.setCityTown(RandomStringUtils.insecure().nextAlphabetic(35));
      addressDetails.setCounty(RandomStringUtils.insecure().nextAlphabetic(35));

      mockMvc
          .perform(
              post("/application/sections/correspondence-address")
                  .param("action", "update")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(redirectedUrl("/application/sections/linked-cases"));

      verify(applicationService, times(1))
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
      verify(addressService, never()).getAddresses(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should contain validation errors max lengths exceeded")
    void correspondenceAddressPostValidationErrorsMaxLengthsExceeded(String caseContext)
        throws Exception {
      final String applicationId = "123";
      final UserDetail user = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final AddressFormData addressDetails = new AddressFormData();
      addressDetails.setHouseNameNumber(RandomStringUtils.insecure().nextAlphabetic(36));
      addressDetails.setPostcode(RandomStringUtils.insecure().nextAlphabetic(16));
      addressDetails.setCareOf(RandomStringUtils.insecure().nextAlphabetic(36));
      addressDetails.setAddressLine1(RandomStringUtils.insecure().nextAlphabetic(71));
      addressDetails.setAddressLine2(RandomStringUtils.insecure().nextAlphabetic(36));
      addressDetails.setCityTown(RandomStringUtils.insecure().nextAlphabetic(36));
      addressDetails.setCounty(RandomStringUtils.insecure().nextAlphabetic(36));

      when(lookupService.getCountries()).thenReturn(Mono.just(mockCommonLookupDetail));
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address".formatted(caseContext))
                  .param("action", "update")
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(CASE, ebsCase)
              .flashAttr("addressDetails", addressDetails))
          .andDo(print())
          .andExpect(view().name("application/sections/correspondence-address-details"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "houseNameNumber"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "postcode"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "careOf"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "addressLine1"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "addressLine2"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "county"))
          .andExpect(model().attributeHasFieldErrors("addressDetails", "cityTown"));

      verify(applicationService, never())
          .updateCorrespondenceAddress(applicationId, addressDetails, user);
      verify(addressService, never()).getAddresses(any());
    }
  }

  @Nested
  @DisplayName("GET: /{caseContext}/sections/correspondence-address/search")
  class GetSearchResultsTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

      mockMvc
          .perform(
              get("/%s/sections/correspondence-address/search".formatted(caseContext))
                  .sessionAttr(ADDRESS_SEARCH_RESULTS, results))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/address-search-results"))
          .andExpect(
              model()
                  .attribute(
                      "formAction",
                      "%s/sections/correspondence-address/search".formatted(caseContext)))
          .andExpect(model().attribute("addressSearchResults", results));
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/application/sections/correspondence-address/search")
  class PostSearchResultsTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address/search".formatted(caseContext))
                  .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
                  .sessionAttr("addressDetails", new AddressFormData()))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/%s/sections/correspondence-address".formatted(caseContext)));

      verify(addressService, times(1)).filterAndUpdateAddressFormData(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should have validation errors")
    void shouldHaveValidationErrors(String caseContext) throws Exception {
      final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();

      doAnswer(
              invocation -> {
                final Errors errors = (Errors) invocation.getArguments()[1];
                errors.reject("required.uprn", "Please select an address");
                return null;
              })
          .when(addressSearchValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post("/%s/sections/correspondence-address/search".formatted(caseContext))
                  .sessionAttr(ADDRESS_SEARCH_RESULTS, results)
                  .sessionAttr("addressDetails", new AddressFormData()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/address-search-results"))
          .andExpect(
              model()
                  .attribute(
                      "formAction",
                      "%s/sections/correspondence-address/search".formatted(caseContext)))
          .andExpect(model().attribute("addressSearchResults", results));

      verify(addressService, never()).filterAndUpdateAddressFormData(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("GET: /application/sections/linked-cases")
  class GetLinkedCasesTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final String applicationId = "123";
      final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();

      when(applicationService.getLinkedCases(applicationId)).thenReturn(linkedCases);

      mockMvc
          .perform(
              get("/%s/sections/linked-cases".formatted(caseContext))
                  .sessionAttr(APPLICATION_ID, applicationId))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-summary"))
          .andExpect(model().attribute("linkedCases", linkedCases));

      verify(applicationService, times(1)).getLinkedCases(applicationId);
    }
  }

  @Nested
  @DisplayName("GET: /application/sections/linked-cases/{linked-case-id}/remove")
  class GetRemoveLinkedCaseTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final Integer linkedCaseId = 1;
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();
      linkedCase.setId(linkedCaseId);

      final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();
      linkedCases.setContent(Collections.singletonList(linkedCase));

      mockMvc
          .perform(
              get(
                      "/%s/sections/linked-cases/{linked-case-id}/remove".formatted(caseContext),
                      linkedCaseId)
                  .sessionAttr("linkedCases", linkedCases))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-remove"))
          .andExpect(model().attribute("linkedCase", linkedCase));
    }
  }

  @Nested
  @DisplayName("POST: /application/sections/linked-cases/{linked-case-id}/remove")
  class PostRemoveLinkedCaseTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final String applicationId = "123";
      final String linkedCaseId = "1";
      final UserDetail user = new UserDetail();

      mockMvc
          .perform(
              post(
                      "/%s/sections/linked-cases/{linked-case-id}/remove".formatted(caseContext),
                      linkedCaseId)
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/%s/sections/linked-cases".formatted(caseContext)));

      verify(applicationService, times(1)).removeLinkedCase(linkedCaseId, user);
    }
  }

  @Nested
  @DisplayName("GET: /application/sections/linked-cases/{linked-case-id}/confirm")
  class GetConfirmLinkedCaseTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final Integer linkedCaseId = 1;
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();
      linkedCase.setId(linkedCaseId);

      final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases = new ResultsDisplay<>();
      linkedCases.setContent(Collections.singletonList(linkedCase));

      when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              get(
                      "/%s/sections/linked-cases/{linked-case-id}/confirm".formatted(caseContext),
                      linkedCaseId)
                  .sessionAttr("linkedCases", linkedCases))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-confirm"))
          .andExpect(model().attribute("currentLinkedCase", linkedCase));
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/sections/linked-cases/{linked-case-id}/confirm")
  class PostConfirmLinkedCaseTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final String applicationId = "123";
      final String linkedCaseId = "1";
      final UserDetail user = new UserDetail();
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

      mockMvc
          .perform(
              post(
                      "/%s/sections/linked-cases/{linked-case-id}/confirm".formatted(caseContext),
                      linkedCaseId)
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .flashAttr("linkedCase", linkedCase))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/%s/sections/linked-cases".formatted(caseContext)));

      verify(applicationService, times(1)).updateLinkedCase(linkedCaseId, linkedCase, user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors(String caseContext) throws Exception {
      final String applicationId = "123";
      final String linkedCaseId = "1";
      final UserDetail user = new UserDetail();
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

      when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      doAnswer(
              invocation -> {
                final Errors errors = (Errors) invocation.getArguments()[1];
                errors.reject("error.code", "Error message");
                return null;
              })
          .when(linkedCaseValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post(
                      "/%s/sections/linked-cases/{linked-case-id}/confirm".formatted(caseContext),
                      linkedCaseId)
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .flashAttr("linkedCase", linkedCase))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-confirm"));

      verify(applicationService, never()).updateLinkedCase(linkedCaseId, linkedCase, user);
    }
  }

  @Nested
  @DisplayName("GET: /application/sections/linked-cases/search")
  class GetLinkedCasesSearchTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final ProviderDetail mockProviderDetail = new ProviderDetail();
      final CaseStatusLookupDetail mockCaseStatusValues = new CaseStatusLookupDetail();

      when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS))
          .thenReturn(Mono.just(mockCommonLookupDetail));
      when(providerService.getProvider(any())).thenReturn(Mono.just(mockProviderDetail));
      when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(mockCaseStatusValues));

      mockMvc
          .perform(
              get("/%s/sections/linked-cases/search".formatted(caseContext))
                  .sessionAttr(USER_DETAILS, buildUserDetail()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-search"));
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/sections/linked-cases/search")
  class PostLinkedCasesSearchTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors(String caseContext) throws Exception {

      final ProviderDetail mockProviderDetail = new ProviderDetail();
      final CaseStatusLookupDetail mockCaseStatusValues = new CaseStatusLookupDetail();

      when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS))
          .thenReturn(Mono.just(mockCommonLookupDetail));
      when(providerService.getProvider(any())).thenReturn(Mono.just(mockProviderDetail));
      when(lookupService.getCaseStatusValues()).thenReturn(Mono.just(mockCaseStatusValues));

      final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
      doAnswer(
              invocation -> {
                final Errors errors = (Errors) invocation.getArguments()[1];
                errors.reject("error.code", "Error message");
                return null;
              })
          .when(searchCriteriaValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post("/%s/sections/linked-cases/search".formatted(caseContext))
                  .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
                  .sessionAttr(USER_DETAILS, buildUserDetail())
                  .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
                  .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-search"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle no search results")
    void shouldHandleNoSearchResults(String caseContext) throws Exception {
      final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
      when(applicationService.getCases(any(), any())).thenReturn(Collections.emptyList());

      mockMvc
          .perform(
              post("/%s/sections/linked-cases/search".formatted(caseContext))
                  .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
                  .sessionAttr(USER_DETAILS, buildUserDetail())
                  .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
                  .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-search-no-results"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle too many results")
    void shouldHandleTooManyResults(String caseContext) throws Exception {
      final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
      when(applicationService.getCases(any(), any()))
          .thenThrow(new TooManyResultsException("test"));

      mockMvc
          .perform(
              post("/%s/sections/linked-cases/search".formatted(caseContext))
                  .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
                  .sessionAttr(USER_DETAILS, buildUserDetail())
                  .sessionAttr("linkedCases", new ResultsDisplay<LinkedCaseResultRowDisplay>())
                  .flashAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(
              view().name("application/sections/application-linked-case-search-too-many-results"));
    }
  }

  @Nested
  @DisplayName("GET: /{caseContext}/sections/linked-cases/search/results")
  class GetLinkedCasesSearchResultsTests {
    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final int page = 0;
      final int size = 10;
      final List<BaseApplicationDetail> caseSearchResults =
          Arrays.asList(new BaseApplicationDetail(), new BaseApplicationDetail());
      final ApplicationDetails linkedCaseSearchResults = new ApplicationDetails();

      when(applicationMapper.toApplicationDetails(any())).thenReturn(linkedCaseSearchResults);

      mockMvc
          .perform(
              get("/%s/sections/linked-cases/search/results".formatted(caseContext))
                  .param("page", String.valueOf(page))
                  .param("size", String.valueOf(size))
                  .flashAttr(CASE_SEARCH_RESULTS, caseSearchResults))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-search-results"))
          .andExpect(model().attribute(CASE_RESULTS_PAGE, linkedCaseSearchResults));

      verify(applicationMapper, times(1)).toApplicationDetails(any());
    }
  }

  @Nested
  @DisplayName("GET: /{caseContext}/sections/linked-cases/{case-reference-id}/add")
  class GetAddLinkedCaseTests {
    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final String caseReferenceId = "123456789";
      final ApplicationDetails linkedCaseSearchResults = new ApplicationDetails();
      final List<BaseApplicationDetail> applications = new ArrayList<>();
      final BaseApplicationDetail baseApplication = new BaseApplicationDetail();
      baseApplication.setCaseReferenceNumber(caseReferenceId);
      applications.add(baseApplication);
      linkedCaseSearchResults.setContent(applications);
      final LinkedCaseResultRowDisplay linkedCaseResultRowDisplay =
          new LinkedCaseResultRowDisplay();

      when(resultDisplayMapper.toLinkedCaseResultRowDisplay(baseApplication))
          .thenReturn(linkedCaseResultRowDisplay);
      when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              get(
                      "/%s/sections/linked-cases/{case-reference-id}/add".formatted(caseContext),
                      caseReferenceId)
                  .sessionAttr(CASE_RESULTS_PAGE, linkedCaseSearchResults))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-add"))
          .andExpect(model().attribute("currentLinkedCase", linkedCaseResultRowDisplay));

      verify(resultDisplayMapper, times(1)).toLinkedCaseResultRowDisplay(baseApplication);
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/sections/linked-cases/add")
  class PostAddLinkedCaseTests {
    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult(String caseContext) throws Exception {
      final String applicationId = "app123";
      final UserDetail user = new UserDetail();
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

      mockMvc
          .perform(
              post("/%s/sections/linked-cases/add".formatted(caseContext))
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .flashAttr("currentLinkedCase", linkedCase))
          .andDo(print())
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/%s/sections/linked-cases".formatted(caseContext)));

      verify(linkedCaseValidator, times(1)).validate(eq(linkedCase), any(BindingResult.class));
      verify(applicationService, times(1))
          .addLinkedCase(eq(applicationId), eq(linkedCase), eq(user));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors(String caseContext) throws Exception {
      final String applicationId = "app123";
      final UserDetail user = new UserDetail();
      final LinkedCaseResultRowDisplay linkedCase = new LinkedCaseResultRowDisplay();

      doAnswer(
              invocation -> {
                final BindingResult bindingResult = invocation.getArgument(1);
                bindingResult.rejectValue(
                    "relationToCase",
                    "required.relationToCase",
                    "Please complete 'How is this application / case related to your application?'.");
                return null;
              })
          .when(linkedCaseValidator)
          .validate(eq(linkedCase), any(BindingResult.class));

      when(lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
          .thenReturn(Mono.just(mockCommonLookupDetail));

      mockMvc
          .perform(
              post("/%s/sections/linked-cases/add".formatted(caseContext))
                  .sessionAttr(APPLICATION_ID, applicationId)
                  .sessionAttr(USER_DETAILS, user)
                  .flashAttr("currentLinkedCase", linkedCase))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/application-linked-case-add"))
          .andExpect(model().attributeHasFieldErrors("currentLinkedCase", "relationToCase"));

      verify(applicationService, never())
          .addLinkedCase(anyString(), any(LinkedCaseResultRowDisplay.class), any(UserDetail.class));
    }
  }
}
