package uk.gov.laa.ccms.caab.controller.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypesValidator;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
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

  @BeforeEach
  public void setup() {
    // The active case advice is registered so the case context banner behaves as it does in the
    // running app: it is populated from the shared session attribute, not by this controller.
    mockMvc =
        standaloneSetup(providerRequestsController)
            .setControllerAdvice(new ActiveCaseModelAdvice())
            .build();
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

  /** Mocks AV scan service to throw an exception. */
  private void mockAvScanServiceToThrow() throws AvScanException {
    doThrow(new AvScanException("Virus alert"))
        .when(avScanService)
        .performAvScan(any(), any(), any(), any(), any(), any(InputStream.class));
  }

  @Test
  @DisplayName("GET /provider-requests/types should return provider request type view")
  void testGetRequestType() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(get("/provider-requests/types").sessionAttr(USER_DETAILS, userDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-type"))
        .andExpect(model().attributeExists("providerRequestTypeDetails"))
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName(
      "POST /provider-requests/types should redirect to provider request details on success")
  void testRequestTypePost_Success() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestTypeFormData providerRequestTypeDetails =
        new ProviderRequestTypeFormData();

    doAnswer(invocation -> null).when(providerRequestTypeValidator).validate(any(), any());

    mockMvc
        .perform(
            post("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/details"));

    verify(providerRequestTypeValidator).validate(any(), any());
  }

  @Test
  @DisplayName("POST /provider-requests/types should return form view with validation errors")
  void testRequestTypePost_HasValidationErrors() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestTypeFormData providerRequestTypeDetails =
        new ProviderRequestTypeFormData();

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

    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(
            post("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-type"))
        .andExpect(
            model().attributeHasFieldErrors("providerRequestTypeDetails", "providerRequestType"))
        .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA))
        .andExpect(model().attributeExists("providerRequestTypeDetails"))
        .andExpect(model().attributeExists("providerRequestTypes"));

    verify(providerRequestTypeValidator).validate(any(), any());
    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should populate provider request types in the model")
  void testPopulateProviderRequestTypes() throws Exception {
    final ProviderRequestTypeLookupValueDetail mockRequestType =
        new ProviderRequestTypeLookupValueDetail();
    final ProviderRequestTypeLookupDetail mockDetail = new ProviderRequestTypeLookupDetail();
    mockDetail.setContent(List.of(mockRequestType));

    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(mockDetail));

    mockMvc
        .perform(get("/provider-requests/types").sessionAttr(USER_DETAILS, userDetails))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", List.of(mockRequestType)));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should populate provider request types in the model based on user function codes")
  void testPopulateProviderRequestTypesBasedOnUserFunctionCodes() throws Exception {
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

    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(mockDetail));

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails.addFunctionsItem("CR")))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(
            model().attribute("providerRequestTypes", List.of(mockRequestType2, mockRequestType3)));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should handle empty provider request types in the model")
  void testPopulateProviderRequestTypes_Empty() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(get("/provider-requests/types").sessionAttr(USER_DETAILS, userDetails))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should return provider request detail view with populated model attributes")
  void testProviderRequestsDetails_PopulatesModel() {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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

    final String viewName =
        providerRequestsController.providerRequestsDetails(
            providerRequestFlow, providerRequestDetailsForm, model);

    verify(lookupService).getProviderRequestTypes(null, "testType");
    verify(mapper).populateProviderRequestDetailsForm(providerRequestDetailsForm, dynamicForm);
    verify(model).addAttribute("providerRequestDynamicForm", dynamicForm);
    verify(model).addAttribute("providerRequestDetails", providerRequestDetailsForm);
    verify(model).addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);

    assertEquals("requests/provider-request-detail", viewName);
    assertTrue(providerRequestDetailsForm.isClaimUploadEnabled());
    assertEquals("Upload Files", providerRequestDetailsForm.getClaimUploadLabel());
    assertEquals("Additional Info", providerRequestDetailsForm.getAdditionalInformationLabel());
  }

  @Test
  @DisplayName(
      "GET /provider-requests/details should return provider request detail view with populated model")
  void testGetRequestDetail_PopulatesModel() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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

    mockMvc
        .perform(
            get("/provider-requests/details")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow))
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

  @Test
  @DisplayName("Should redirect to document upload when action is 'document_upload'")
  void testPostRequestDetail_documentUpload() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);
    providerRequestFlow.setCaseReferenceNumber(null);

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail()
            .isClaimUploadEnabled(false)
            .additionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .param("action", "document_upload")
                .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/documents?providerRequestType=testType"));

    verify(mapper)
        .toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
  }

  @Test
  @DisplayName("Should remove document and return view name when action is 'document_delete'")
  void testPostRequestDetail_documentDelete() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .param("action", "document_delete")
                .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"));

    verify(evidenceService)
        .removeDocument(anyString(), eq(123), eq(CcmsModule.REQUEST), eq(userDetails.getLoginId()));
  }

  @Test
  @DisplayName("Should return provider request details view with validation errors")
  void testPostRequestDetail_validationErrors() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
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

    verify(providerRequestDetailsValidator).validate(eq(providerRequestDetailsForm), any());
  }

  @Test
  @DisplayName("Should handle AV scan exception and return provider request details view")
  void testPostRequestDetail_avScanException() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr("providerRequestDetails", providerRequestDetailsForm)
                .param("action", "submit"))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
        .andExpect(model().attributeExists("providerRequestDetails"))
        .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA));

    verify(avScanService).performAvScan(any(), any(), any(), any(), any(), any(InputStream.class));
  }

  @Test
  @DisplayName(
      "GET /provider-requests/documents should return document upload view with populated model")
  void testAddDocumentsToRequest() throws Exception {
    final String maxFileSize = String.valueOf(5L * 1024 * 1024);
    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    final ProviderRequestFlowFormData providerRequestFlow = createProviderRequestFlow("testType");
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));

    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(maxFileSize); // 5 MB

    mockMvc
        .perform(
            get("/provider-requests/documents")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-doc-upload"))
        .andExpect(
            model()
                .attributeExists(
                    EVIDENCE_UPLOAD_FORM_DATA)) // Check EvidenceUploadFormData is in the model
        .andExpect(model().attributeExists("documentTypes")) // Check documentTypes dropdown
        .andExpect(
            model()
                .attribute(
                    "documentTypes",
                    List.of(
                        new CommonLookupValueDetail().code("DOC1").description("Document Type 1"))))
        .andExpect(
            model().attribute("validExtensions", "pdf or docx")) // Check valid file extensions
        .andExpect(model().attribute("maxFileSize", maxFileSize)); // Check max file size

    // Verify interactions with mocks
    verify(lookupService).getCommonValues(COMMON_VALUE_DOCUMENT_TYPES);
    verify(providerRequestDocumentUploadValidator).getValidExtensions();
    verify(providerRequestDocumentUploadValidator).getMaxFileSize();
  }

  @Test
  @DisplayName("Should handle document upload and redirect to provider request details")
  void testPostDocuments_success() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createProviderRequestFlow("testType");
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    final MockMultipartFile mockFile =
        new MockMultipartFile("file", "testfile.txt", "text/plain", "Test content".getBytes());
    evidenceUploadFormData.setFile(mockFile);

    final EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();
    when(mapper.toProviderRequestDocumentDetail(eq(evidenceUploadFormData)))
        .thenReturn(evidenceDocumentDetail);
    when(evidenceService.addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId())))
        .thenReturn(Mono.just("Success"));

    // Perform the request
    mockMvc
        .perform(
            post("/provider-requests/documents")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData))
        .andExpect(status().is3xxRedirection())
        // A general enquiry carries no case reference, but must still carry its request type so
        // the details page it returns to is self-describing.
        .andExpect(redirectedUrl("/provider-requests/details?providerRequestType=testType"));

    // Verify interactions
    verify(mapper).toProviderRequestDocumentDetail(evidenceUploadFormData);
    verify(evidenceService).addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId()));
  }

  @Test
  @DisplayName("Should handle validation errors during document upload")
  void testPostDocuments_validationErrors() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createProviderRequestFlow("testType");
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

    mockMvc
        .perform(
            post("/provider-requests/documents")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-doc-upload"))
        .andExpect(model().attributeHasFieldErrors(EVIDENCE_UPLOAD_FORM_DATA, "file"))
        .andExpect(model().attributeExists("documentTypes"))
        .andExpect(
            model()
                .attribute(
                    "documentTypes",
                    List.of(
                        new CommonLookupValueDetail().code("DOC1").description("Document Type 1"))))
        .andExpect(model().attribute("validExtensions", "pdf or docx"))
        .andExpect(model().attribute("maxFileSize", MAX_FILE_SIZE));

    // Verify interactions with mocked dependencies
    verify(providerRequestDocumentUploadValidator).validate(eq(evidenceUploadFormData), any());
    verify(lookupService).getCommonValues(COMMON_VALUE_DOCUMENT_TYPES);
    verify(providerRequestDocumentUploadValidator).getValidExtensions();
    verify(providerRequestDocumentUploadValidator).getMaxFileSize();
  }

  @Test
  @DisplayName("GET /provider-requests/types with caseReferenceNumber should set caseReference")
  void testGetRequestType_withCaseReference_setsCaseReference() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(true), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .param("caseReferenceNumber", "123456789012"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("caseReference", "123456789012"));
  }

  @Test
  @DisplayName(
      "POST /provider-requests/types should redirect to /details preserving caseReferenceNumber")
  void testRequestTypePost_redirectsWithCaseReference() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef("123456789012");
    final ProviderRequestTypeFormData providerRequestTypeDetails =
        new ProviderRequestTypeFormData();
    providerRequestTypeDetails.setProviderRequestType("testType");

    doAnswer(invocation -> null).when(providerRequestTypeValidator).validate(any(), any());

    mockMvc
        .perform(
            post("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .param("caseReferenceNumber", "123456789012")
                .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/provider-requests/details?caseReferenceNumber=123456789012"
                    + "&providerRequestType=testType"));
  }

  @Test
  @DisplayName("Should include case ref number in redirect to doc upload if present")
  void testDocUpload_redirectsWithCaseReference() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef("123456789012");
    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail()
            .isClaimUploadEnabled(false)
            .additionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .param("action", "document_upload")
                .param("caseReferenceNumber", "123456789012")
                .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/provider-requests/documents?caseReferenceNumber=123456789012"
                    + "&providerRequestType=testType"));

    verify(mapper)
        .toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
  }

  @Test
  @DisplayName("Service should be called with caseReferenceNumber when provided")
  void testSubmitProviderRequest_callsServiceWithCaseRef() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createFlowWithCaseRef("123456789012");
    final ProviderRequestTypeFormData requestTypeFormData = new ProviderRequestTypeFormData();
    requestTypeFormData.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(requestTypeFormData);

    ProviderRequestDetailsFormData details = new ProviderRequestDetailsFormData();
    details.setClaimUploadEnabled(false);

    UserDetail user = new UserDetail();
    Model model = new ExtendedModelMap();
    BindingResult binding = new BeanPropertyBindingResult(details, "details");

    when(providerRequestService.submitProviderRequest(any(), any(), any(), any())).thenReturn("id");

    when(evidenceService.getEvidenceDocumentsForApplicationOrOutcome(any(), any()))
        .thenReturn(Mono.just(new EvidenceDocumentDetails().content(List.of())));

    when(evidenceService.uploadAndUpdateDocuments(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    providerRequestsController.postRequestDetail(
        user, providerRequestFlow, "submit", "123456789012", null, details, model, binding);

    verify(providerRequestService)
        .submitProviderRequest(
            any(ProviderRequestTypeFormData.class),
            any(ProviderRequestDetailsFormData.class),
            eq("123456789012"),
            any(UserDetail.class));
  }

  @Test
  @DisplayName("GET /types without caseReferenceNumber should pass isCaseRelated=false to lookup")
  void testInvalidCaseReference() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .param("caseReferenceNumber", "123**"))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("caseReference"));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName(
      "GET /types without a case reference resets stale flow context and preserves ACTIVE_CASE")
  void testGetRequestType_resetsStaleContextAndKeepsActiveCase() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    // Simulate switching to a general enquiry after starting a case query: the session flow still
    // holds the previous case reference and selected type.
    final ProviderRequestFlowFormData staleFlow = createProviderRequestFlow("CASE_QUERY_TYPE");
    staleFlow.setCaseReferenceNumber("300000123456");

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, staleFlow)
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("caseReference"))
        // The amendment journey relies on ACTIVE_CASE: it must not be cleared by the request flow.
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase));

    // Stale case linkage and type selection are cleared so the general enquiry is not case-linked.
    assertEquals("-1", staleFlow.getCaseReferenceNumber());
    assertNull(staleFlow.getRequestTypeFormData().getProviderRequestType());
  }

  @Test
  @DisplayName("GET /details without a case reference resets a stale case linkage to the marker")
  void testGetRequestDetail_resetsStaleCaseLinkage() throws Exception {
    final ProviderRequestFlowFormData staleFlow = createProviderRequestFlow("testType");
    staleFlow.setCaseReferenceNumber("300000123456");

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(List.of());
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    when(providerRequestDetailsValidator.getValidExtensions()).thenReturn(List.of("xml"));
    when(providerRequestDetailsValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    mockMvc
        .perform(
            get("/provider-requests/details")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, staleFlow))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("caseReference"));

    assertEquals("-1", staleFlow.getCaseReferenceNumber());
  }

  @Test
  @DisplayName("GET /details rebuilds from the URL request type even if the session flow was reset")
  void testGetRequestDetail_rebuildsFromUrlContextAfterSessionReset() throws Exception {
    // Simulate another wizard having reset the shared session flow: no type and no case reference.
    final ProviderRequestFlowFormData resetFlow = new ProviderRequestFlowFormData();

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(List.of());
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions()).thenReturn(List.of("pdf"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    when(providerRequestDetailsValidator.getValidExtensions()).thenReturn(List.of("xml"));
    when(providerRequestDetailsValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);

    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "testType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, resetFlow))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
        .andExpect(model().attribute("providerRequestType", "testType"))
        .andExpect(model().attribute("caseReference", "123456789012"));

    // The type and case reference from the URL were applied to the flow so the page (and any
    // submission from it) is correct despite the reset session flow.
    assertEquals("testType", resetFlow.getRequestTypeFormData().getProviderRequestType());
    assertEquals("123456789012", resetFlow.getCaseReferenceNumber());
  }

  @Test
  @DisplayName("GET /details rebuilds its own enquiry's form when navigating back to it")
  void testGetRequestDetail_discardsAnotherEnquirysDetails() throws Exception {
    // A case query is started from a case, then a general enquiry is started from the home page,
    // then the browser back button returns to the still-unsubmitted case query page. Both wizards
    // share one session flow, and the two request types share a field code.
    stubDynamicFormLookup("caseQueryType", "SHARED_FIELD", "CASE_QUERY_FIELD");
    stubDynamicFormLookup("generalType", "SHARED_FIELD", "GENERAL_FIELD");
    stubDetailsPageLookups();
    stubDynamicOptionPopulation();

    final ProviderRequestFlowFormData sharedFlow = new ProviderRequestFlowFormData();

    // Case query page.
    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "caseQueryType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, sharedFlow))
        .andExpect(status().isOk());

    // General enquiry page, entered from the home page: the wizard resets the shared flow.
    sharedFlow.reset();
    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("providerRequestType", "generalType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, sharedFlow))
        .andExpect(status().isOk());

    final UUID generalEnquiryDocumentSession =
        sharedFlow.getRequestDetailsFormData().getDocumentSessionId();

    // Back button to the case query page.
    final MvcResult result =
        mockMvc
            .perform(
                get("/provider-requests/details")
                    .param("caseReferenceNumber", "123456789012")
                    .param("providerRequestType", "caseQueryType")
                    .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, sharedFlow))
            .andExpect(status().isOk())
            .andExpect(model().attribute("providerRequestType", "caseQueryType"))
            .andExpect(model().attribute("caseReference", "123456789012"))
            .andReturn();

    final ProviderRequestDetailsFormData rendered =
        (ProviderRequestDetailsFormData)
            result.getModelAndView().getModel().get("providerRequestDetails");

    // The page must render the case query's own fields, not the general enquiry's leftovers, and
    // must not adopt its document session (which would attach the other enquiry's uploads).
    assertEquals(Set.of("SHARED_FIELD", "CASE_QUERY_FIELD"), rendered.getDynamicOptions().keySet());
    assertNotEquals(generalEnquiryDocumentSession, rendered.getDocumentSessionId());
  }

  @Test
  @DisplayName("GET /details keeps the answers already entered for the same enquiry")
  void testGetRequestDetail_keepsSameEnquiryAnswers() throws Exception {
    stubDynamicFormLookup("caseQueryType", "CASE_QUERY_FIELD");
    stubDetailsPageLookups();
    stubDynamicOptionPopulation();

    final ProviderRequestFlowFormData flow = new ProviderRequestFlowFormData();

    // First visit establishes the enquiry; the user then answers a question and returns to the
    // page (e.g. via the document upload journey).
    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "caseQueryType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, flow))
        .andExpect(status().isOk());

    flow.getRequestDetailsFormData().setAdditionalInformation("Case query text");
    final UUID documentSessionId = flow.getRequestDetailsFormData().getDocumentSessionId();

    final MvcResult result =
        mockMvc
            .perform(
                get("/provider-requests/details")
                    .param("caseReferenceNumber", "123456789012")
                    .param("providerRequestType", "caseQueryType")
                    .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, flow))
            .andExpect(status().isOk())
            .andReturn();

    final ProviderRequestDetailsFormData rendered =
        (ProviderRequestDetailsFormData)
            result.getModelAndView().getModel().get("providerRequestDetails");

    assertEquals("Case query text", rendered.getAdditionalInformation());
    assertEquals(documentSessionId, rendered.getDocumentSessionId());
  }

  /** Stubs the lookup for a request type whose dynamic form has the given free-text field codes. */
  private void stubDynamicFormLookup(final String requestType, final String... fieldCodes) {
    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsClaimUploadEnabled(true);
    dynamicForm.setDataItems(
        Arrays.stream(fieldCodes)
            .map(code -> new ProviderRequestDataLookupValueDetail().code(code).type("TEXT"))
            .toList());

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, requestType))
        .thenReturn(Mono.just(lookupDetail));
  }

  /** Stubs the document type dropdown and file upload constraints the details page needs. */
  private void stubDetailsPageLookups() {
    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(List.of());
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));
    when(providerRequestDocumentUploadValidator.getValidExtensions()).thenReturn(List.of("pdf"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
    when(providerRequestDetailsValidator.getValidExtensions()).thenReturn(List.of("xml"));
    when(providerRequestDetailsValidator.getMaxFileSize()).thenReturn(MAX_FILE_SIZE);
  }

  /** Makes the mapper mock populate dynamic options the way the real mapper does. */
  private void stubDynamicOptionPopulation() {
    doAnswer(
            invocation -> {
              final ProviderRequestDetailsFormData form = invocation.getArgument(0);
              final ProviderRequestTypeLookupValueDetail dynamicForm = invocation.getArgument(1);
              dynamicForm
                  .getDataItems()
                  .forEach(item -> form.getDynamicOptions().put(item.getCode(), null));
              return null;
            })
        .when(mapper)
        .populateProviderRequestDetailsForm(any(), any());
  }

  @Test
  @DisplayName("GET /details for a general enquiry hides a case banner left in the session")
  void testGetRequestDetail_hidesStaleCaseBanner() throws Exception {
    // The user visited a case (leaving ACTIVE_CASE in the session) and then jumped straight back
    // to a general enquiry page in the browser history, bypassing the home page.
    stubDynamicFormLookup("generalType", "GENERAL_FIELD");
    stubDetailsPageLookups();
    stubDynamicOptionPopulation();

    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("providerRequestType", "generalType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        // The enquiry is not case-linked, so no case context banner should be rendered...
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE))
        // ...but the shared session attribute must survive for the amendment journey.
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase));
  }

  @Test
  @DisplayName("GET /details for a case query hides a case banner for a different case")
  void testGetRequestDetail_hidesBannerForDifferentCase() throws Exception {
    stubDynamicFormLookup("caseQueryType", "CASE_QUERY_FIELD");
    stubDetailsPageLookups();
    stubDynamicOptionPopulation();

    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "caseQueryType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(
                    ACTIVE_CASE, ActiveCase.builder().caseReferenceNumber("300000123456").build()))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE));
  }

  @Test
  @DisplayName("GET /details keeps the case banner for the case the enquiry is about")
  void testGetRequestDetail_keepsMatchingCaseBanner() throws Exception {
    stubDynamicFormLookup("caseQueryType", "CASE_QUERY_FIELD");
    stubDetailsPageLookups();
    stubDynamicOptionPopulation();

    final ActiveCase ownCase = ActiveCase.builder().caseReferenceNumber("123456789012").build();

    mockMvc
        .perform(
            get("/provider-requests/details")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "caseQueryType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(ACTIVE_CASE, ownCase))
        .andExpect(status().isOk())
        .andExpect(model().attribute(ACTIVE_CASE, ownCase));
  }

  @Test
  @DisplayName("GET /documents for a general enquiry hides a case banner left in the session")
  void testAddDocuments_hidesStaleCaseBanner() throws Exception {
    stubDetailsPageLookups();

    mockMvc
        .perform(
            get("/provider-requests/documents")
                .param("providerRequestType", "generalType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase));
  }

  @Test
  @DisplayName("GET /types for a general enquiry hides a case banner left in the session")
  void testGetRequestType_hidesStaleCaseBanner() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail().content(List.of())));

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, activeCase));
  }

  @Test
  @DisplayName("GET /types for a general enquiry hides a banner rebuilt from the case in session")
  void testGetRequestType_hidesBannerRebuiltFromSessionCase() throws Exception {
    // Coming from a case leaves CASE in the session; the active case advice rebuilds ACTIVE_CASE
    // from it on every request, so clearing ACTIVE_CASE alone is not enough.
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail().content(List.of())));

    final ApplicationDetail sessionCase =
        new ApplicationDetail()
            .caseReferenceNumber("300000123456")
            .client(new ClientDetail().firstName("Test").surname("Client").reference("CLI1"));

    mockMvc
        .perform(
            get("/provider-requests/types")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(CASE, sessionCase))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist(ACTIVE_CASE));
  }

  @Test
  @DisplayName("GET /details with no session flow restarts the wizard instead of erroring")
  void testGetRequestDetail_missingSessionFlowRestartsWizard() throws Exception {
    // No PROVIDER_REQUEST_FLOW_FORM_DATA in the session (e.g. it was completed and cleared, timed
    // out, or a devtools restart wiped it) and the user navigates back into this mid-flow page.
    mockMvc
        .perform(get("/provider-requests/details").param("providerRequestType", "testType"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("GET /documents with no session flow restarts the wizard instead of erroring")
  void testAddDocuments_missingSessionFlowRestartsWizard() throws Exception {
    mockMvc
        .perform(get("/provider-requests/documents").param("providerRequestType", "testType"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("GET /details restarts the wizard when no request type is known")
  void testGetRequestDetail_blankRequestTypeRestartsWizard() throws Exception {
    // An old bookmark, or a page reached after another wizard reset the shared flow: with no type
    // in the request and none in the session, the lookup would be unfiltered and an arbitrary
    // enquiry form rendered.
    mockMvc
        .perform(
            get("/provider-requests/details")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("POST /details restarts the wizard when no request type is known")
  void testPostRequestDetail_blankRequestTypeRestartsWizard() throws Exception {
    mockMvc
        .perform(
            post("/provider-requests/details")
                .param("action", "submit")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(USER_DETAILS, userDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("GET /documents restarts the wizard when no request type is known")
  void testAddDocuments_blankRequestTypeRestartsWizard() throws Exception {
    mockMvc
        .perform(
            get("/provider-requests/documents")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("POST /documents restarts the wizard when no request type is known")
  void testAddDocumentsPost_blankRequestTypeRestartsWizard() throws Exception {
    mockMvc
        .perform(
            multipart("/provider-requests/documents")
                .file(new MockMultipartFile("file", "test.pdf", "application/pdf", "x".getBytes()))
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData())
                .sessionAttr(USER_DETAILS, userDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/types"));
  }

  @Test
  @DisplayName("GET /documents exposes a back link carrying the case reference and request type")
  void testAddDocuments_backLinkCarriesRequestContext() throws Exception {
    stubDetailsPageLookups();

    mockMvc
        .perform(
            get("/provider-requests/documents")
                .param("caseReferenceNumber", "123456789012")
                .param("providerRequestType", "caseQueryType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData()))
        .andExpect(status().isOk())
        .andExpect(
            model()
                .attribute(
                    "providerRequestDetailsUrl",
                    "/provider-requests/details?caseReferenceNumber=123456789012"
                        + "&providerRequestType=caseQueryType"));
  }

  @Test
  @DisplayName("GET /documents back link omits the case reference for a general enquiry")
  void testAddDocuments_backLinkOmitsCaseReferenceForGeneralEnquiry() throws Exception {
    stubDetailsPageLookups();

    mockMvc
        .perform(
            get("/provider-requests/documents")
                .param("providerRequestType", "generalType")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, new ProviderRequestFlowFormData()))
        .andExpect(status().isOk())
        .andExpect(
            model()
                .attribute(
                    "providerRequestDetailsUrl",
                    "/provider-requests/details?providerRequestType=generalType"));
  }

  @Test
  @DisplayName("File-too-large keeps the hidden fields and back link on the upload page")
  void testHandleUploadFileTooLarge_keepsRequestContext() throws Exception {
    stubDetailsPageLookups();

    final ProviderRequestFlowFormData providerRequestFlow =
        createProviderRequestFlow("caseQueryType");
    providerRequestFlow.setCaseReferenceNumber("123456789012");
    final Model uploadModel = new ExtendedModelMap();

    final String viewName =
        providerRequestsController.handleUploadFileTooLarge(
            new EvidenceUploadFormData(), providerRequestFlow, uploadModel);

    assertEquals("requests/provider-request-doc-upload", viewName);
    assertEquals("caseQueryType", uploadModel.getAttribute("providerRequestType"));
    assertEquals(
        "/provider-requests/details?caseReferenceNumber=123456789012"
            + "&providerRequestType=caseQueryType",
        uploadModel.getAttribute("providerRequestDetailsUrl"));
  }
}
