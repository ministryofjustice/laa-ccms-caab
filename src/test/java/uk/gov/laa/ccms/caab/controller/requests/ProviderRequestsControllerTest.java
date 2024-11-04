package uk.gov.laa.ccms.caab.controller.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypesValidator;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ProviderRequestsControllerTest {

  private MockMvc mockMvc;

  @Mock
  private LookupService lookupService;

  @Mock
  private ProviderRequestTypesValidator providerRequestTypeValidator;

  @Mock
  private ProviderRequestDetailsValidator providerRequestDetailsValidator;

  @Mock
  private AvScanService avScanService;

  @Mock
  private ProviderRequestsMapper mapper;

  @Mock
  private Model model;

  @InjectMocks
  private ProviderRequestsController providerRequestsController;

  @Autowired
  private WebApplicationContext webApplicationContext;


  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerRequestsController).build();
  }

  /**
   * Creates a populated ProviderRequestFlowFormData with a specific request type.
   */
  private ProviderRequestFlowFormData createProviderRequestFlow(final String requestType) {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType(requestType);
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    return providerRequestFlow;
  }

  /**
   * Mocks the lookupService to return a ProviderRequestTypeLookupDetail containing a single
   * dynamic form with lookup items.
   */
  private void mockLookupServiceWithDynamicForm(final ProviderRequestTypeLookupValueDetail dynamicForm,
                                                final List<ProviderRequestDataLookupValueDetail> lookupItems) {
   final  ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    dynamicForm.setDataItems(lookupItems);
    lookupDetail.setContent(List.of(dynamicForm));
    when(lookupService.getProviderRequestTypes(null, "testType")).thenReturn(Mono.just(lookupDetail));
  }

  /**
   * Creates a ProviderRequestTypeLookupValueDetail with default configuration.
   */
  private ProviderRequestTypeLookupValueDetail createDefaultDynamicForm() {
    final ProviderRequestTypeLookupValueDetail dynamicForm = new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsFileUploadEnabled(true);
    dynamicForm.setFileUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");
    return dynamicForm;
  }

  /**
   * Creates a mock lookup item with specified code and lookup type.
   */
  private ProviderRequestDataLookupValueDetail createLookupItem(final String code, final String lovLookupType) {
    final ProviderRequestDataLookupValueDetail lookupItem = new ProviderRequestDataLookupValueDetail();
    lookupItem.setCode(code);
    lookupItem.setLovLookupType(lovLookupType);
    lookupItem.setType("LOV");
    return lookupItem;
  }

  /**
   * Mocks AV scan service to throw an exception.
   */
  private void mockAvScanServiceToThrow() throws AvScanException {
    doThrow(new AvScanException("Virus alert")).when(avScanService).performAvScan(
        any(), any(), any(), any(), any(), any(InputStream.class));
  }

  @Test
  @DisplayName("GET /provider-requests/types should return provider request type view")
  void testGetRequestType() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc.perform(get("/provider-requests/types"))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-type"))
        .andExpect(model().attributeExists("providerRequestTypeDetails"))
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("POST /provider-requests/types should redirect to provider request details on success")
  void testRequestTypePost_Success() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestTypeFormData providerRequestTypeDetails = new ProviderRequestTypeFormData();

    doAnswer(invocation -> null).when(providerRequestTypeValidator).validate(any(), any());
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc.perform(post("/provider-requests/types")
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
    final ProviderRequestTypeFormData providerRequestTypeDetails = new ProviderRequestTypeFormData();

    doAnswer(invocation -> {
      final Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("providerRequestType", "required.providerRequestType", "Please select a request type.");
      return null;
    }).when(providerRequestTypeValidator).validate(any(), any());

    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc.perform(post("/provider-requests/types")
            .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
        .flashAttr("providerRequestTypeDetails", providerRequestTypeDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-type"))
        .andExpect(model().attributeHasFieldErrors("providerRequestTypeDetails", "providerRequestType"))
        .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA))
        .andExpect(model().attributeExists("providerRequestTypeDetails"))
        .andExpect(model().attributeExists("providerRequestTypes"));

    verify(providerRequestTypeValidator).validate(any(), any());
    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }


  @Test
  @DisplayName("Should populate provider request types in the model")
  void testPopulateProviderRequestTypes() throws Exception {
    final ProviderRequestTypeLookupValueDetail mockRequestType = new ProviderRequestTypeLookupValueDetail();
    final ProviderRequestTypeLookupDetail mockDetail = new ProviderRequestTypeLookupDetail();
    mockDetail.setContent(List.of(mockRequestType));

    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(mockDetail));

    mockMvc.perform(get("/provider-requests/types"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", List.of(mockRequestType)));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should handle empty provider request types in the model")
  void testPopulateProviderRequestTypes_Empty() throws Exception {
    when(lookupService.getProviderRequestTypes(eq(false), isNull()))
        .thenReturn(Mono.just(new ProviderRequestTypeLookupDetail()));

    mockMvc.perform(get("/provider-requests/types"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("providerRequestTypes"))
        .andExpect(model().attribute("providerRequestTypes", Collections.emptyList()));

    verify(lookupService).getProviderRequestTypes(eq(false), isNull());
  }

  @Test
  @DisplayName("Should return provider request detail view with populated model attributes")
  void testProviderRequestsDetails_PopulatesModel() {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData providerRequestDetailsForm = new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);

    final ProviderRequestTypeLookupValueDetail dynamicForm = new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsFileUploadEnabled(true);
    dynamicForm.setFileUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, "testType")).thenReturn(Mono.just(lookupDetail));

    final String viewName = providerRequestsController.providerRequestsDetails(
        providerRequestFlow, providerRequestDetailsForm, model);

    verify(lookupService).getProviderRequestTypes(null, "testType");
    verify(mapper).populateProviderRequestDetailsForm(
        providerRequestDetailsForm, dynamicForm);
    verify(model).addAttribute("providerRequestDynamicForm", dynamicForm);
    verify(model).addAttribute("providerRequestDetails", providerRequestDetailsForm);
    verify(model).addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);

    assertEquals("requests/provider-request-detail", viewName);
    assertTrue(providerRequestDetailsForm.isFileUploadEnabled());
    assertEquals("Upload Files", providerRequestDetailsForm.getFileUploadLabel());
    assertEquals("Additional Info", providerRequestDetailsForm.getAdditionalInformationLabel());
  }

  @Test
  @DisplayName("GET /provider-requests/details should return provider request detail view with populated model")
  void testGetRequestDetail_PopulatesModel() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData providerRequestDetailsForm = new ProviderRequestDetailsFormData();
    final ProviderRequestTypeFormData providerRequestType = new ProviderRequestTypeFormData();
    providerRequestType.setProviderRequestType("testType");
    providerRequestFlow.setRequestTypeFormData(providerRequestType);
    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    final ProviderRequestTypeLookupValueDetail dynamicForm = new ProviderRequestTypeLookupValueDetail();
    dynamicForm.setIsFileUploadEnabled(true);
    dynamicForm.setFileUploadPrompt("Upload Files");
    dynamicForm.setAdditionalInformationPrompt("Additional Info");

    final ProviderRequestTypeLookupDetail lookupDetail = new ProviderRequestTypeLookupDetail();
    lookupDetail.setContent(List.of(dynamicForm));

    when(lookupService.getProviderRequestTypes(null, "testType")).thenReturn(Mono.just(lookupDetail));

    mockMvc.perform(get("/provider-requests/details")
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
    final ProviderRequestTypeLookupValueDetail providerRequestType = new ProviderRequestTypeLookupValueDetail();

    final ProviderRequestDataLookupValueDetail lookupItem = new ProviderRequestDataLookupValueDetail();
    lookupItem.setCode("testCode");
    lookupItem.setLovLookupType("testLookupType");
    lookupItem.setType("LOV");

    providerRequestType.setDataItems(List.of(lookupItem));

    final List<CommonLookupValueDetail> commonValues = List.of(new CommonLookupValueDetail()
        .code("value1")
        .description("Label 1"));

    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(commonValues);

    when(lookupService.getCommonValues("testLookupType")).thenReturn(Mono.just(commonLookupDetail));

    providerRequestsController.populateProviderRequestDetailsLookupDropdowns(model, providerRequestType);

    verify(lookupService).getCommonValues("testLookupType");
    verify(model).addAttribute("testCode", commonValues);
  }

  @Test
  @DisplayName("POST /provider-requests/details should redirect to home on successful submission with populated lookups")
  void testPostRequestDetail_SuccessWithLookups() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = new ProviderRequestFlowFormData();
    final ProviderRequestDetailsFormData providerRequestDetailsForm = new ProviderRequestDetailsFormData();
    providerRequestDetailsForm.setFileUploadEnabled(false);

    mockMvc.perform(post("/provider-requests/details")
            .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
            .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"));

    verify(providerRequestDetailsValidator).validate(eq(providerRequestDetailsForm), any());
    verify(mapper).toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
  }

  @Test
  @DisplayName("POST /provider-requests/details should return to detail view with validation errors and populate lookups")
  void testPostRequestDetail_HasValidationErrorsWithLookups() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createProviderRequestFlow("testType");
    final ProviderRequestDetailsFormData providerRequestDetailsForm = new ProviderRequestDetailsFormData();

    final ProviderRequestTypeLookupValueDetail dynamicForm = createDefaultDynamicForm();
    final ProviderRequestDataLookupValueDetail lookupItem = createLookupItem("testCode", "testLookupType");

    mockLookupServiceWithDynamicForm(dynamicForm, List.of(lookupItem));

    List<CommonLookupValueDetail> commonValues = List.of(new CommonLookupValueDetail()
        .code("value1")
        .description("Label 1"));
    CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(commonValues);
    when(lookupService.getCommonValues("testLookupType")).thenReturn(Mono.just(commonLookupDetail));

    doAnswer(invocation -> {
      final BindingResult bindingResult = invocation.getArgument(1);
      bindingResult.rejectValue("additionalInformation", "error.code", "Validation error");
      return null;
    }).when(providerRequestDetailsValidator).validate(any(), any());

    mockMvc.perform(post("/provider-requests/details")
            .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
            .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
        .andExpect(model().attributeExists("providerRequestDetails"))
        .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA));

    verify(providerRequestDetailsValidator).validate(eq(providerRequestDetailsForm), any());
    verify(mapper).toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
    verify(lookupService).getCommonValues(any());
  }

  @Test
  @DisplayName("POST /provider-requests/details should return to detail view on AV scan failure with populated lookups")
  void testPostRequestDetail_AVScanFailureWithLookups() throws Exception {
    final ProviderRequestFlowFormData providerRequestFlow = createProviderRequestFlow("testType");
    final ProviderRequestDetailsFormData providerRequestDetailsForm = new ProviderRequestDetailsFormData();
    providerRequestDetailsForm.setFileUploadEnabled(true);

    final MockMultipartFile mockFile =
        new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
    providerRequestDetailsForm.setFile(mockFile);

    final ProviderRequestTypeLookupValueDetail dynamicForm = createDefaultDynamicForm();
    final ProviderRequestDataLookupValueDetail lookupItem = createLookupItem("testCode", "testLookupType");

    mockLookupServiceWithDynamicForm(dynamicForm, List.of(lookupItem));

    final List<CommonLookupValueDetail> commonValues = List.of(new CommonLookupValueDetail().code("value1").description("Label 1"));
    final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
    commonLookupDetail.setContent(commonValues);
    when(lookupService.getCommonValues("testLookupType")).thenReturn(Mono.just(commonLookupDetail));

    mockAvScanServiceToThrow();

    mockMvc.perform(multipart("/provider-requests/details")
            .file(mockFile)
            .sessionAttr(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow)
            .flashAttr("providerRequestDetails", providerRequestDetailsForm))
        .andExpect(status().isOk())
        .andExpect(view().name("requests/provider-request-detail"))
        .andExpect(model().attributeHasFieldErrors("providerRequestDetails", "file"))
        .andExpect(model().attributeExists(PROVIDER_REQUEST_FLOW_FORM_DATA))
        .andExpect(model().attribute("providerRequestDynamicForm", dynamicForm));

    verify(avScanService).performAvScan(any(), any(), any(), any(), eq("test.txt"), any());
    verify(providerRequestDetailsValidator).validate(eq(providerRequestDetailsForm), any());
    verify(lookupService).getCommonValues("testLookupType");
    verify(mapper).toProviderRequestDetailsFormData(providerRequestDetailsForm, providerRequestFlow);
  }

}
