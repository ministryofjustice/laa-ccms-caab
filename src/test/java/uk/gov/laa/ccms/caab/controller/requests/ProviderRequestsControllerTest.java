package uk.gov.laa.ccms.caab.controller.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypesValidator;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.constants.ProviderRequestFlowType;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderRequestService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class ProviderRequestsControllerTest {

  private MockMvc mockMvc;

  @Mock private LookupService lookupService;

  @Mock private EvidenceService evidenceService;

  @Mock private ProviderRequestService providerRequestService;

  @Mock private ProviderRequestTypesValidator providerRequestTypeValidator;

  @Mock private ProviderRequestDetailsValidator providerRequestDetailsValidator;

  @Mock private ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

  @Mock private AvScanService avScanService;

  @Mock private ProviderRequestsMapper mapper;

  @Mock private Model model;

  @InjectMocks private ProviderRequestsController providerRequestsController;

  private static final String MAX_FILE_SIZE = String.valueOf(5L * 1024 * 1024);
  private static final String PROVIDER_REQUEST_SUBMIT_URL = "providerRequestSubmitUrl";
  private static final String PROVIDER_REQUEST_BACK_URL = "providerRequestBackUrl";
  private static final String PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_ATTRIBUTE =
      "providerRequestEvidenceUploadFormAttribute";

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerRequestsController).build();
  }

  private static final UserDetail userDetails = buildUserDetail();

  private static final ActiveCase activeCase =
      ActiveCase.builder().caseReferenceNumber("testCaseReferenceNumber").build();

  /** Creates a populated ProviderRequestFlowFormData with a specific request type. */
  private ProviderRequestFlowFormData createProviderRequestFlow(final String requestType) {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType(requestType);
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    return providerRequestFlow;
  }

  private ProviderRequestFlowFormData createFlowWithCaseRef(String caseReferenceNumber) {
    ProviderRequestFlowFormData flow = new ProviderRequestFlowFormData();
    flow.setCaseReferenceNumber(caseReferenceNumber);
    return flow;
  }

  /**
   * Mocks the lookupService to return a ProviderRequestTypeLookupDetail containing a single dynamic
   * form with lookup items.
   */
  private void mockLookupServiceWithDynamicForm(
      final ProviderRequestTypeLookupValueDetail dynamicForm,
      final List<ProviderRequestDataLookupValueDetail> lookupItems) {
    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    dynamicForm.setDataItems(lookupItems);
    lookupDetail.setContent(List.of(dynamicForm));
    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
  }

  /** Creates a ProviderRequestTypeLookupValueDetail with default configuration. */
  private ProviderRequestTypeLookupValueDetail createDefaultDynamicForm() {
    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);
    dynamicForm.setClaimUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");
    return dynamicForm;
  }

  /** Creates a mock lookup item with specified code and lookup type. */
  private ProviderRequestDataLookupValueDetail createLookupItem(
      final String code, final String lovLookupType) {
    final ProviderRequestDataLookupValueDetail lookupItem =
        new ProviderRequestDataLookupValueDetail();
    lookupItem.setCode(code);
    lookupItem.setLovLookupType(lovLookupType);
    lookupItem.setType("LOV");
    return lookupItem;
  }

  private String buildExpectedUrl(
      final ProviderRequestFlowType flowType, final String path, final String caseRef) {
    String url = flowType.getBasePath() + path;
    if (flowType.isCaseScoped()) {
      url += "?caseReferenceNumber=" + caseRef;
    }
    return url;
  }

  /** Mocks AV scan service to throw an exception. */
  private void mockAvScanServiceToThrow() throws AvScanException {
    doThrow(new AvScanException("Virus alert"))
        .when(avScanService)
        .performAvScan(any(), any(), any(), any(), any(), any(InputStream.class));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1, /home", // general flow
    "CASE, 300001234567, /case/overview", // case flow
  })
  @DisplayName("GET general/case provider requests types should return provider request type view")
  void testGetRequestType(String requestType, String caseRef, String expectedBackUrl)
      throws Exception {
    when(lookupService.getProviderRequestTypes(anyBoolean(), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    final ResultActions resultActions =
        mockMvc
            .perform(get(requestUrl).sessionAttr(USER_DETAILS, userDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-type"))
            .andExpect(model().attributeExists("providerRequestTypeDetails"))
            .andExpect(model().attributeExists("providerRequestTypes"))
            .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL, providerRequestFlowType.getBasePath() + "/types"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    if (providerRequestFlowType.isCaseScoped()) {
      resultActions.andExpect(model().attribute("caseReference", caseRef));
    } else {
      resultActions.andExpect(model().attributeDoesNotExist("caseReference"));
    }

    verify(lookupService)
        .getProviderRequestTypes(eq(providerRequestFlowType.isCaseScoped()), isNull());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1", // general flow
    "CASE, 300001234567", // case flow
  })
  @DisplayName(
      "POST general/case provider requests types should redirect to provider request details on success")
  void testRequestTypePost_Success(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestTypeFormData providerRequestTypeDetails =
        new ProviderRequestTypeFormData();

    doAnswer(invocation -> null).when(providerRequestTypeValidator).validate(any(), any());

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    String expectedUrl = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    mockMvc
        .perform(
            post(providerRequestFlowType.getBasePath() + "/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedUrl));

    verify(providerRequestTypeValidator).validate(any(), any());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1, /home", // general flow
    "CASE, 300001234567, /case/overview", // case flow
  })
  @DisplayName(
      "POST general/case provider requests types should return form view with validation errors")
  void testRequestTypePost_HasValidationErrors(
      String requestType, String caseRef, String expectedBackUrl) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestTypeFormData providerRequestTypeDetails =
        new ProviderRequestTypeFormData();

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    doAnswer(
            invocation -> {
              final Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue(
                  "providerRequestType",
                  "required.providerRequestType",
                  "Please select a request type.");
              return null;
            })
        .when(providerRequestTypeValidator)
        .validate(any(), any());

    when(lookupService.getProviderRequestTypes(anyBoolean(), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    final ResultActions resultActions =
        mockMvc
            .perform(
                post(providerRequestFlowType.getBasePath() + "/types")
                    .sessionAttr(USER_DETAILS, userDetails)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                    .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-type"))
            .andExpect(
                model()
                    .attributeHasFieldErrors("providerRequestTypeDetails", "providerRequestType"))
            .andExpect(model().attributeExists(providerRequestFlowType.getFlowSessionAttribute()))
            .andExpect(model().attributeExists("providerRequestTypeDetails"))
            .andExpect(model().attributeExists("providerRequestTypes"));

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL, providerRequestFlowType.getBasePath() + "/types"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    verify(providerRequestTypeValidator).validate(any(), any());
    verify(lookupService)
        .getProviderRequestTypes(eq(providerRequestFlowType.isCaseScoped()), isNull());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("Should populate provider request types in the model for general/case flow")
  void testPopulateProviderRequestTypes(String requestType, String caseRef) throws Exception {
    final ProviderRequestTypeLookupValueDetail mockRequestType =
        new ProviderRequestTypeLookupValueDetail();
    final ProviderRequestTypeLookupDetail mockDetail = new ProviderRequestTypeLookupDetail();
    mockDetail.setContent(List.of(mockRequestType));
    final ProviderRequestFlowType providerRequestFlowType =
        ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    when(lookupService.getProviderRequestTypes(
            eq(providerRequestFlowType.isCaseScoped()), isNull()))
        .thenReturn(Mono.just(mockDetail));

    final ResultActions resultActions =
        mockMvc
            .perform(get(requestUrl).sessionAttr(USER_DETAILS, userDetails))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("providerRequestTypes"))
            .andExpect(model().attribute("providerRequestTypes", List.of(mockRequestType)));

    verify(lookupService)
        .getProviderRequestTypes(eq(providerRequestFlowType.isCaseScoped()), isNull());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName(
      "Should populate provider request types in the model based on user function codes for general/case flow")
  void testPopulateProviderRequestTypesBasedOnUserFunctionCodes(String requestType, String caseRef)
      throws Exception {
    final ProviderRequestTypeLookupValueDetail mockRequestType1 =
        new ProviderRequestTypeLookupValueDetail();
    mockRequestType1.setName("test1");
    mockRequestType1.setAccessFunctionCode("BU");

    final ProviderRequestTypeLookupValueDetail mockRequestType2 =
        new ProviderRequestTypeLookupValueDetail();
    mockRequestType2.setName("test2");
    mockRequestType2.setAccessFunctionCode("CR");

    final ProviderRequestTypeLookupValueDetail mockRequestType3 =
        new ProviderRequestTypeLookupValueDetail();
    mockRequestType3.setName("test3");

    final ProviderRequestTypeLookupDetail mockDetail = new ProviderRequestTypeLookupDetail();
    mockDetail.setContent(List.of(mockRequestType1, mockRequestType2, mockRequestType3));
    final ProviderRequestFlowType providerRequestFlowType =
        ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    when(lookupService.getProviderRequestTypes(
            eq(providerRequestFlowType.isCaseScoped()), isNull()))
        .thenReturn(Mono.just(mockDetail));

    final ResultActions resultActions =
        mockMvc
            .perform(get(requestUrl).sessionAttr(USER_DETAILS, userDetails.addFunctionsItem("CR")))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("providerRequestTypes"))
            .andExpect(
                model()
                    .attribute(
                        "providerRequestTypes", List.of(mockRequestType2, mockRequestType3)));

    verify(lookupService)
        .getProviderRequestTypes(eq(providerRequestFlowType.isCaseScoped()), isNull());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("Should handle empty provider request types in the model for general/case flow")
  void testPopulateProviderRequestTypes_Empty(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowType providerRequestFlowType =
        ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    when(lookupService.getProviderRequestTypes(
            eq(providerRequestFlowType.isCaseScoped()), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    final ResultActions resultActions =
        mockMvc
            .perform(get(requestUrl).sessionAttr(USER_DETAILS, userDetails))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("providerRequestTypes"))
            .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    verify(lookupService)
        .getProviderRequestTypes(eq(providerRequestFlowType.isCaseScoped()), isNull());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName(
      "Should return provider request detail view with populated model attributes for general/case flow")
  void testProviderRequestsDetails_PopulatesModel(String requestType, String caseRef) {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);
    dynamicForm.setClaimUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));

    final ProviderRequestFlowType providerRequestFlowType =
        ProviderRequestFlowType.valueOf(requestType);

    final String viewName =
        providerRequestsController.providerRequestsDetails(
            providerRequestFlow, providerRequestDetailsForm, model, providerRequestFlowType);

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    verify(lookupService).getProviderRequestTypes(null, "testType");
    verify(mapper).populateProviderRequestDetailsForm(providerRequestDetailsForm, dynamicForm);
    verify(model).addAttribute("providerRequestDynamicForm", dynamicForm);
    verify(model).addAttribute("providerRequestDetails", providerRequestDetailsForm);
    verify(model).addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
    verify(model)
        .addAttribute(
            PROVIDER_REQUEST_SUBMIT_URL, providerRequestFlowType.getBasePath() + "/details");
    verify(model).addAttribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl);
    assertEquals("requests/provider-request-detail", viewName);
    assertTrue(providerRequestDetailsForm.isClaimUploadEnabled());
    assertEquals("Upload Files", providerRequestDetailsForm.getClaimUploadLabel());
    assertEquals("Additional Info", providerRequestDetailsForm.getAdditionalInformationLabel());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName(
      "GET general/case provider request details should return detail view with populated model")
  void testRequestDetail_Get_PopulatesModel(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);
    dynamicForm.setClaimUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    when(providerRequestDetailsValidator.getValidExtensions()).thenReturn(List.of("xml"));
    when(providerRequestDetailsValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    final ResultActions resultActions =
        mockMvc
            .perform(
                get(requestUrl)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-detail"))
            .andExpect(model().attributeExists("documentTypes"))
            .andExpect(model().attributeExists("maxFileSize"))
            .andExpect(model().attributeExists("validExtensions"))
            .andExpect(model().attributeExists("claimMaxFileSize"))
            .andExpect(model().attributeExists("claimValidExtensions"))
            .andExpect(model().attributeExists("providerRequestDynamicForm"))
            .andExpect(model().attribute("providerRequestDynamicForm", dynamicForm))
            .andExpect(model().attributeExists("providerRequestDetails"))
            .andExpect(model().attribute("providerRequestDetails", providerRequestDetailsForm))
            .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA))
            .andExpect(model().attribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow));

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/details"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    if (providerRequestFlowType.isCaseScoped()) {
      resultActions.andExpect(model().attribute("caseReference", caseRef));
    } else {
      resultActions.andExpect(model().attributeDoesNotExist("caseReference"));
    }

    verify(lookupService).getProviderRequestTypes(null, "testType");
    verify(mapper).populateProviderRequestDetailsForm(providerRequestDetailsForm, dynamicForm);
  }

  @Test
  @DisplayName("Should populate model with lookup dropdown values for provider request details")
  void testPopulateProviderRequestDetailsLookupDropdowns() {
    final ProviderRequestTypeLookupValueDetail providerRequestType =
        new ProviderRequestTypeLookupValueDetail();

    final ProviderRequestDataLookupValueDetail lookupItem =
        new ProviderRequestDataLookupValueDetail();
    lookupItem.setCode("testCode");
    lookupItem.setLovLookupType("testLookupType");
    lookupItem.setType("LOV");

    providerRequestType.setDataItems(List.of(lookupItem));

    final List<CommonLookupValueDetail> commonValues =
        List.of(new CommonLookupValueDetail().code("value1").description("Label 1"));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(commonValues);

    when(lookupService.getCommonValues("testLookupType")).thenReturn(Mono.just(commonLookupDetail));

    providerRequestsController.populateProviderRequestDetailsLookupDropdowns(
        model, providerRequestType);

    verify(lookupService).getCommonValues("testLookupType");
    verify(model).addAttribute("testCode", commonValues);
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case request details should remove document and return detail view")
  void testRequestDetail_Post_documentDelete(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    providerRequestDetailsForm.setDocumentIdToDelete(123);
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    doNothing()
        .when(evidenceService)
        .removeDocument(anyString(), eq(123), eq(CcmsModule.REQUEST), eq(userDetails.getLoginId()));

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(false);
    dynamicForm.setAdditionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(evidenceService.getEvidenceDocumentsForApplicationOrOutcome(any(), eq(CcmsModule.REQUEST)))
        .thenReturn(Mono.just(new EvidenceDocumentDetails()));

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    final ResultActions resultActions =
        mockMvc
            .perform(
                post(providerRequestFlowType.getBasePath() + "/details")
                    .sessionAttr(USER_DETAILS, userDetails)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                    .param("action", "document_delete")
                    .flashAttr("providerRequestDetails", providerRequestDetailsForm))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-detail"));

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/details"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    verify(evidenceService)
        .removeDocument(anyString(), eq(123), eq(CcmsModule.REQUEST), eq(userDetails.getLoginId()));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case request details should return detail view with validation errors")
  void testRequestDetail_Post_validationErrors(String requestType, String caseRef)
      throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);

    doAnswer(
            invocation -> {
              final Errors errors = invocation.getArgument(1);
              errors.rejectValue("additionalInformation", "error.code", "Validation error");
              return null;
            })
        .when(providerRequestDetailsValidator)
        .validate(any(), any());

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail()
            .isClaimUploadEnabled(false)
            .additionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(evidenceService.getEvidenceDocumentsForApplicationOrOutcome(any(), eq(CcmsModule.REQUEST)))
        .thenReturn(Mono.just(new EvidenceDocumentDetails()));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    when(providerRequestDetailsValidator.getValidExtensions()).thenReturn(List.of("xml"));
    when(providerRequestDetailsValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    final ResultActions resultActions =
        mockMvc
            .perform(
                post(providerRequestFlowType.getBasePath() + "/details")
                    .sessionAttr(USER_DETAILS, userDetails)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                    .flashAttr("providerRequestDetails", providerRequestDetailsForm)
                    .param("action", "submit"))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-detail"))
            .andExpect(model().attributeExists("documentTypes"))
            .andExpect(model().attributeExists("maxFileSize"))
            .andExpect(model().attributeExists("validExtensions"))
            .andExpect(model().attributeExists("claimMaxFileSize"))
            .andExpect(model().attributeExists("claimValidExtensions"))
            .andExpect(model().attributeExists("providerRequestDynamicForm"))
            .andExpect(model().attribute("providerRequestDynamicForm", dynamicForm))
            .andExpect(
                model().attributeHasFieldErrors("providerRequestDetails", "additionalInformation"))
            .andExpect(model().attributeExists("providerRequestDetails"))
            .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA));

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/details"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    verify(providerRequestDetailsValidator).validate(eq(providerRequestDetailsForm), any());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case request details should handle AV scan exception")
  void testRequestDetail_Post_avScanException(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestDetailsForm.setClaimUploadEnabled(true);
    providerRequestDetailsForm.setFile(
        new MockMultipartFile(
            "theFile", "originalName.ppp", "contentType", "the file data".getBytes()));

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail()
            .isClaimUploadEnabled(true)
            .additionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));

    doThrow(new AvScanException("Virus detected"))
        .when(avScanService)
        .performAvScan(any(), any(), any(), any(), any(), any(InputStream.class));

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    final ResultActions resultActions =
        mockMvc
            .perform(
                post(providerRequestFlowType.getBasePath() + "/details")
                    .sessionAttr(USER_DETAILS, userDetails)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                    .flashAttr("providerRequestDetails", providerRequestDetailsForm)
                    .param("action", "submit"))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-detail"))
            .andExpect(model().attributeExists("providerRequestDetails"))
            .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA));

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/types", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/details"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    verify(avScanService).performAvScan(any(), any(), any(), any(), any(), any(InputStream.class));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName(
      "GET general/case provider request documents should return upload view with populated model")
  void testAddDocumentsToRequestGet(String requestType, String caseRef) throws Exception {
    final String maxFileSize = String.valueOf(5L * 1024 * 1024);
    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));

    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(maxFileSize); // 5 MB

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    String requestUrl = buildExpectedUrl(providerRequestFlowType, "/documents", caseRef);

    final ResultActions resultActions =
        mockMvc
            .perform(
                get(requestUrl)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-doc-upload"))
            .andExpect(
                model()
                    .attributeExists(providerRequestFlowType.getEvidenceUploadSessionAttribute()))
            .andExpect(
                model()
                    .attribute(
                        PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_ATTRIBUTE,
                        providerRequestFlowType.getEvidenceUploadSessionAttribute()))
            .andExpect(model().attributeExists("documentTypes")) // Check documentTypes dropdown
            .andExpect(
                model()
                    .attribute(
                        "documentTypes",
                        List.of(
                            new CommonLookupValueDetail()
                                .code("DOC1")
                                .description("Document Type 1"))))
            .andExpect(
                model().attribute("validExtensions", "pdf or docx")) // Check valid file extensions
            .andExpect(model().attribute("maxFileSize", maxFileSize)); // Check max file size

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/documents"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    if (providerRequestFlowType.isCaseScoped()) {
      resultActions.andExpect(model().attribute("caseReference", caseRef));
    } else {
      resultActions.andExpect(model().attributeDoesNotExist("caseReference"));
    }

    // Verify interactions with mocks
    verify(lookupService).getCommonValues(COMMON_VALUE_DOCUMENT_TYPES);
    verify(providerRequestDocumentUploadValidator).getValidExtensions();
    verify(providerRequestDocumentUploadValidator).getMaxFileSize();
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case provider request documents should redirect to request details")
  void testAddDocumentsToRequestPost_success(String requestType, String caseRef) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    final MockMultipartFile mockFile =
        new MockMultipartFile("file", "testfile.txt", "text/plain", "Test content".getBytes());
    evidenceUploadFormData.setFile(mockFile);

    final EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();
    when(mapper.toProviderRequestDocumentDetail(eq(evidenceUploadFormData)))
        .thenReturn(evidenceDocumentDetail);
    when(evidenceService.addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId())))
        .thenReturn(Mono.just("Success"));

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    String expectedRedirect = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    // Perform the request
    mockMvc
        .perform(
            post(providerRequestFlowType.getBasePath() + "/documents")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                .flashAttr(
                    providerRequestFlowType.getEvidenceUploadSessionAttribute(),
                    evidenceUploadFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirect));

    // Verify interactions
    verify(mapper).toProviderRequestDocumentDetail(evidenceUploadFormData);
    verify(evidenceService).addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId()));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case provider request documents should handle validation errors")
  void testAddDocumentsToRequestPost_validationErrors(String requestType, String caseRef)
      throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();

    // Mock providerRequestDocumentUploadValidator behavior
    doAnswer(
            invocation -> {
              final Errors errors = invocation.getArgument(1);
              errors.rejectValue("file", "error.file.invalid", "Invalid file format");
              return null;
            })
        .when(providerRequestDocumentUploadValidator)
        .validate(any(), any());

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));

    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    final ResultActions resultActions =
        mockMvc
            .perform(
                post(providerRequestFlowType.getBasePath() + "/documents")
                    .sessionAttr(USER_DETAILS, userDetails)
                    .sessionAttr(
                        providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                    .flashAttr(
                        providerRequestFlowType.getEvidenceUploadSessionAttribute(),
                        evidenceUploadFormData))
            .andExpect(status().isOk())
            .andExpect(view().name("requests/provider-request-doc-upload"))
            .andExpect(
                model()
                    .attributeHasFieldErrors(
                        providerRequestFlowType.getEvidenceUploadSessionAttribute(), "file"))
            .andExpect(
                model()
                    .attribute(
                        PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_ATTRIBUTE,
                        providerRequestFlowType.getEvidenceUploadSessionAttribute()))
            .andExpect(model().attributeExists("documentTypes"))
            .andExpect(
                model()
                    .attribute(
                        "documentTypes",
                        List.of(
                            new CommonLookupValueDetail()
                                .code("DOC1")
                                .description("Document Type 1"))))
            .andExpect(model().attribute("validExtensions", "pdf or docx"))
            .andExpect(model().attribute("maxFileSize", MAX_FILE_SIZE));

    String expectedBackUrl = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    resultActions
        .andExpect(
            model()
                .attribute(
                    PROVIDER_REQUEST_SUBMIT_URL,
                    providerRequestFlowType.getBasePath() + "/documents"))
        .andExpect(model().attribute(PROVIDER_REQUEST_BACK_URL, expectedBackUrl));

    // Verify interactions with mocked dependencies
    verify(providerRequestDocumentUploadValidator).validate(eq(evidenceUploadFormData), any());
    verify(lookupService).getCommonValues(COMMON_VALUE_DOCUMENT_TYPES);
    verify(providerRequestDocumentUploadValidator).getValidExtensions();
    verify(providerRequestDocumentUploadValidator).getMaxFileSize();
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("Should pass sanitised filename to AV scan during document upload")
  void testPostDocuments_usesSanitisedFilenameForAvScan(String requestType, String caseRef)
      throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    final ProviderRequestFlowType providerRequestFlowType =
        ProviderRequestFlowType.valueOf(requestType);
    final MockMultipartFile mockFile =
        new MockMultipartFile(
            "file", "My interesting%filename!.pdf", "application/pdf", "Test content".getBytes());
    evidenceUploadFormData.setFile(mockFile);
    evidenceUploadFormData.setSanitisedFileName("My_interesting_filename_.pdf");

    final EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();
    when(mapper.toProviderRequestDocumentDetail(eq(evidenceUploadFormData)))
        .thenReturn(evidenceDocumentDetail);
    when(evidenceService.addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId())))
        .thenReturn(Mono.just("Success"));

    doAnswer(
            invocation -> {
              final EvidenceUploadFormData formData = invocation.getArgument(0);
              formData.setSanitisedFileName("My_interesting_filename_.pdf");
              formData.setFileExtension("pdf");
              return null;
            })
        .when(providerRequestDocumentUploadValidator)
        .validate(any(), any());

    String expectedRedirect = buildExpectedUrl(providerRequestFlowType, "/details", caseRef);

    mockMvc
        .perform(
            post(providerRequestFlowType.getBasePath() + "/documents")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                .flashAttr(
                    providerRequestFlowType.getEvidenceUploadSessionAttribute(),
                    evidenceUploadFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirect));

    verify(avScanService)
        .performAvScan(
            any(), any(), any(), any(), eq("My_interesting_filename_.pdf"), any(InputStream.class));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL", "CASE",
  })
  @DisplayName("Should not persist document when validator rejects problematic filename")
  void testPostDocuments_validationErrorPreventsPersistence(String requestType) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    evidenceUploadFormData.setFile(
        new MockMultipartFile(
            "file",
            "Test             Upload--  -- copyDoublespaces.rtf",
            "text/plain",
            "Test content".getBytes()));

    doAnswer(
            invocation -> {
              final Errors errors = invocation.getArgument(1);
              errors.rejectValue("file", "scan.failure", "Service error");
              return null;
            })
        .when(providerRequestDocumentUploadValidator)
        .validate(any(), any());

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "rtf"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    mockMvc
        .perform(
            post(providerRequestFlowType.getBasePath() + "/documents")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                .flashAttr(
                    providerRequestFlowType.getEvidenceUploadSessionAttribute(),
                    evidenceUploadFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-doc-upload"));

    verify(evidenceService, never()).addDocument(any(), anyString());
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1, /general-provider-requests/documents",
    "CASE, 123456789012, /case-provider-requests/documents?caseReferenceNumber=123456789012",
  })
  @DisplayName("POST general/case provider request details redirects to document upload by flow")
  void testRequestDetailPost_DocumentUploadRedirectByFlow(
      String requestType, String caseRef, String expectedRedirect) throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);

    mockMvc
        .perform(
            post(providerRequestFlowType.getBasePath() + "/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(providerRequestFlowType.getFlowSessionAttribute(), providerRequestFlow)
                .param("action", "document_upload")
                .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirect));

    verify(mapper)
        .toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName("POST general/case submit provider request uses flow-specific case reference")
  void testSubmitProviderRequest_UsesFlowSpecificCaseReference(String requestType, String caseRef) {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    providerRequestFlow.setRequestTypeFormData(new ProviderRequestTypeFormData());
    providerRequestFlow.setRequestDetailsFormData(new ProviderRequestDetailsFormData());

    final ProviderRequestDetailsFormData details = new ProviderRequestDetailsFormData();
    details.setClaimUploadEnabled(false);

    final UserDetail user = new UserDetail();
    final Model model = new ExtendedModelMap();
    final BindingResult binding = new BeanPropertyBindingResult(details, "details");
    final MockHttpSession session = new MockHttpSession();

    when(providerRequestService.submitProviderRequest(any(), any(), any(), any())).thenReturn("id");

    when(evidenceService.getEvidenceDocumentsForApplicationOrOutcome(any(), any()))
        .thenReturn(Mono.just(new EvidenceDocumentDetails().content(List.of())));

    when(evidenceService.uploadAndUpdateDocuments(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    ProviderRequestFlowType providerRequestFlowType = ProviderRequestFlowType.valueOf(requestType);
    final String viewName;
    if (providerRequestFlowType.isCaseScoped()) {
      viewName =
          providerRequestsController.postCaseRequestDetail(
              user, providerRequestFlow, "submit", details, model, binding, session);
    } else {
      viewName =
          providerRequestsController.postGeneralRequestDetail(
              user, providerRequestFlow, "submit", details, model, binding, session);
    }

    verify(providerRequestService)
        .submitProviderRequest(
            any(ProviderRequestTypeFormData.class),
            any(ProviderRequestDetailsFormData.class),
            eq(caseRef),
            any(UserDetail.class));
    final String expectedViewName =
        providerRequestFlowType.isCaseScoped()
            ? "redirect:/application/submit-case-provider-request/confirmed?caseReferenceNumber="
                + caseRef
            : "redirect:/application/submit-general-provider-request/confirmed";
    assertEquals(expectedViewName, viewName);
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1",
    "CASE, 123456789012",
  })
  @DisplayName(
      "MaxUploadSizeExceededException handler returns upload form with flow-specific form,"
          + " binding error, and return URLs for general/case flow")
  void testHandleUploadFileTooLarge(String requestType, String caseRef) {
    final ProviderRequestFlowType flowType = ProviderRequestFlowType.valueOf(requestType);
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef(caseRef);
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();

    final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setServletPath(flowType.getBasePath() + "/documents");

    final MockHttpSession session = new MockHttpSession();
    session.setAttribute(flowType.getFlowSessionAttribute(), providerRequestFlow);
    session.setAttribute(flowType.getEvidenceUploadSessionAttribute(), evidenceUploadFormData);

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    doAnswer(
            invocation -> {
              final Errors errors = invocation.getArgument(0);
              errors.rejectValue("file", "error.file.size", "File too large");
              return null;
            })
        .when(providerRequestDocumentUploadValidator)
        .rejectFileSize(any(Errors.class));

    final ExtendedModelMap model = new ExtendedModelMap();

    final String viewName =
        providerRequestsController.handleUploadFileTooLarge(servletRequest, session, model);

    assertEquals("requests/provider-request-doc-upload", viewName);

    // Assert the correct flow-specific evidence upload form attribute name is in the model
    assertEquals(
        flowType.getEvidenceUploadSessionAttribute(),
        model.get(PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_ATTRIBUTE));
    assertEquals(evidenceUploadFormData, model.get(flowType.getEvidenceUploadSessionAttribute()));

    // Assert binding result is present and carries the file-size error
    final BindingResult bindingResult =
        (BindingResult)
            model.get(
                BindingResult.MODEL_KEY_PREFIX + flowType.getEvidenceUploadSessionAttribute());
    assertNotNull(bindingResult);
    assertTrue(bindingResult.hasErrors());

    // Assert flow-specific submit and back URLs are correct
    assertEquals(flowType.getBasePath() + "/documents", model.get(PROVIDER_REQUEST_SUBMIT_URL));
    assertEquals(
        buildExpectedUrl(flowType, "/details", caseRef), model.get(PROVIDER_REQUEST_BACK_URL));

    verify(providerRequestDocumentUploadValidator).rejectFileSize(any(Errors.class));
  }

  @ParameterizedTest
  @CsvSource({
    "GENERAL, -1, /home",
    "CASE, 123456789012, /case/overview",
  })
  @DisplayName(
      "POST general/case provider request confirmed clears flow-specific session attributes,"
          + " leaves the other flow intact, and redirects to /home vs /case/overview")
  void testProviderRequestSubmitted(String requestType, String caseRef, String expectedRedirect)
      throws Exception {
    final ProviderRequestFlowType flowType = ProviderRequestFlowType.valueOf(requestType);
    final ProviderRequestFlowType otherFlow =
        flowType == ProviderRequestFlowType.GENERAL
            ? ProviderRequestFlowType.CASE
            : ProviderRequestFlowType.GENERAL;

    final ProviderRequestFlowFormData generalFlow = createFlowWithCaseRef("-1");
    final ProviderRequestFlowFormData caseFlow = createFlowWithCaseRef(caseRef);
    final EvidenceUploadFormData generalEvidenceUpload = new EvidenceUploadFormData();
    final EvidenceUploadFormData caseEvidenceUpload = new EvidenceUploadFormData();

    final String confirmUrl =
        "/application/submit-%s-provider-request/confirmed"
            .formatted(flowType.isCaseScoped() ? "case" : "general");

    final MvcResult result =
        mockMvc
            .perform(
                post(confirmUrl)
                    .sessionAttr(SUBMISSION_RESULT, "confirmed")
                    .sessionAttr(
                        ProviderRequestFlowType.GENERAL.getFlowSessionAttribute(), generalFlow)
                    .sessionAttr(
                        ProviderRequestFlowType.GENERAL.getEvidenceUploadSessionAttribute(),
                        generalEvidenceUpload)
                    .sessionAttr(ProviderRequestFlowType.CASE.getFlowSessionAttribute(), caseFlow)
                    .sessionAttr(
                        ProviderRequestFlowType.CASE.getEvidenceUploadSessionAttribute(),
                        caseEvidenceUpload))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(expectedRedirect))
            .andReturn();

    final HttpSession session = result.getRequest().getSession(false);

    // SUBMISSION_RESULT and the selected flow's session attributes must be cleared
    assertNull(session.getAttribute(SUBMISSION_RESULT));
    assertNull(session.getAttribute(flowType.getFlowSessionAttribute()));
    assertNull(session.getAttribute(flowType.getEvidenceUploadSessionAttribute()));

    // The other flow's session attributes must remain intact
    assertNotNull(session.getAttribute(otherFlow.getFlowSessionAttribute()));
    assertNotNull(session.getAttribute(otherFlow.getEvidenceUploadSessionAttribute()));
  }

  @Test
  @DisplayName("GET /general-provider-requests/types disregards caseReferenceNumber")
  void testInvalidCaseReference() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(
            get(ProviderRequestFlowType.GENERAL.getBasePath() + "/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .param("caseReferenceNumber", "123456789012"))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("caseReference"));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }
}
