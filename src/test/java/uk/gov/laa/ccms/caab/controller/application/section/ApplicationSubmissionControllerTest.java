package uk.gov.laa.ccms.caab.controller.application.section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_VIEW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CASE;
import static uk.gov.laa.ccms.caab.controller.application.section.ApplicationSubmissionController.CHILD_LOOKUP;
import static uk.gov.laa.ccms.caab.controller.application.section.ApplicationSubmissionController.PARENT_LOOKUP;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.validators.application.ProviderDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.declaration.DeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.SubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseTransactionResponse;

@ExtendWith(SpringExtension.class)
class ApplicationSubmissionControllerTest {

  private MockMvc mockMvc;

  @Mock
  private Model model;

  private static final String ERROR_ATTRIBUTE = "errorMessages";

  @Mock
  private ApplicationService applicationService;

  @Mock
  private AssessmentService assessmentService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientService clientService;

  @Mock
  private EvidenceService evidenceService;

  @Mock
  private ClientDetailMapper clientDetailsMapper;

  @Mock
  private SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;

  @Mock
  private DeclarationSubmissionValidator declarationSubmissionValidator;

  @Mock
  private ProceedingAndCostsMapper proceedingAndCostsMapper;

  @Mock
  private ProviderDetailsValidator providerDetailsValidator;
  @Mock
  private CorrespondenceAddressValidator correspondenceAddressValidator;
  @Mock
  private ProceedingMatterTypeDetailsValidator matterTypeValidator;
  @Mock
  private ProceedingDetailsValidator proceedingTypeValidator;
  @Mock
  private ProceedingFurtherDetailsValidator furtherDetailsValidator;
  @Mock
  private PriorAuthorityTypeDetailsValidator priorAuthorityTypeValidator;
  @Mock
  private PriorAuthorityDetailsValidator priorAuthorityDetailsValidator;
  @Mock
  private OrganisationOpponentValidator organisationOpponentValidator;
  @Mock
  private IndividualOpponentValidator individualOpponentValidator;

  @InjectMocks
  private ApplicationSubmissionController applicationSubmissionController;

  private ActiveCase activeCase;

  private static final UserDetail userDetail = buildUserDetail();

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(applicationSubmissionController).build();

