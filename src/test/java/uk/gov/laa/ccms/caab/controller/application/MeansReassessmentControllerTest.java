package uk.gov.laa.ccms.caab.controller.application;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_QUICK_EDIT_TYPE;
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
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.mapper.SubmissionSummaryDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
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
  void startMeansReassessmentBuildsInMemoryApplicationWithoutPersisting() throws Exception {
    when(amendmentService.buildMeansReassessment(ebsCase, user)).thenReturn(amendment);

    mockMvc
        .perform(
            get("/means-reassessment").sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/means-reassessment/summary"))
        // The application is held in the session but not persisted, so there is no id/summary and
        // the case overview never sees a draft (old PUI parity).
        .andExpect(request().sessionAttribute(APPLICATION, amendment))
        .andExpect(request().sessionAttribute(ACTIVE_CASE, notNullValue()))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_ID))
        .andExpect(request().sessionAttributeDoesNotExist(APPLICATION_SUMMARY));

    verify(amendmentService).buildMeansReassessment(ebsCase, user);
    verify(applicationService, never()).getTdsApplications(any(), any(), anyInt(), anyInt());
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
  void deleteMeansReassessmentClearsAssessmentOnly() throws Exception {
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

    // Only the means assessment data is removed, mirroring old PUI's DeleteAssessmentController -
    // the shared draft is left untouched (the case overview ignores a means-reassessment draft).
    verify(assessmentService).deleteAssessments(eq(user), anyList(), eq("CASE123"), eq(null));
    verify(applicationService, never()).abandonApplication(any(), any());
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
        .andExpect(request().sessionAttribute(SUBMISSION_TRANSACTION_ID, "TRANS123"))
        // The post-submission cleanup can only learn that this was a means reassessment from the
        // session: the quick edit type is never persisted against the TDS draft.
        .andExpect(
            request()
                .sessionAttribute(
                    SUBMISSION_QUICK_EDIT_TYPE,
                    QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT));
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
