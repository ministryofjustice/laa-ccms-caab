package uk.gov.laa.ccms.caab.controller.application.section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_VIEW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;

@ExtendWith(SpringExtension.class)
class ApplicationSubmissionControllerTest {

  private MockMvc mockMvc;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private AssessmentService assessmentService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientService clientService;

  @Mock
  private ClientDetailMapper clientDetailsMapper;

  @Mock
  private SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;

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

    Mockito.verify(applicationService, Mockito.times(1))
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

    Mockito.verify(applicationService, Mockito.times(1)).getApplication(any());
    Mockito.verify(assessmentService, Mockito.times(1)).getAssessments(any(), any(), any());
    Mockito.verify(clientService, Mockito.times(1)).getClient(any(), any(), any());
    Mockito.verify(lookupService, Mockito.times(2)).getAssessmentSummaryAttributes(any());
    Mockito.verify(lookupService, Mockito.times(1)).getProceedingSubmissionMappingContext();
    Mockito.verify(lookupService, Mockito.times(1)).getOpponentSubmissionMappingContext();
    Mockito.verify(lookupService, Mockito.times(1)).getGeneralDetailsSubmissionMappingContext();
  }



}
