package uk.gov.laa.ccms.caab.controller.requests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypeDetailsValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
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
  private ProviderRequestTypeDetailsValidator providerRequestTypeValidator;

  @InjectMocks
  private ProviderRequestsController providerRequestsController;

  @Autowired
  private WebApplicationContext webApplicationContext;


  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerRequestsController).build();
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
}
