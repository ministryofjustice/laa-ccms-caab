package uk.gov.laa.ccms.caab.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.opa.context.ContextToken;
import uk.gov.laa.ccms.caab.opa.util.SecurityUtils;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryAttributeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class AssessmentControllerTest {

  @Mock
  private AssessmentService assessmentService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientService clientService;

  @Mock
  private SecurityUtils contextSecurityUtil;

  @InjectMocks
  private AssessmentController assessmentController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    assessmentController.owdUrl = "http://example.com";
    assessmentController.interviewStyling = "interview-styling.css";
    assessmentController.fontStyling = "font-styling.css";
    assessmentController.interviewJavascript = "interview-javascript.js";

    this.mockMvc = standaloneSetup(assessmentController).build();
  }

  private static final UserDetail userDetails = buildUserDetail();

  private static final ActiveCase activeCase = ActiveCase.builder()
      .caseReferenceNumber("testCaseReferenceNumber")
      .build();

  @Test
  public void assessmentRemoveDisplaysCorrectView() throws Exception {
    this.mockMvc.perform(get("/assessments/testCategory/remove"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/assessments/assessment-remove"))
        .andExpect(model().attribute("assessment", "testCategory"));
  }

  @ParameterizedTest
  @CsvSource({
      "merits",
      "means"
  })
  public void assessmentRemovePostRedirectsToSummaryWhenAssessmentExists(
      final String category) throws Exception {
    when(assessmentService.deleteAssessments(any(), any(), any(), any())).thenReturn(Mono.empty());

    this.mockMvc.perform(post("/assessments/%s/remove".formatted(category))
            .sessionAttr(USER_DETAILS, userDetails)
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections"));
  }

  @Test
  public void assessmentGet_validAssessment() throws Exception {
    when(applicationService.getApplication(anyString())).thenReturn(
        Mono.just(buildApplicationDetail(1, true, new Date())));
    when(contextSecurityUtil.createHubContext(anyString(), anyLong(), anyString(), anyLong(), anyString(), anyString(), anyString())).thenReturn(
        "contextToken");
    when(clientService.getClient(anyString(), anyString(), anyString())).thenReturn(
        Mono.just(new ClientDetail()));

    final AssessmentDetail assessmentDetail = buildAssessmentDetail(new Date());
    assessmentDetail.setId(1L);

    when(assessmentService.getAssessments(any(), anyString(), anyString())).thenReturn(
        Mono.just(new AssessmentDetails().addContentItem(assessmentDetail)));

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "means")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    mockMvc.perform(request)
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/assessments/assessment-get"))
        .andExpect(model().attributeExists("checkpoint"))
        .andExpect(model().attributeExists("cancelUrl"))
        .andExpect(model().attributeExists("owdUrl"))
        .andExpect(model().attributeExists("frameTitle"))
        .andExpect(model().attributeExists("returnLinkText"))
        .andExpect(model().attributeExists("deploymentName"))
        .andExpect(model().attributeExists("interviewsCSS"))
        .andExpect(model().attributeExists("fontsCSS"))
        .andExpect(model().attributeExists("interviewsJS"))
        .andExpect(model().attributeExists("params"))
        .andExpect(model().attributeExists("submitReturnUrl"))
        .andExpect(model().attributeExists("username"))
        .andExpect(model().attributeExists("resumeId"))
        .andExpect(model().attributeExists("assessmentType"));
  }

  @Test
  public void assessmentGet_applicationNotFound() {
    when(applicationService.getApplication(anyString())).thenReturn(Mono.empty());

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "means")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve application", exception.getCause().getMessage());
  }

  @Test
  public void assessmentGet_unknownAssessment() {
    when(applicationService.getApplication(anyString()))
        .thenReturn(Mono.just(buildApplicationDetail(1, true, new Date())));

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "unknown")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Invalid assessment type", exception.getCause().getMessage());
  }

  @Test
  public void assessmentGet_clientNotFound() {
    when(applicationService.getApplication(anyString())).thenReturn(Mono.just(buildApplicationDetail(1, true, new Date())));
    when(assessmentService.getAssessments(any(), anyString(), anyString())).thenReturn(Mono.empty());
    when(clientService.getClient(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "means")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve client details", exception.getCause().getMessage());
  }

  @Test
  public void assessmentGet_assessmentDetailsNotFound() {
    when(applicationService.getApplication(anyString())).thenReturn(Mono.just(buildApplicationDetail(1, true, new Date())));
    when(contextSecurityUtil.createHubContext(anyString(), anyLong(), anyString(), anyLong(), anyString(), anyString(), anyString())).thenReturn("contextToken");
    when(clientService.getClient(anyString(), anyString(), anyString())).thenReturn(Mono.just(new ClientDetail()));
    when(assessmentService.getAssessments(any(), anyString(), anyString())).thenReturn(Mono.empty());

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "means")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve assessment details", exception.getCause().getMessage());
  }

  @Test
  public void assessmentGet_prepopAssessmentNotFound() {
    when(applicationService.getApplication(anyString())).thenReturn(Mono.just(buildApplicationDetail(1, true, new Date())));
    when(contextSecurityUtil.createHubContext(anyString(), anyLong(), anyString(), anyLong(), anyString(), anyString(), anyString())).thenReturn("contextToken");
    when(clientService.getClient(anyString(), anyString(), anyString())).thenReturn(Mono.just(new ClientDetail()));
    when(assessmentService.getAssessments(any(), anyString(), anyString())).thenReturn(Mono.just(new AssessmentDetails()));

    final MockHttpServletRequestBuilder request = get("/assessments")
        .param("assessment", "means")
        .param("invoked-from", "summary")
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve assessment details", exception.getCause().getMessage());
  }

  @Test
  public void assessmentConfirmDisplaysCorrectView() throws Exception {
    final String token = "someToken";
    final ContextToken contextToken = new ContextToken();
    contextToken.setRulebaseId(1L);
    contextToken.setProviderId("providerId");
    contextToken.setCaseId("caseReferenceNumber");

    // Setup mock data for parent summary lookups
    final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    parentSummaryLookup.setName("PROCEEDING");
    parentSummaryLookup.setDisplayName("Proceeding");
    parentSummaryLookup.setEntityLevel(1);
    parentSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("PROCEEDING_NAME")
        .displayName("Proceeding Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups = List.of(parentSummaryLookup);

    // Setup mock data for child summary lookups
    final AssessmentSummaryEntityLookupValueDetail childSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    childSummaryLookup.setName("CHILD_ENTITY");
    childSummaryLookup.setDisplayName("Child Entity");
    childSummaryLookup.setEntityLevel(2);
    childSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("CHILD_NAME")
        .displayName("Child Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups = List.of(childSummaryLookup);

    final AssessmentSummaryEntityLookupDetail parentSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    parentSummaryLookupDetail.setContent(parentSummaryLookups);

    final AssessmentSummaryEntityLookupDetail childSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    childSummaryLookupDetail.setContent(childSummaryLookups);

    // Mock the necessary methods to return expected values
    when(lookupService.getAssessmentSummaryAttributes("PARENT"))
        .thenReturn(Mono.just(parentSummaryLookupDetail));
    when(lookupService.getAssessmentSummaryAttributes("CHILD"))
        .thenReturn(Mono.just(childSummaryLookupDetail));

    when(contextSecurityUtil.createContextToken(anyString())).thenReturn(contextToken);
    when(assessmentService.getAssessments(anyList(), anyString(), anyString()))
        .thenReturn(Mono.just(new AssessmentDetails().addContentItem(buildAssessmentDetail(new Date()))));
    when(assessmentService.getAssessmentSummaryToDisplay(any(), any(), any())).thenReturn(new ArrayList<>());

    final MockHttpServletRequestBuilder request = get("/assessments/confirm")
        .param("val", token)
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    this.mockMvc.perform(request)
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/assessments/assessment-confirm"))
        .andExpect(model().attributeExists("summary"));
  }

  @Test
  public void assessmentConfirm_throwsExceptionWhenAssessmentDetailsNotFound() {
    final String token = "someToken";
    final ContextToken contextToken = new ContextToken();
    contextToken.setRulebaseId(1L);
    contextToken.setProviderId("providerId");
    contextToken.setCaseId("caseReferenceNumber");

    // Setup mock data for parent summary lookups
    final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    parentSummaryLookup.setName("PROCEEDING");
    parentSummaryLookup.setDisplayName("Proceeding");
    parentSummaryLookup.setEntityLevel(1);
    parentSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("PROCEEDING_NAME")
        .displayName("Proceeding Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups = List.of(parentSummaryLookup);

    // Setup mock data for child summary lookups
    final AssessmentSummaryEntityLookupValueDetail childSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    childSummaryLookup.setName("CHILD_ENTITY");
    childSummaryLookup.setDisplayName("Child Entity");
    childSummaryLookup.setEntityLevel(2);
    childSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("CHILD_NAME")
        .displayName("Child Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups = List.of(childSummaryLookup);

    final AssessmentSummaryEntityLookupDetail parentSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    parentSummaryLookupDetail.setContent(parentSummaryLookups);

    final AssessmentSummaryEntityLookupDetail childSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    childSummaryLookupDetail.setContent(childSummaryLookups);

    // Mock the necessary methods to return expected values
    when(lookupService.getAssessmentSummaryAttributes("PARENT"))
        .thenReturn(Mono.just(parentSummaryLookupDetail));
    when(lookupService.getAssessmentSummaryAttributes("CHILD"))
        .thenReturn(Mono.just(childSummaryLookupDetail));

    when(contextSecurityUtil.createContextToken(anyString())).thenReturn(contextToken);
    when(assessmentService.getAssessments(anyList(), anyString(), anyString())).thenReturn(Mono.empty());

    final MockHttpServletRequestBuilder request = get("/assessments/confirm")
        .param("val", token)
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve assessment data", exception.getCause().getMessage());
  }

  @Test
  public void assessmentConfirm_throwsExceptionWhenAssessmentDetailNotFound() {
    final String token = "someToken";
    final ContextToken contextToken = new ContextToken();
    contextToken.setRulebaseId(1L);
    contextToken.setProviderId("providerId");
    contextToken.setCaseId("caseReferenceNumber");

    // Setup mock data for parent summary lookups
    final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    parentSummaryLookup.setName("PROCEEDING");
    parentSummaryLookup.setDisplayName("Proceeding");
    parentSummaryLookup.setEntityLevel(1);
    parentSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("PROCEEDING_NAME")
        .displayName("Proceeding Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups = List.of(parentSummaryLookup);

    // Setup mock data for child summary lookups
    final AssessmentSummaryEntityLookupValueDetail childSummaryLookup = new AssessmentSummaryEntityLookupValueDetail();
    childSummaryLookup.setName("CHILD_ENTITY");
    childSummaryLookup.setDisplayName("Child Entity");
    childSummaryLookup.setEntityLevel(2);
    childSummaryLookup.addAttributesItem(new AssessmentSummaryAttributeLookupValueDetail()
        .name("CHILD_NAME")
        .displayName("Child Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups = List.of(childSummaryLookup);

    final AssessmentSummaryEntityLookupDetail parentSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    parentSummaryLookupDetail.setContent(parentSummaryLookups);

    final AssessmentSummaryEntityLookupDetail childSummaryLookupDetail = new AssessmentSummaryEntityLookupDetail();
    childSummaryLookupDetail.setContent(childSummaryLookups);

    // Mock the necessary methods to return expected values
    when(lookupService.getAssessmentSummaryAttributes("PARENT"))
        .thenReturn(Mono.just(parentSummaryLookupDetail));
    when(lookupService.getAssessmentSummaryAttributes("CHILD"))
        .thenReturn(Mono.just(childSummaryLookupDetail));

    when(contextSecurityUtil.createContextToken(anyString())).thenReturn(contextToken);
    when(assessmentService.getAssessments(anyList(), anyString(), anyString()))
        .thenReturn(Mono.just(new AssessmentDetails())); // Empty AssessmentDetails

    final MockHttpServletRequestBuilder request = get("/assessments/confirm")
        .param("val", token)
        .sessionAttr(USER_DETAILS, userDetails)
        .sessionAttr(APPLICATION_ID, "applicationId")
        .sessionAttr(ACTIVE_CASE, activeCase);

    final Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(request));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to retrieve assessment details", exception.getCause().getMessage());
  }

}