    activeCase = ActiveCase.builder()
        .providerId(1)
        .applicationId(1)
        .caseReferenceNumber("abc123")
        .clientReferenceNumber("xyz123").build();
  }

  @Test
  @DisplayName("Test /application/abandon")
  void testViewAbandonApplicationConfirmation() throws Exception {

    mockMvc.perform(get("/application/abandon")
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-abandon-confirmation"));
  }

  @Test
  @DisplayName("Test abandonApplication - success")
  void testAbandonApplication() throws Exception {
    final ApplicationDetail applicationDetail = buildApplicationDetail(1, true, new Date());

    when(applicationService.getApplication("1")).thenReturn(Mono.just(applicationDetail));

    mockMvc.perform(post("/application/abandon/confirmed")
            .sessionAttr(ACTIVE_CASE, activeCase)
            .sessionAttr(USER_DETAILS, userDetail))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"));

    verify(applicationService, times(1))
        .abandonApplication(applicationDetail, userDetail);
  }

  @Test
  @DisplayName("Test applicationSummary - success")
  void testApplicationSummary() throws Exception {
    final ApplicationDetail applicationDetail = buildApplicationDetail(1, true, new Date());

    final AssessmentDetail meansTestAssessment = new AssessmentDetail();
    meansTestAssessment.setName(AssessmentRulebase.MEANS.getName());
    final AssessmentDetail meritsTestAssessment = new AssessmentDetail();
    meritsTestAssessment.setName(AssessmentRulebase.MERITS.getName());
    final AssessmentDetails assessmentDetails = new AssessmentDetails()
        .content(List.of(meansTestAssessment, meritsTestAssessment));

    final ClientFlowFormData clientFlowFormData = new ClientFlowFormData(ACTION_VIEW);
    final List<AssessmentSummaryEntityLookupValueDetail> assessmentSummaryLookups = List.of();
    final AssessmentSummaryEntityLookupDetail assessmentSummaryEntityLookupDetail = new AssessmentSummaryEntityLookupDetail();
    assessmentSummaryEntityLookupDetail.setContent(assessmentSummaryLookups);
    final ProceedingSubmissionSummaryMappingContext proceedingContext = ProceedingSubmissionSummaryMappingContext.builder().build();
    final OpponentSubmissionSummaryMappingContext opponentContext = OpponentSubmissionSummaryMappingContext.builder().build();
    final GeneralDetailsSubmissionSummaryMappingContext generalDetailsContext = GeneralDetailsSubmissionSummaryMappingContext.builder().build();

    when(applicationService.getApplication(any())).thenReturn(Mono.just(applicationDetail));
    when(assessmentService.getAssessments(any(), any(), any())).thenReturn(Mono.just(assessmentDetails));
    when(clientService.getClient(any(), any(), any())).thenReturn(Mono.just(buildClientDetail()));
    when(lookupService.getAssessmentSummaryAttributes(any())).thenReturn(Mono.just(assessmentSummaryEntityLookupDetail));
    when(lookupService.getProceedingSubmissionMappingContext()).thenReturn(Mono.just(proceedingContext));
    when(lookupService.getOpponentSubmissionMappingContext()).thenReturn(Mono.just(opponentContext));
    when(lookupService.getGeneralDetailsSubmissionMappingContext()).thenReturn(Mono.just(generalDetailsContext));
    when(clientDetailsMapper.toClientFlowFormData(any())).thenReturn(clientFlowFormData);

    when(assessmentService.getAssessmentSummaryToDisplay(any(), any(), any())).thenReturn(List.of());
    when(submissionSummaryDisplayMapper.toProviderSummaryDisplay(any())).thenReturn(new ProviderSubmissionSummaryDisplay());
    when(lookupService.getClientSummaryListLookups(any())).thenReturn(Mono.just(new HashMap<>()));
    when(submissionSummaryDisplayMapper.toGeneralDetailsSummaryDisplay(any(), any())).thenReturn(new GeneralDetailsSubmissionSummaryDisplay());
    when(submissionSummaryDisplayMapper.toProceedingAndCostSummaryDisplay(any(), any())).thenReturn(new ProceedingAndCostSubmissionSummaryDisplay());
    when(submissionSummaryDisplayMapper.toOpponentsAndOtherPartiesSummaryDisplay(any(), any())).thenReturn(new OpponentsAndOtherPartiesSubmissionSummaryDisplay());

    mockMvc.perform(get("/application/summary")
            .sessionAttr(USER_DETAILS, userDetail)
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-summary-complete"))
        .andExpect(model().attributeExists("submissionSummary"));

    verify(applicationService, times(1)).getApplication(any());
    verify(assessmentService, times(1)).getAssessments(any(), any(), any());
    verify(clientService, times(1)).getClient(any(), any(), any());
    verify(lookupService, times(1)).getAssessmentSummaryAttributes(PARENT_LOOKUP);
    verify(lookupService, times(1)).getAssessmentSummaryAttributes(CHILD_LOOKUP);
    verify(lookupService, times(1)).getProceedingSubmissionMappingContext();
    verify(lookupService, times(1)).getOpponentSubmissionMappingContext();
    verify(lookupService, times(1)).getGeneralDetailsSubmissionMappingContext();
  }

  @Test
  @DisplayName("Test applicationSummary print - success")
  void testApplicationSummaryPrint() throws Exception {
    final SubmissionSummaryDisplay submissionSummary = SubmissionSummaryDisplay.builder().build();

    mockMvc.perform(get("/application/summary/print")
            .sessionAttr(SUBMISSION_SUMMARY, submissionSummary))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-summary-complete-printable"))
        .andExpect(model().attributeExists("submissionSummary"));

    Mockito.verifyNoInteractions(applicationService, assessmentService, clientService, lookupService, submissionSummaryDisplayMapper);
  }

  @Test
  @DisplayName("Test applicationSummaryPost - success")
  void testApplicationSummaryPost_success() throws Exception {
    final SummarySubmissionFormData formData = new SummarySubmissionFormData();
    final SubmissionSummaryDisplay submissionSummary = SubmissionSummaryDisplay.builder().build();

    mockMvc.perform(post("/application/summary")
            .flashAttr("summarySubmissionFormData", formData)
            .sessionAttr(SUBMISSION_SUMMARY, submissionSummary))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/declaration"));

    Mockito.verifyNoInteractions(applicationService, assessmentService, clientService, lookupService, submissionSummaryDisplayMapper);
  }

  @Test
  @DisplayName("Test applicationDeclaration GET - success")
  void testApplicationDeclarationGet_success() throws Exception {
    final DeclarationLookupDetail declarationLookupDetail = new DeclarationLookupDetail();
    final List<DynamicCheckbox> checkboxes = List.of(new DynamicCheckbox());

    when(lookupService.getDeclarations(any())).thenReturn(Mono.just(declarationLookupDetail));
    when(submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(any())).thenReturn(checkboxes);

    mockMvc.perform(get("/application/declaration"))
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-submit-declaration"))
        .andExpect(model().attributeExists("summarySubmissionFormData"));

    verify(lookupService, times(1)).getDeclarations(any());
    verify(submissionSummaryDisplayMapper, times(1)).toDeclarationFormDataDynamicOptionList(declarationLookupDetail);
  }

  @Test
  @DisplayName("Test applicationDeclarationPost - success")
  void testApplicationDeclarationPost_success() throws Exception {
    final SummarySubmissionFormData formData = new SummarySubmissionFormData();

    // Mock user session attributes
    final UserDetail mockUser = buildUserDetail();
    final ActiveCase mockActiveCase = ActiveCase.builder()
        .providerId(1)
        .applicationId(1)
        .caseReferenceNumber("caseRef123")
        .clientReferenceNumber("clientRef456")
        .build();

    // Mock evidence service behavior
    final EvidenceDocumentDetails mockEvidenceDocDetails = mock(EvidenceDocumentDetails.class);
    when(evidenceService.getEvidenceDocumentsForCase(anyString(), eq(APPLICATION))).thenReturn(Mono.just(mockEvidenceDocDetails));
    doNothing().when(evidenceService).registerPreviouslyUploadedDocuments(any(), any());
    when(evidenceService.uploadAndUpdateDocuments(any(), anyString(), eq(null), any())).thenReturn(Mono.empty());

    // Mock application service
    final ApplicationDetail mockApplicationDetail = mock(ApplicationDetail.class);
    when(applicationService.getApplication(anyString())).thenReturn(Mono.just(mockApplicationDetail));

    // Mock assessment service
    final AssessmentDetails mockMeansAssessments = mock(AssessmentDetails.class);
    final AssessmentDetails mockMeritsAssessments = mock(AssessmentDetails.class);
    when(assessmentService.getAssessments(anyList(), anyString(), anyString()))
        .thenReturn(Mono.just(mockMeansAssessments))
        .thenReturn(Mono.just(mockMeritsAssessments));

    // Mock application service case creation
    final CaseTransactionResponse mockCaseTransactionResponse = mock(CaseTransactionResponse.class);
    when(applicationService.createCase(any(), any(), any(), any(), any())).thenReturn(mockCaseTransactionResponse);
    when(mockCaseTransactionResponse.getTransactionId()).thenReturn("transactionId123");

    // Perform the test
    mockMvc.perform(post("/application/declaration")
            .flashAttr("summarySubmissionFormData", formData)
            .sessionAttr(USER_DETAILS, mockUser)
            .sessionAttr(ACTIVE_CASE, mockActiveCase))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/%s".formatted(SUBMISSION_CREATE_CASE)));

    // Verify interactions
    verify(declarationSubmissionValidator, times(1)).validate(any(), any());
    verify(evidenceService, times(2)).getEvidenceDocumentsForCase(anyString(), eq(APPLICATION));
    verify(evidenceService, times(1)).registerPreviouslyUploadedDocuments(any(), any());
    verify(evidenceService, times(1)).uploadAndUpdateDocuments(any(), anyString(), eq(null), any());
    verify(applicationService, times(1)).getApplication(anyString());
    verify(assessmentService, times(2)).getAssessments(anyList(), anyString(), anyString());
    verify(applicationService, times(1)).createCase(any(), any(), any(), any(), any());

    // Ensure no other interactions occur
    Mockito.verifyNoMoreInteractions(applicationService, evidenceService, assessmentService);
  }

  @Test
  @DisplayName("Test /application/validate - success and validation failure")
  void testApplicationValidate() throws Exception {
    final String applicationId = "12345";

    // Mocking the data returned by the service methods
    final ApplicationFormData mockProviderDetails = new ApplicationFormData();
    final AddressFormData mockGeneralDetails = new AddressFormData();

    final ProceedingDetail mockProceedingDetail = new ProceedingDetail();
    mockProceedingDetail.setTypeOfOrder(new StringDisplayValue().id("orderTypeId").displayValue("Order Type"));

    final PriorAuthorityDetail mockPriorAuthorityDetail = new PriorAuthorityDetail();

    final ApplicationDetail mockApplicationDetail = new ApplicationDetail();
    mockApplicationDetail.setProceedings(List.of(mockProceedingDetail));
    mockApplicationDetail.setPriorAuthorities(List.of(mockPriorAuthorityDetail));

    final IndividualOpponentFormData mockIndividualOpponent = new IndividualOpponentFormData();
    final List<AbstractOpponentFormData> mockOpponents = List.of(mockIndividualOpponent);

    final ProceedingFlowFormData proceedingFlowFormData = new ProceedingFlowFormData("edit");
    proceedingFlowFormData.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
    proceedingFlowFormData.setProceedingDetails(new ProceedingFormDataProceedingDetails());

    final PriorAuthorityFlowFormData priorAuthorityFlowFormData = new PriorAuthorityFlowFormData("edit");
    priorAuthorityFlowFormData.setPriorAuthorityTypeFormData(new PriorAuthorityTypeFormData());
    priorAuthorityFlowFormData.setPriorAuthorityDetailsFormData(new PriorAuthorityDetailsFormData());

    when(applicationService.getMonoProviderDetailsFormData(applicationId))
        .thenReturn(Mono.just(mockProviderDetails));
    when(applicationService.getMonoCorrespondenceAddressFormData(applicationId))
        .thenReturn(Mono.just(mockGeneralDetails));
    when(applicationService.getApplication(applicationId))
        .thenReturn(Mono.just(mockApplicationDetail));
    when(applicationService.getOpponents(applicationId))
        .thenReturn(mockOpponents);
    when(lookupService.getOrderTypeDescription(any()))
        .thenReturn(Mono.just("Order Type Description"));

    when(proceedingAndCostsMapper.toProceedingFlow(any(), any()))
        .thenReturn(proceedingFlowFormData);
    when(proceedingAndCostsMapper.toPriorAuthorityFlowFormData(any()))
        .thenReturn(priorAuthorityFlowFormData);

    // Mock validation errors
    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("provider.required", "Provider validation failed.");
      return null;
    }).when(providerDetailsValidator).validate(any(), any());

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("correspondence.required", "General details validation failed.");
      return null;
    }).when(correspondenceAddressValidator).validate(any(), any());

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("proceeding.required", "Proceeding validation failed.");
      return null;
    }).when(matterTypeValidator).validate(any(), any());

    final MvcResult mvcResult = mockMvc.perform(get("/application/validate")
            .sessionAttr(APPLICATION_ID, applicationId))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-validation-error-correction"))
        .andExpect(model().attributeExists("providerDetailsErrors"))
        .andExpect(model().attributeExists("generalDetailsErrors"))
        .andExpect(model().attributeExists("proceedingsErrors"))
        .andExpect(model().attributeExists("generalDetailsFormData"))
        .andExpect(model().attributeExists("providerDetailsFormData"));

    verify(applicationService).getMonoProviderDetailsFormData(applicationId);
    verify(applicationService).getMonoCorrespondenceAddressFormData(applicationId);
    verify(applicationService).getApplication(applicationId);
    verify(applicationService).getOpponents(applicationId);
    verify(providerDetailsValidator).validate(any(), any());
    verify(correspondenceAddressValidator).validate(any(), any());
    verify(matterTypeValidator).validate(any(), any());
  }

  @Test
  @DisplayName("Test validateProceedings with proceeding errors")
  void testValidateProceedings_WithErrors() {
    // Mock ProceedingDetail and ProceedingFlowFormData
    final ProceedingDetail proceedingDetail = new ProceedingDetail();
    proceedingDetail.setTypeOfOrder(new StringDisplayValue().id("orderTypeId").displayValue("Order Type"));

    final ProceedingFlowFormData proceedingFlowFormData = new ProceedingFlowFormData("edit");
    proceedingFlowFormData.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
    proceedingFlowFormData.setProceedingDetails(new ProceedingFormDataProceedingDetails());

    when(lookupService.getOrderTypeDescription(any()))
        .thenReturn(Mono.just("Order Type Description"));

    when(proceedingAndCostsMapper.toProceedingFlow(any(), any()))
        .thenReturn(proceedingFlowFormData);

    // Mock validation for MatterTypeDetails
    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("proceedingMatterTypeDetails.required", "Matter Type validation failed.");
      return null;
    }).when(matterTypeValidator).validate(any(), any());

    // Mock validation for ProceedingDetails
    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("proceedingTypeDetails.required", "Proceeding Type validation failed.");
      return null;
    }).when(proceedingTypeValidator).validate(any(), any());

    // Mock validation for FurtherDetails
    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("proceedingFurtherDetails.required", "Further Details validation failed.");
      return null;
    }).when(furtherDetailsValidator).validate(any(), any());

    when(model.containsAttribute("proceedingMatterTypeDetails")).thenReturn(true);
    when(model.getAttribute("proceedingMatterTypeDetails")).thenReturn(List.of("Matter Type validation failed."));

    when(model.containsAttribute("proceedingTypeDetails")).thenReturn(true);
    when(model.getAttribute("proceedingTypeDetails")).thenReturn(List.of("Proceeding Type validation failed."));

    when(model.containsAttribute("proceedingFurtherDetails")).thenReturn(true);
    when(model.getAttribute("proceedingFurtherDetails")).thenReturn(List.of("Further Details validation failed."));

    final List<ProceedingDetail> proceedings = List.of(proceedingDetail);

    final boolean result = Boolean.TRUE.equals(
        applicationSubmissionController.validateProceedings(proceedings, model).block());

    assertTrue(result);
  }

  @Test
  @DisplayName("Test validateProceedings with no proceedings returns false")
  void testValidateProceedings_WithNoProceedings() {
    final List<ProceedingDetail> proceedings = List.of();

    final boolean result = Boolean.TRUE.equals(
        applicationSubmissionController.validateProceedings(proceedings, model).block());

    assertFalse(result);
  }

  @Test
  @DisplayName("Test validateProceedings with null proceedings returns false")
  void testValidateProceedings_WithNullProceedings() {
    final boolean result = Boolean.TRUE.equals(
        applicationSubmissionController.validateProceedings(null, model).block());

    assertFalse(result);
  }

  @Test
  @DisplayName("Test validatePriorAuthorities with PriorAuthority errors")
  void testValidatePriorAuthorities_WithErrors() {
    final PriorAuthorityDetail priorAuthorityDetail = new PriorAuthorityDetail();
    final PriorAuthorityFlowFormData priorAuthorityFlowFormData = new PriorAuthorityFlowFormData("edit");
    priorAuthorityFlowFormData.setPriorAuthorityTypeFormData(new PriorAuthorityTypeFormData());
    priorAuthorityFlowFormData.setPriorAuthorityDetailsFormData(new PriorAuthorityDetailsFormData());

    when(proceedingAndCostsMapper.toPriorAuthorityFlowFormData(any()))
        .thenReturn(priorAuthorityFlowFormData);

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("priorAuthorityType.required", "Prior Authority Type validation failed.");
      return null;
    }).when(priorAuthorityTypeValidator).validate(any(), any());

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("priorAuthorityDetails.required", "Prior Authority Details validation failed.");
      return null;
    }).when(priorAuthorityDetailsValidator).validate(any(), any());

    when(model.containsAttribute("priorAuthorityType")).thenReturn(true);
    when(model.getAttribute("priorAuthorityType")).thenReturn(List.of("Prior Authority Type validation failed."));

    when(model.containsAttribute("priorAuthorityDetails")).thenReturn(true);
    when(model.getAttribute("priorAuthorityDetails")).thenReturn(List.of("Prior Authority Details validation failed."));

    final List<PriorAuthorityDetail> priorAuthorities = List.of(priorAuthorityDetail);

    final boolean result = applicationSubmissionController.validatePriorAuthorities(priorAuthorities, model);

    assertTrue(result);
  }

  @Test
  @DisplayName("Test validatePriorAuthorities with no PriorAuthorities returns false")
  void testValidatePriorAuthorities_WithNoPriorAuthorities() {
    final List<PriorAuthorityDetail> priorAuthorities = List.of();

    boolean result = applicationSubmissionController.validatePriorAuthorities(priorAuthorities, model);

    assertFalse(result);
  }

  @Test
  @DisplayName("Test validatePriorAuthorities with null PriorAuthorities returns false")
  void testValidatePriorAuthorities_WithNullPriorAuthorities() {
    final boolean result = applicationSubmissionController.validatePriorAuthorities(null, model);

    assertFalse(result);
  }

  @Test
  @DisplayName("Test validateOpponents with IndividualOpponentFormData returns true with errors")
  void testValidateOpponents_WithIndividualOpponentErrors() {
    final List<AbstractOpponentFormData> opponents = List.of(new IndividualOpponentFormData());

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("individualOpponent.required", "Error: Individual opponent validation failed.");
      return null;
    }).when(individualOpponentValidator).validate(any(), any());

    when(model.containsAttribute("individualOpponent")).thenReturn(true);
    when(model.getAttribute("individualOpponent")).thenReturn(List.of("Error 1"));

    final boolean result = applicationSubmissionController.validateOpponents(opponents, model);

    assertTrue(result);
  }

  @Test
  @DisplayName("Test validateOpponents with OrganisationOpponentFormData returns true with errors")
  void testValidateOpponents_WithOrganisationOpponentErrors() {
    final List<AbstractOpponentFormData> opponents = List.of(new OrganisationOpponentFormData());

    doAnswer(invocation -> {
      final Errors errors = invocation.getArgument(1);
      errors.reject("organisationOpponent.required", "Error: Organisation opponent validation failed.");
      return null;
    }).when(organisationOpponentValidator).validate(any(), any());

    when(model.containsAttribute("organisationOpponent")).thenReturn(true);
    when(model.getAttribute("organisationOpponent")).thenReturn(List.of("Error 1"));

    final boolean result = applicationSubmissionController.validateOpponents(opponents, model);

    assertTrue(result);
  }

  @Test
  @DisplayName("Test validateOpponents with no opponents returns false")
  void testValidateOpponents_WithNoOpponents() {
    final List<AbstractOpponentFormData> opponents = List.of();

    final boolean result = applicationSubmissionController.validateOpponents(opponents, model);

    assertFalse(result);
  }

  @Test
  @DisplayName("Test validateOpponents with null opponents returns false")
  void testValidateOpponents_WithNullOpponents() {
    final boolean result = applicationSubmissionController.validateOpponents(null, model);

    assertFalse(result);
  }

  @Test
  @DisplayName("Test getErrorsFromModel returns errors when attribute is present")
  void testGetErrorsFromModel_WithAttributePresent() {
    final List<String> expectedErrors = List.of("Error 1", "Error 2");

    when(model.containsAttribute(ERROR_ATTRIBUTE)).thenReturn(true);
    when(model.getAttribute(ERROR_ATTRIBUTE)).thenReturn(expectedErrors);

    final List<String> actualErrors = applicationSubmissionController.getErrorsFromModel(model, ERROR_ATTRIBUTE);

    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  @DisplayName("Test getErrorsFromModel returns empty list when attribute is not present")
  void testGetErrorsFromModel_WithoutAttributePresent() {
    when(model.containsAttribute(ERROR_ATTRIBUTE)).thenReturn(false);

    final List<String> actualErrors = applicationSubmissionController.getErrorsFromModel(model, ERROR_ATTRIBUTE);

    assertEquals(Collections.emptyList(), actualErrors);
  }

  @Test
  @DisplayName("Test POST /application/validate redirects to /application/sections")
  void testApplicationValidatePost_RedirectsToApplicationSections() throws Exception {
    mockMvc.perform(post("/application/validate"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/sections"));
  }


}
