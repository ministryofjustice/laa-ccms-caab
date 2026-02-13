package uk.gov.laa.ccms.caab.controller.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
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
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
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

  @Mock private ProviderRequestTypesValidator providerRequestTypeValidator;

  @Mock private ProviderRequestDetailsValidator providerRequestDetailsValidator;

  @Mock private ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

  @Mock private AvScanService avScanService;

  @Mock private ProviderRequestsMapper mapper;

  @Mock private Model model;

  @InjectMocks private ProviderRequestsController providerRequestsController;

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

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));

    mockMvc
        .perform(
            get("/provider-requests/details")
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
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

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        new ProviderRequestTypeLookupValueDetail()
            .isClaimUploadEnabled(false)
            .additionalInformationPrompt("Additional Info");

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
                .param("action", "document_upload")
                .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/provider-requests/documents"));

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

    when(lookupService.getProviderRequestTypes(null, "testType"))
        .thenReturn(Mono.just(lookupDetail));
    when(evidenceService.getEvidenceDocumentsForApplicationOrOutcome(any(), eq(CcmsModule.REQUEST)))
        .thenReturn(Mono.just(new EvidenceDocumentDetails()));

    mockMvc
        .perform(
            post("/provider-requests/details")
                .sessionAttr(USER_DETAILS, userDetails)
                .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
                .flashAttr("providerRequestDetails", providerRequestDetailsForm)
                .param("action", "submit"))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
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
    commonLookupDetail.setContent(
        List.of(new CommonLookupValueDetail().code("DOC1").description("Document Type 1")));
    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(commonLookupDetail));

    when(providerRequestDocumentUploadValidator.getValidExtensions())
        .thenReturn(List.of("pdf", "docx"));
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(maxFileSize); // 5 MB

    mockMvc
        .perform(get("/provider-requests/documents"))
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
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
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
        .andExpect(redirectedUrl("/provider-requests/details"));

    // Verify interactions
    verify(mapper).toProviderRequestDocumentDetail(evidenceUploadFormData);
    verify(evidenceService).addDocument(eq(evidenceDocumentDetail), eq(userDetails.getLoginId()));
  }

  @Test
  @DisplayName("Should handle validation errors during document upload")
  void testPostDocuments_validationErrors() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    final String maxFileSize = String.valueOf(5L * 1024 * 1024);

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
    when(providerRequestDocumentUploadValidator.getMaxFileSize()).thenReturn(maxFileSize);

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
        .andExpect(model().attribute("maxFileSize", maxFileSize));

    // Verify interactions with mocked dependencies
    verify(providerRequestDocumentUploadValidator).validate(eq(evidenceUploadFormData), any());
    verify(lookupService).getCommonValues(COMMON_VALUE_DOCUMENT_TYPES);
    verify(providerRequestDocumentUploadValidator).getValidExtensions();
    verify(providerRequestDocumentUploadValidator).getMaxFileSize();
  }
}
