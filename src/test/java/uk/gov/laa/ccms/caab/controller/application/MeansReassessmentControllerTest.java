package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.validators.declaration.DeclarationSubmissionValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class MeansReassessmentControllerTest {

  @Mock private ApplicationService applicationService;
  @Mock private AmendmentService amendmentService;
  @Mock private AssessmentService assessmentService;
  @Mock private LookupService lookupService;
  @Mock private SubmissionSummaryDisplayMapper submissionSummaryDisplayMapper;
  @Mock private DeclarationSubmissionValidator declarationSubmissionValidator;

  @InjectMocks private MeansReassessmentController controller;

  private MockMvc mockMvc;
  private UserDetail user;
  private ApplicationDetail ebsCase;
  private ApplicationDetail amendment;
  private ActiveCase activeCase;

  @BeforeEach
  void setUp() {
    mockMvc = standaloneSetup(controller).build();
    user = buildUserDetail();
    ebsCase = buildApplicationDetail(123, false, new Date()).caseReferenceNumber("CASE123");
    ebsCase.setAvailableFunctions(List.of(FunctionConstants.SUBMIT_MEANS_REASSESSMENT));
    amendment =
        buildApplicationDetail(456, true, new Date()).id(456).caseReferenceNumber("CASE123");
    activeCase =
        ActiveCase.builder()
            .applicationId(456)
            .caseReferenceNumber("CASE123")
            .providerId(user.getProvider().getId())
            .clientReferenceNumber("CLIENT123")
            .build();
  }

  @Test
  void startMeansReassessmentCreatesDraftWhenNoneExists() throws Exception {
    BaseApplicationDetail createdSummary = new BaseApplicationDetail().id(456);

    when(applicationService.getTdsApplications(any(), eq(user), eq(0), eq(1)))
        .thenReturn(new ApplicationDetails().content(Collections.emptyList()))
        .thenReturn(new ApplicationDetails().content(List.of(createdSummary)));
    when(amendmentService.createMeansReassessmentForCase(ebsCase, user)).thenReturn("456");
    when(applicationService.getApplication("456")).thenReturn(Mono.just(amendment));

    mockMvc
        .perform(
            get("/means-reassessment").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/means-reassessment/summary"))
        .andExpect(request().sessionAttribute(APPLICATION_SUMMARY, createdSummary))
        .andExpect(request().sessionAttribute(APPLICATION, amendment))
        .andExpect(request().sessionAttribute(APPLICATION_ID, "456"));

    verify(amendmentService).createMeansReassessmentForCase(ebsCase, user);
  }

  @Test
  void summaryShowsSubmitForCompletedAssessmentAndPermission() throws Exception {
    AssessmentDetail assessment =
        buildAssessmentDetail(new Date()).status(AssessmentStatus.COMPLETE.getStatus());

    when(assessmentService.getAssessments(anyList(), anyString(), eq("CASE123")))
        .thenReturn(Mono.just(new AssessmentDetails().content(List.of(assessment))));

    mockMvc
        .perform(
            get("/means-reassessment/summary")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name("application/means-reassessment/summary"))
        .andExpect(model().attribute("assessmentStatus", "Complete"))
        .andExpect(model().attribute("assessmentComplete", true))
        .andExpect(model().attribute("canSubmit", true))
        .andExpect(model().attribute("noSubmitPermission", false));
  }

  @Test
  void summaryDoesNotShowSubmitWhenLegacySubmitFunctionIsMissing() throws Exception {
    ebsCase.setAvailableFunctions(Collections.emptyList());
    AssessmentDetail assessment =
        buildAssessmentDetail(new Date()).status(AssessmentStatus.COMPLETE.getStatus());

    when(assessmentService.getAssessments(anyList(), anyString(), eq("CASE123")))
        .thenReturn(Mono.just(new AssessmentDetails().content(List.of(assessment))));

    mockMvc
        .perform(
            get("/means-reassessment/summary")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("assessmentComplete", true))
        .andExpect(model().attribute("canSubmit", false))
        // Completed assessment but no MNSB -> show the no-permission message (mirrors old PUI).
        .andExpect(model().attribute("noSubmitPermission", true));
  }

  @Test
  void deleteMeansReassessmentDeletesAssessmentsAndDraft() throws Exception {
    when(assessmentService.deleteAssessments(eq(user), anyList(), eq("CASE123"), eq(null)))
        .thenReturn(Mono.empty());

    mockMvc
        .perform(
            post("/means-reassessment/delete")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(APPLICATION, amendment)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/case/overview"))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_SUMMARY))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_ID))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION))
        .andExpect(request().sessionAttributeDoesNotExist(ACTIVE_CASE));

    verify(applicationService).abandonApplication(amendment, user);
  }

  @Test
  void submitMeansReassessmentRedirectsToSubmissionPolling() throws Exception {
    AssessmentDetail assessment =
        buildAssessmentDetail(new Date()).status(AssessmentStatus.COMPLETE.getStatus());

    when(assessmentService.getAssessments(
            List.of(AssessmentRulebase.MEANS.getName()),
            user.getProvider().getId().toString(),
            "CASE123"))
        .thenReturn(Mono.just(new AssessmentDetails().content(List.of(assessment))));
    when(amendmentService.submitMeansReassessment(user, amendment, assessment))
        .thenReturn("TRANS123");

    mockMvc
        .perform(
            post("/means-reassessment/declaration")
                .flashAttr("summarySubmissionFormData", new SummarySubmissionFormData())
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(APPLICATION, amendment)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/submit-case"))
        .andExpect(request().sessionAttribute(SUBMISSION_TRANSACTION_ID, "TRANS123"));
  }

  @Test
  void declarationRedirectsToSummaryWhenAssessmentIncomplete() throws Exception {
    AssessmentDetail assessment =
        buildAssessmentDetail(new Date()).status(AssessmentStatus.INCOMPLETE.getStatus());

    when(assessmentService.getAssessments(anyList(), anyString(), eq("CASE123")))
        .thenReturn(Mono.just(new AssessmentDetails().content(List.of(assessment))));

    mockMvc
        .perform(
            get("/means-reassessment/declaration")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/means-reassessment/summary"));
  }

  @Test
  void declarationPageRendersForCompletedAssessment() throws Exception {
    AssessmentDetail assessment =
        buildAssessmentDetail(new Date()).status(AssessmentStatus.COMPLETE.getStatus());

    when(assessmentService.getAssessments(anyList(), anyString(), eq("CASE123")))
        .thenReturn(Mono.just(new AssessmentDetails().content(List.of(assessment))));
    when(lookupService.getDeclarations("APPLICATION"))
        .thenReturn(Mono.just(new DeclarationLookupDetail()));
    when(submissionSummaryDisplayMapper.toDeclarationFormDataDynamicOptionList(any()))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/means-reassessment/declaration")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name("application/means-reassessment/declaration"));
  }
}
