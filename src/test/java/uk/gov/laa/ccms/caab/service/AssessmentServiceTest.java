package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROGRESS_STATUS_TYPES;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.OPPONENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.INCOMPLETE;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetail;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetailMultipleOpponents;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetailMultipleProceedings;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildProceedingsEntityTypeDetail;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildProceedingsEntityTypeDetailWithMultipleScopes;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntitiesForEntityType;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;
import uk.gov.laa.ccms.caab.client.AssessmentApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.mapper.AssessmentMapper;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.AssessmentScreen;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpaAttribute;
import uk.gov.laa.ccms.caab.model.OpaEntity;
import uk.gov.laa.ccms.caab.model.OpaInstance;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryAttributeDisplay;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.data.model.AssessmentSummaryAttributeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseAssessmentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
public class AssessmentServiceTest {

  @Mock private AssessmentApiClient assessmentApiClient;
  @Mock private EbsApiClient ebsApiClient;
  @Mock private SoaApiClient soaApiClient;
  @Mock private LookupService lookupService;
  @Mock private AssessmentMapper assessmentMapper;
  @Mock private CaabApiClient caabApiClient;
  @InjectMocks private AssessmentService assessmentService;

  private final UserDetail user = buildUserDetail();

  @BeforeEach
  void setUp() {
    user.setLoginId("test-user");
    user.setUserType("test-role");
    BaseProvider provider = new BaseProvider();
    provider.setId(123);
    user.setProvider(provider);
  }

  private static final String PROGRESS_STATUS_CODE = "TEST";
  private static final String PROGRESS_STATUS_DESC = "Test";
  private static final Long ASSESSMENT_ID = 1234567L;

  private final Date auditDate = new Date(System.currentTimeMillis());

  private void stubProgressStatusDescriptions() {
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), anyString()))
        .thenAnswer(
            invocation -> {
              final String status = invocation.getArgument(1);
              final String description =
                  switch (status) {
                    case "REQUIRED" -> "Re-assessment Required";
                    case "UNCHANGED" -> "Unchanged";
                    default -> status;
                  };
              return Mono.just(Optional.of(new CommonLookupValueDetail().description(description)));
            });
  }

  @Test
  public void testSaveAssessment_createAssessment() {
    when(assessmentApiClient.createAssessment(any(AssessmentDetail.class), anyString()))
        .thenReturn(Mono.empty());

    final UserDetail user = new UserDetail();
    user.setLoginId("testUser");

    final AssessmentDetail assessmentWithoutId = new AssessmentDetail();

    assessmentService.saveAssessment(user, assessmentWithoutId).block();
    Mockito.verify(assessmentApiClient).createAssessment(assessmentWithoutId, user.getLoginId());
  }

  @Test
  public void testSaveAssessment_updateAssessment() {
    when(assessmentApiClient.updateAssessment(any(), any(AssessmentDetail.class), anyString()))
        .thenReturn(Mono.empty());

    final UserDetail user = new UserDetail();
    user.setLoginId("testUser");

    final AssessmentDetail assessmentWithId = new AssessmentDetail();
    assessmentWithId.setId(123L);

    assessmentService.saveAssessment(user, assessmentWithId).block();
    Mockito.verify(assessmentApiClient)
        .updateAssessment(assessmentWithId.getId(), assessmentWithId, user.getLoginId());
  }

  @Test
  void getAssessments_ReturnsAssessmentDetails_Success() {
    final String assessmentName = "meansAssessment";
    final String providerId = "1";
    final String caseReferenceNumber = "12345";

    when(assessmentApiClient.getAssessments(any(), anyString(), anyString()))
        .thenReturn(just(new AssessmentDetails()));

    final Mono<AssessmentDetails> result =
        assessmentService.getAssessments(List.of(assessmentName), providerId, caseReferenceNumber);

    StepVerifier.create(result).expectNextMatches(Objects::nonNull).verifyComplete();
  }

  @Test
  void deleteAssessments_Success() {
    final String assessmentName = "meansAssessment";
    final String caseReferenceNumber = "12345";
    final String status = "COMPLETE";

    final UserDetail user = buildUserDetail();

    when(assessmentApiClient.deleteAssessments(
            eq(List.of(assessmentName)),
            eq(user.getProvider().getId().toString()),
            eq(caseReferenceNumber),
            eq(status),
            eq(user.getLoginId())))
        .thenReturn(Mono.empty());

    final Mono<Void> result =
        assessmentService.deleteAssessments(
            user, List.of(assessmentName), caseReferenceNumber, status);

    StepVerifier.create(result).expectComplete().verify();
  }

  @Test
  void testCalculateAssessmentStatuses_assessmentsNotStarted() {
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    final UserDetail user = buildUserDetail();

    assessmentService.calculateAssessmentStatuses(application, null, null, user);

    assertNull(application.getMeansAssessmentStatus());
    assertNull(application.getMeritsAssessmentStatus());
  }

  @Test
  void testCalculateAssessmentStatuses_startedMeansAssessment() {
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    final AssessmentDetail meansAssessment =
        new AssessmentDetail().name(MEANS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(application, meansAssessment, null, user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());
  }

  @ParameterizedTest
  @CsvSource({"COMPLETE", "ERROR"})
  void testCalculateAssessmentStatuses_meansAssessment_reassessmentRequired(
      final String assessmentStatus) {
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    final AssessmentDetail meansAssessment =
        new AssessmentDetail().id(ASSESSMENT_ID).name(MEANS.getName()).status(assessmentStatus);

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    when(assessmentApiClient.patchAssessment(eq(ASSESSMENT_ID), eq(user.getLoginId()), any()))
        .thenReturn(Mono.empty());

    assessmentService.calculateAssessmentStatuses(application, meansAssessment, null, user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());

    verify(assessmentApiClient).patchAssessment(eq(ASSESSMENT_ID), eq(user.getLoginId()), any());
  }

  @ParameterizedTest
  @CsvSource({"COMPLETE", "ERROR"})
  void testCalculateAssessmentStatuses_meritsAssessment_reassessmentRequired(
      final String assessmentStatus) {
    // A proceeding changed a minute after the merits assessment was created - old PUI's key-change
    // rule. Without a key change nothing on an application can require a merits reassessment.
    final Date meritsCreated = new Date(System.currentTimeMillis() - 60_000);

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .auditTrail(
                        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(new Date())));

    final AssessmentDetail meritsAssessment =
        new AssessmentDetail()
            .id(ASSESSMENT_ID)
            .name(MERITS.getName())
            .status(assessmentStatus)
            .auditDetail(new AuditDetail().created(meritsCreated));

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    when(assessmentApiClient.patchAssessment(eq(ASSESSMENT_ID), eq(user.getLoginId()), any()))
        .thenReturn(Mono.empty());

    assessmentService.calculateAssessmentStatuses(application, null, meritsAssessment, user);

    assertNull(application.getMeansAssessment());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);

    verify(assessmentApiClient).patchAssessment(eq(ASSESSMENT_ID), eq(user.getLoginId()), any());
  }

  @Test
  void testCalculateAssessmentStatuses_meansAssessment_amendment_assessmentAmended() {
    final ApplicationDetail application =
        new ApplicationDetail().amendment(true).meansAssessmentAmended(true);

    final AssessmentDetail meansAssessment =
        new AssessmentDetail().id(ASSESSMENT_ID).name(MEANS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(application, meansAssessment, null, user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());
  }

  @Test
  void testCalculateAssessmentStatuses_meritsAssessment_amendment_assessmentAmended() {
    final ApplicationDetail application =
        new ApplicationDetail().amendment(true).meritsAssessmentAmended(true);

    final AssessmentDetail meritsAssessment =
        new AssessmentDetail().id(ASSESSMENT_ID).name(MERITS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(application, null, meritsAssessment, user);

    assertNull(application.getMeansAssessment());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);
  }

  @Test
  void testCalculateAssessmentStatuses_amendment_flagsMeritsAssessmentAmended() {
    // Amendment where the merits-amended flag has NOT been set up-front (the Bug A scenario).
    final ApplicationDetail application = new ApplicationDetail().amendment(true);

    final AssessmentDetail meritsAssessment =
        new AssessmentDetail().id(ASSESSMENT_ID).name(MERITS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(application, null, meritsAssessment, user);

    // The merits-amended tracking block flags the merits as amended so its status is taken from the
    // assessment rather than being forced to "Unchanged"/"Required".
    assertTrue(application.getMeritsAssessmentAmended());
    assertEquals(PROGRESS_STATUS_DESC, application.getMeritsAssessmentStatus());
  }

  @Test
  void testCalculateAssessmentStatuses_amendment_statusUnchanged_doesNotFlagMeritsAmended() {
    // The stored merits status holds the lookup DESCRIPTION; the assessment's raw status resolves
    // to the same description, so nothing has changed and the merits must NOT be re-flagged as
    // amended.
    final ApplicationDetail application =
        new ApplicationDetail().amendment(true).meritsAssessmentStatus(PROGRESS_STATUS_DESC);

    final AssessmentDetail meritsAssessment =
        new AssessmentDetail().id(ASSESSMENT_ID).name(MERITS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    when(assessmentApiClient.patchAssessment(eq(ASSESSMENT_ID), eq(user.getLoginId()), any()))
        .thenReturn(Mono.empty());

    assessmentService.calculateAssessmentStatuses(application, null, meritsAssessment, user);

    assertNull(application.getMeritsAssessmentAmended());
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_entityTypeNull_assertsTrue() {
    final ApplicationDetail application = new ApplicationDetail();

    final boolean result =
        assessmentService.checkAssessmentForProceedingKeyChange(application, null);

    assertTrue(result);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetail();

    final boolean result =
        assessmentService.checkAssessmentForProceedingKeyChange(
            application, proceedingsEntityTypeDetail);

    assertFalse(result);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_multipleScopeLimitations_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(789)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation)))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetailWithMultipleScopes();

    final boolean result =
        assessmentService.checkAssessmentForProceedingKeyChange(
            application, proceedingsEntityTypeDetail);

    assertFalse(result);
  }

  @ParameterizedTest
  @CsvSource({
    // matter type difference
    "123, OTHER, TEST, TEST, TEST",

    // proceeding type difference
    "123, TEST, OTHER, TEST, TEST",

    // client involvement difference
    "123, TEST, TEST, OTHER, TEST",

    // scope limitation difference
    "123, TEST, TEST, TEST, OTHER"
  })
  void testCheckAssessmentForProceedingKeyChange_assertsTrue(
      final Integer proceedingId,
      final String matterType,
      final String proceedingType,
      final String clientInvolvement,
      final String scopeLimitation) {
    final ApplicationDetail application =
        new ApplicationDetail()
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(proceedingId)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetail();

    final boolean result =
        assessmentService.checkAssessmentForProceedingKeyChange(
            application, proceedingsEntityTypeDetail);

    assertTrue(result);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_proceedingNotInAssessment_assertsTrue() {
    // A proceeding missing from the assessment was added after it was run, so the assessment must
    // be redone (old PUI AssessmentHelper.isMeritsReassessmentRequired).
    final ApplicationDetail application =
        new ApplicationDetail()
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(789)
                    .matterType(new StringDisplayValue().id("TEST"))
                    .proceedingType(new StringDisplayValue().id("TEST"))
                    .clientInvolvement(new StringDisplayValue().id("TEST"))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id("TEST"))));

    final boolean result =
        assessmentService.checkAssessmentForProceedingKeyChange(
            application, buildProceedingsEntityTypeDetail());

    assertTrue(result);
  }

  @Test
  void testCalculateAssessmentStatuses_startedMeritsAssessment() {
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    final AssessmentDetail meritsAssessment =
        new AssessmentDetail().name(MERITS.getName()).status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail().code(PROGRESS_STATUS_CODE).description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any()))
        .thenReturn(Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(application, null, meritsAssessment, user);

    assertNull(application.getMeansAssessmentStatus());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);
  }

  @Test
  void testCalculateAssessmentStatuses_withMeritsReassessmentRequiredTrue() {
    final ApplicationDetail application = new ApplicationDetail();
    application.setAmendment(false);
    application.setMeritsReassessmentRequired(true);

    final AssessmentDetail meansAssessment = new AssessmentDetail();
    meansAssessment.setId(101L);
    meansAssessment.setName(MEANS.getName());
    meansAssessment.setStatus("COMPLETE");

    final AssessmentDetail meritsAssessment = new AssessmentDetail();
    meritsAssessment.setId(102L);
    meritsAssessment.setName(MERITS.getName());
    meritsAssessment.setStatus("COMPLETE");

    when(assessmentApiClient.patchAssessment(anyLong(), anyString(), any()))
        .thenReturn(Mono.empty());
    when(lookupService.getCommonValue(anyString(), anyString()))
        .thenReturn(
            Mono.just(
                Optional.of(new CommonLookupValueDetail().description("Re-assessment Required"))));

    assessmentService.calculateAssessmentStatuses(
        application, meansAssessment, meritsAssessment, user);

    // Means is flagged twice, merits only by the reassessment-required block: with no proceedings
    // or opponents there is no key-change date, which every old PUI merits rule depends on.
    verify(assessmentApiClient, times(2)).patchAssessment(eq(101L), anyString(), any());
    verify(assessmentApiClient, times(1)).patchAssessment(eq(102L), anyString(), any());
    assertEquals("Re-assessment Required", application.getMeansAssessmentStatus());
    assertEquals("Re-assessment Required", application.getMeritsAssessmentStatus());
  }

  @Test
  void
      testIsReassessmentRequired_amendment_noMeritsAssessment_substantiveApp_emergencyCert_returnsTrue() {
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .applicationType(
                new uk.gov.laa.ccms.caab.model.ApplicationType().id(APP_TYPE_SUBSTANTIVE));

    final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase =
        new uk.gov.laa.ccms.soa.gateway.model.CaseDetail()
            .applicationDetails(
                new uk.gov.laa.ccms.soa.gateway.model.SubmittedApplicationDetails()
                    .proceedings(Collections.emptyList()))
            .certificateType(APP_TYPE_EMERGENCY);

    when(soaApiClient.getCase(eq("CASE-123"), anyString(), anyString()))
        .thenReturn(Mono.just(ebsCase));

    // No merits assessment + substantive amendment of an emergency certificate => reassessment
    // required (old PUI keys this off assessment == null, not the checkpoint).
    final boolean result =
        assessmentService.isReassessmentRequired(application, MERITS, null, null, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_amendment_noMeritsAssessment_costLimitIncreased_returnsTrue() {
    final Date amendmentCreated = new Date(System.currentTimeMillis() - 60_000);

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().created(amendmentCreated))
            .addProceedingsItem(
                new ProceedingDetail()
                    .auditTrail(
                        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(amendmentCreated)))
            .costLimit(
                new uk.gov.laa.ccms.caab.model.CostLimitDetail()
                    .limitAtTimeOfMerits(new java.math.BigDecimal("1000")))
            .costs(
                new uk.gov.laa.ccms.caab.model.CostStructureDetail()
                    .requestedCostLimitation(new java.math.BigDecimal("2000")));

    // Cost limit increased since the merits cost limit was recorded => reassessment required even
    // though no merits assessment has been performed yet (old PUI checks this at the top level).
    final boolean result =
        assessmentService.isReassessmentRequired(application, MERITS, null, null, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_amendment_noRequestedOrDefaultCostLimit_returnsTrueWithoutNpe() {
    final Date amendmentCreated = new Date(System.currentTimeMillis() - 60_000);

    // limitAtTimeOfMerits is set but neither requested nor default cost limit is known. This must
    // be
    // treated as reassessment-required rather than throwing on BigDecimal.compareTo(null).
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().created(amendmentCreated))
            .addProceedingsItem(
                new ProceedingDetail()
                    .auditTrail(
                        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(amendmentCreated)))
            .costLimit(
                new uk.gov.laa.ccms.caab.model.CostLimitDetail()
                    .limitAtTimeOfMerits(new java.math.BigDecimal("1000")))
            .costs(new uk.gov.laa.ccms.caab.model.CostStructureDetail());

    final boolean result =
        assessmentService.isReassessmentRequired(application, MERITS, null, null, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_amendment_hasCheckpoint_returnsFalse() {
    final ApplicationDetail application =
        new ApplicationDetail().amendment(true).caseReferenceNumber("CASE-123");

    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setName(MERITS.getName());
    assessment.setCheckpoint(
        new uk.gov.laa.ccms.caab.assessment.model.AssessmentCheckpointDetail());

    // No EBS case is stubbed: an existing assessment rules out the emergency-certificate rule, so
    // the lookup must not happen.

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertFalse(result);
  }

  @Test
  void testCalculateAssessmentStatuses_amendmentNoMeansAssessment_ecfCase_leavesMeansUnchanged() {
    // Old PUI has no ECF means rule: on an amendment, means only requires reassessment for a
    // substantive amendment of an emergency certificate (AssessmentHelper, LSC-1783).
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .applicationType(new ApplicationType().id(APP_TYPE_EXCEPTIONAL_CASE_FUNDING));

    // No EBS case is stubbed: an ECF application type rules out the emergency-certificate rule
    // before the lookup, so it must not happen.
    stubProgressStatusDescriptions();

    assessmentService.calculateAssessmentStatuses(application, null, null, user);

    assertEquals("Unchanged", application.getMeansAssessmentStatus());
    assertEquals("Unchanged", application.getMeritsAssessmentStatus());
  }

  @Test
  void
      testCalculateAssessmentStatuses_amendmentSubstantiveEmergency_setsBothMeansAndMeritsRequired() {
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .applicationType(new ApplicationType().id(APP_TYPE_SUBSTANTIVE));

    final uk.gov.laa.ccms.soa.gateway.model.CaseDetail ebsCase =
        new uk.gov.laa.ccms.soa.gateway.model.CaseDetail()
            .applicationDetails(
                new uk.gov.laa.ccms.soa.gateway.model.SubmittedApplicationDetails()
                    .proceedings(Collections.emptyList()))
            .certificateType(APP_TYPE_EMERGENCY);

    when(soaApiClient.getCase(eq("CASE-123"), anyString(), anyString()))
        .thenReturn(Mono.just(ebsCase));
    stubProgressStatusDescriptions();

    assessmentService.calculateAssessmentStatuses(application, null, null, user);

    // Both assessments require reassessment: a substantive amendment of an emergency certificate
    // with no assessment is old PUI's single amendment rule for means as well as merits.
    assertEquals("Re-assessment Required", application.getMeansAssessmentStatus());
    assertEquals("Re-assessment Required", application.getMeritsAssessmentStatus());
  }

  @Test
  void testIsReassessmentRequired_amendmentMeritsKeyChangeAfterAmendmentCreated_returnsTrue() {
    final Date amendmentCreated = new Date(System.currentTimeMillis() - 60_000);
    final Date proceedingChanged = new Date();

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .meritsAssessmentAmended(false)
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().created(amendmentCreated))
            .addProceedingsItem(
                new ProceedingDetail()
                    .auditTrail(
                        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(proceedingChanged)));

    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setName(MERITS.getName());
    assessment.setCheckpoint(
        new uk.gov.laa.ccms.caab.assessment.model.AssessmentCheckpointDetail());

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_assessmentNull_assertsFalse() {
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    final boolean result = assessmentService.isReassessmentRequired(application, null, user);

    assertFalse(result);
  }

  @Test
  @DisplayName("A proceeding deleted since the means assessment was run requires a reassessment")
  void testIsReassessmentRequired_assessmentHasMoreProceedingsThanApplication_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    // The proceeding count comparison is old PUI's "Condition 021", which lives in the means rules
    // only (AssessmentHelper.isMeansReassessmentRequired). Merits learns of a deleted proceeding
    // from the meritsReassessmentRequired flag that ApplicationService.deleteProceeding sets.
    final AssessmentDetail assessment = buildAssessmentDetailMultipleProceedings();
    assessment.setName(MEANS.getName());

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_meritsReassessmentRequired_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    // The proceeding is dated as the assessment was created, so the key-change rule cannot fire and
    // the flag is the only thing left that can require the reassessment.
    final ApplicationDetail application =
        new ApplicationDetail()
            .meritsReassessmentRequired(true)
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_opponentsHaveBeenUpdated_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    // lased saved date = now - 11 seconds
    final long currentTime = System.currentTimeMillis();
    final Date lastSaved = new Date(currentTime - 11000);
    final Date currentDate = new Date(currentTime);

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(
                        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(currentDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(lastSaved);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_assessmentHasMoreOpponentsThanApplication_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetailMultipleOpponents(auditDate);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertTrue(result);
  }

  @ParameterizedTest
  @CsvSource({"meritsAssessment, true", "meansAssessment, false"})
  void testIsReassessmentRequired_costLimitDifference(
      final String assessmentName, final boolean expectedResult) {

    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(999.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);
    assessment.setName(assessmentName);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment, user);

    assertEquals(expectedResult, result);
  }

  @Test
  @DisplayName(
      "A requested cost limit below the limit at the time of merits does not require a merits "
          + "reassessment")
  void testIsReassessmentRequired_costLimitBelowLimitAtTimeOfMerits_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    // Requested limit (1000) is BELOW the limit captured at the time of merits (2000). Old PUI only
    // reassesses when the limit RISES above it (AssessmentHelper: compareTo(...) < 0), because the
    // provider only has to justify higher costs.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(2000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);
    assessment.setName(MERITS.getName());

    assertFalse(assessmentService.isReassessmentRequired(application, assessment, user));
  }

  @Test
  @DisplayName(
      "A requested cost limit above the limit at the time of merits requires a merits "
          + "reassessment")
  void testIsReassessmentRequired_costLimitAboveLimitAtTimeOfMerits_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    // Requested limit (3000) is ABOVE the limit captured at the time of merits (2000): the provider
    // must justify the higher costs, so the merits assessment must be redone.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(2000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(3000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);
    assessment.setName(MERITS.getName());

    assertTrue(assessmentService.isReassessmentRequired(application, assessment, user));
  }

  @Test
  @DisplayName("An unchanged requested cost limit does not by itself require a merits reassessment")
  void testIsReassessmentRequired_costLimitUnchanged_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);
    assessment.setName(MERITS.getName());

    assertFalse(assessmentService.isReassessmentRequired(application, assessment, user));
  }

  @Test
  @DisplayName(
      "A key change after the merits assessment was created requires a merits reassessment on an "
          + "application, as it does on an amendment")
  void testIsReassessmentRequired_applicationMeritsKeyChangeAfterAssessment_assertsTrue() {
    final Date meritsCreated = new Date(System.currentTimeMillis() - 60_000);

    // An opponent edited a minute after the merits assessment was created. Old PUI applies this
    // rule to applications and amendments alike.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Organisation")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(new Date())))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(meritsCreated);
    assessment.setName(MERITS.getName());

    assertTrue(
        assessmentService.isReassessmentRequired(application, MERITS, assessment, null, user));
  }

  @Test
  @DisplayName(
      "A key change does not require a merits reassessment when the means assessment was the last "
          + "one run")
  void testIsReassessmentRequired_applicationMeritsKeyChangeButMeansLast_assertsFalse() {
    final Date meritsCreated = new Date(System.currentTimeMillis() - 60_000);

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Organisation")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(new Date())))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail meritsAssessment = buildAssessmentDetail(meritsCreated);
    meritsAssessment.setName(MERITS.getName());

    // Means saved after merits: the key change is the means assessment's own, and a means change
    // must not flip merits to reassessment-required (old PUI's !meansLast guard).
    final AssessmentDetail meansAssessment = buildAssessmentDetail(new Date());
    meansAssessment.setName(MEANS.getName());

    assertFalse(
        assessmentService.isReassessmentRequired(
            application, MERITS, meritsAssessment, meansAssessment, user));
  }

  @Test
  @DisplayName("An updated opponent does not require a means reassessment on an application")
  void testIsReassessmentRequired_applicationMeansOpponentUpdated_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    // Only the proceedings drive a means reassessment: old PUI's means rules never look at the
    // opponents, the cost limit or the scope limitations, all of which belong to merits.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id(matterType))
                    .proceedingType(new StringDisplayValue().id(proceedingType))
                    .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id(scopeLimitation))))
            .addOpponentsItem(
                new OpponentDetail()
                    .id(234)
                    .type("Individual")
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(new Date())))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(999.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(5000.00)));

    final AssessmentDetail assessment =
        buildAssessmentDetail(new Date(System.currentTimeMillis() - 60_000));
    assessment.setName(MEANS.getName());

    assertFalse(assessmentService.isReassessmentRequired(application, assessment, user));
  }

  @Test
  @DisplayName("An amendment with an existing assessment does not fetch the EBS case")
  void isReassessmentRequired_amendmentWithAssessment_skipsEbsCaseLookup() {
    // The emergency-certificate rule can only fire when no assessment exists, so the blocking SOA
    // call must be skipped once one does - it runs on every amendment submit-validation.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .caseReferenceNumber("CASE-123")
            .applicationType(new ApplicationType().id(APP_TYPE_SUBSTANTIVE));

    final AssessmentDetail meansAssessment = new AssessmentDetail();
    meansAssessment.setName(MEANS.getName());

    assertFalse(assessmentService.isMeansReassessmentRequired(application, meansAssessment, user));

    verify(soaApiClient, never()).getCase(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Null proceedings are treated as none rather than throwing")
  void checkAssessmentForProceedingKeyChange_nullProceedings_doesNotThrow() {
    // The key-change date can come from opponents alone, so this is reachable with no proceedings.
    final ApplicationDetail application = new ApplicationDetail();
    application.setProceedings(null);

    assertFalse(
        assessmentService.checkAssessmentForProceedingKeyChange(
            application, buildProceedingsEntityTypeDetail()));
    assertTrue(assessmentService.checkAssessmentForProceedingKeyChange(application, null));
  }

  @Test
  @DisplayName("A changed scope limitation requires a merits reassessment but not a means one")
  void testIsReassessmentRequired_applicationScopeLimitationChanged_meritsOnly() {
    // Scope changed from the recorded "TEST": a merits concern only. The proceeding is dated as
    // the assessment was created so the key-change rule cannot fire and mask the difference.
    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(false)
            .addProceedingsItem(
                new ProceedingDetail()
                    .id(123)
                    .matterType(new StringDisplayValue().id("TEST"))
                    .proceedingType(new StringDisplayValue().id("TEST"))
                    .clientInvolvement(new StringDisplayValue().id("TEST"))
                    .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate))
                    .addScopeLimitationsItem(
                        new ScopeLimitationDetail()
                            .scopeLimitation(new StringDisplayValue().id("CHANGED"))))
            .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
            .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail meansAssessment = buildAssessmentDetail(auditDate);
    meansAssessment.setName(MEANS.getName());

    final AssessmentDetail meritsAssessment = buildAssessmentDetail(auditDate);
    meritsAssessment.setName(MERITS.getName());

    assertFalse(assessmentService.isReassessmentRequired(application, meansAssessment, user));
    assertTrue(assessmentService.isReassessmentRequired(application, meritsAssessment, user));
  }

  @ParameterizedTest
  @CsvSource({"proceeding1,,opponent1,,0,0", "P_123,,OPPONENT_234,,1,1", ",123,,234,1,1"})
  void testCleanupData(
      final String proceedingEbsId,
      final Integer proceedingId,
      final String opponentEbsId,
      final Integer opponentId,
      final int proceedingsExpected,
      final int opponentsExpected) {
    // Contains proceeding P_123 and opponent OPPONENT_234
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    // Set up opponents and proceedings for the application
    final ApplicationDetail application = new ApplicationDetail().amendment(false);

    if (proceedingEbsId != null || proceedingId != null) {
      final ProceedingDetail proceeding = new ProceedingDetail();
      if (proceedingEbsId != null) {
        proceeding.setEbsId(proceedingEbsId);
      }
      if (proceedingId != null) {
        proceeding.setId(proceedingId);
      }
      application.addProceedingsItem(proceeding);
    }

    if (opponentEbsId != null || opponentId != null) {
      final OpponentDetail opponent = new OpponentDetail();
      if (opponentEbsId != null) {
        opponent.setEbsId(opponentEbsId);
      }
      if (opponentId != null) {
        opponent.setId(opponentId);
      }
      application.addOpponentsItem(opponent);
    }

    // Call the cleanupData method
    assessmentService.cleanupData(assessment, application);

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);
    final List<AssessmentEntityDetail> opponentEntities =
        getAssessmentEntitiesForEntityType(assessment, OPPONENT);

    assertEquals(proceedingsExpected, proceedingEntities.size());
    assertEquals(opponentsExpected, opponentEntities.size());
  }

  @Test
  @DisplayName(
      "cleanupData retains draft amendment proceedings that are only present in EBS, not in the "
          + "live application proceedings")
  void cleanupDataRetainsDraftAmendmentProceedings() {
    // Assessment contains proceeding P_123 (and opponent OPPONENT_234).
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    // The proceeding only exists as a draft amendment proceeding in EBS - it is NOT in the
    // live application proceedings. Without amendment-aware cleanup it would be deleted.
    final ProceedingDetail draftProceeding = new ProceedingDetail().ebsId("P_123");

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .proceedings(new ArrayList<>())
            .opponents(new ArrayList<>())
            .amendmentProceedingsInEbs(new ArrayList<>(List.of(draftProceeding)));

    assessmentService.cleanupData(assessment, application);

    final List<AssessmentEntityDetail> proceedingEntities =
        getAssessmentEntitiesForEntityType(assessment, PROCEEDING);
    final List<AssessmentEntityDetail> opponentEntities =
        getAssessmentEntitiesForEntityType(assessment, OPPONENT);

    // Draft amendment proceeding retained; opponent with no application match removed.
    assertEquals(1, proceedingEntities.size());
    assertEquals("P_123", proceedingEntities.getFirst().getName());
    assertEquals(0, opponentEntities.size());
  }

  @Test
  void testGetAssessmentOpponentMappingContexts() {
    final ApplicationDetail application = new ApplicationDetail();
    final OpponentDetail opponent1 = new OpponentDetail().title("MR");
    final OpponentDetail opponent2 = new OpponentDetail().title("MS");
    application.addOpponentsItem(opponent1);
    application.addOpponentsItem(opponent2);

    final CommonLookupValueDetail titleLookupMr =
        new CommonLookupValueDetail().code("MR").description("Mr");
    final CommonLookupValueDetail titleLookupMs =
        new CommonLookupValueDetail().code("MS").description("Ms");

    when(lookupService.getCommonValue(eq(COMMON_VALUE_CONTACT_TITLE), eq("MR")))
        .thenReturn(Mono.just(Optional.of(titleLookupMr)));
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CONTACT_TITLE), eq("MS")))
        .thenReturn(Mono.just(Optional.of(titleLookupMs)));

    final List<AssessmentOpponentMappingContext> result =
        assessmentService.getAssessmentOpponentMappingContexts(application);

    assertNotNull(result);
    assertEquals(2, result.size());

    final AssessmentOpponentMappingContext context1 = result.getFirst();
    assertEquals(opponent1, context1.getOpponent());
    assertEquals(titleLookupMr, context1.getTitleCommonLookupValue());

    final AssessmentOpponentMappingContext context2 = result.get(1);
    assertEquals(opponent2, context2.getOpponent());
    assertEquals(titleLookupMs, context2.getTitleCommonLookupValue());

    verify(lookupService).getCommonValue(COMMON_VALUE_CONTACT_TITLE, "MR");
    verify(lookupService).getCommonValue(COMMON_VALUE_CONTACT_TITLE, "MS");
  }

  @Test
  void testFindOrCreate_existingAssessment() {
    final String providerId = "providerId";
    final String referenceId = "referenceId";
    final String assessmentName = "assessmentName";

    final AssessmentDetail existingAssessment =
        new AssessmentDetail()
            .caseReferenceNumber(referenceId)
            .providerId(providerId)
            .name(assessmentName)
            .status(INCOMPLETE.getStatus());
    final AssessmentDetails assessmentDetails = new AssessmentDetails();
    assessmentDetails.setContent(List.of(existingAssessment));

    when(assessmentService.getAssessments(
            eq(List.of(assessmentName)), eq(providerId), eq(referenceId)))
        .thenReturn(Mono.just(assessmentDetails));

    final AssessmentDetail result =
        assessmentService.findOrCreate(providerId, referenceId, assessmentName);

    assertNotNull(result);
    assertEquals(existingAssessment, result);
  }

  @Test
  void testFindOrCreate_newAssessment() {
    final String providerId = "providerId";
    final String referenceId = "referenceId";
    final String assessmentName = "assessmentName";

    when(assessmentService.getAssessments(
            eq(List.of(assessmentName)), eq(providerId), eq(referenceId)))
        .thenReturn(Mono.just(new AssessmentDetails()));

    final AssessmentDetail result =
        assessmentService.findOrCreate(providerId, referenceId, assessmentName);

    assertNotNull(result);
    assertEquals(referenceId, result.getCaseReferenceNumber());
    assertEquals(providerId, result.getProviderId());
    assertEquals(assessmentName, result.getName());
    assertEquals(INCOMPLETE.getStatus(), result.getStatus());
  }

  @Test
  void prepopulateAssessmentFromEbsAddsMissingAssessmentData() {
    final ApplicationDetail application =
        new ApplicationDetail()
            .meansAssessment(
                buildEbsAssessmentResult("global", "CASE-123", "EBS_ATTRIBUTE", "EBS_VALUE"));
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("global")
                        .entities(
                            List.of(
                                new AssessmentEntityDetail()
                                    .name("CASE-123")
                                    .attributes(new ArrayList<>())))));

    assessmentService.prepopulateAssessmentFromEbs(
        application, AssessmentRulebase.MEANS, assessment);

    final AssessmentEntityTypeDetail entityType = assessment.getEntityTypes().getFirst();
    final AssessmentEntityDetail entity = entityType.getEntities().getFirst();
    final AssessmentAttributeDetail attribute = entity.getAttributes().getFirst();

    assertEquals("global", entityType.getName());
    assertEquals("CASE-123", entity.getName());
    assertEquals("EBS_ATTRIBUTE", attribute.getName());
    assertEquals("EBS_VALUE", attribute.getValue());
    assertTrue(attribute.getPrepopulated());
  }

  @Test
  void prepopulateAssessmentFromEbsDoesNotOverwriteExistingAssessmentValues() {
    final ApplicationDetail application =
        new ApplicationDetail()
            .meansAssessment(
                buildEbsAssessmentResult("global", "CASE-123", "EXISTING_ATTRIBUTE", "EBS_VALUE"));
    final AssessmentAttributeDetail existingAttribute =
        new AssessmentAttributeDetail().name("EXISTING_ATTRIBUTE").value("CURRENT_VALUE");
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("global")
                        .entities(
                            List.of(
                                new AssessmentEntityDetail()
                                    .name("CASE-123")
                                    .attributes(new ArrayList<>(List.of(existingAttribute)))))));

    assessmentService.prepopulateAssessmentFromEbs(
        application, AssessmentRulebase.MEANS, assessment);

    final AssessmentAttributeDetail attribute =
        assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes().getFirst();

    assertEquals("CURRENT_VALUE", attribute.getValue());
  }

  @Test
  void prepopulateAssessmentFromEbsDoesNotCreateUnmatchedProceedingEntities() {
    final ApplicationDetail application =
        new ApplicationDetail()
            .meritsAssessment(
                buildEbsAssessmentResult(
                    "proceeding", "STALE_PROCEEDING", "EBS_ATTRIBUTE", "EBS_VALUE"));
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("PROCEEDING")
                        .entities(
                            new ArrayList<>(
                                List.of(
                                    new AssessmentEntityDetail()
                                        .name("P_123")
                                        .attributes(new ArrayList<>()))))));

    assessmentService.prepopulateAssessmentFromEbs(
        application, AssessmentRulebase.MERITS, assessment);

    final List<AssessmentEntityDetail> proceedingEntities =
        assessment.getEntityTypes().getFirst().getEntities();

    assertEquals(1, proceedingEntities.size());
    assertEquals("P_123", proceedingEntities.getFirst().getName());
    assertTrue(proceedingEntities.getFirst().getAttributes().isEmpty());
  }

  private CaseAssessmentDetail ebsRow(
      final String entity,
      final String instance,
      final String attribute,
      final String value,
      final boolean userDefined) {
    return new CaseAssessmentDetail()
        .entityName(entity)
        .instanceLabel(instance)
        .attributeName(attribute)
        .attributeValue(value)
        .attributeType("text")
        .attributeUserDefinedIndicator(userDefined);
  }

  private AssessmentDetail globalAssessment(final String caseRef) {
    return new AssessmentDetail()
        .entityTypes(
            new ArrayList<>(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("global")
                        .entities(
                            new ArrayList<>(
                                List.of(
                                    new AssessmentEntityDetail()
                                        .name(caseRef)
                                        .attributes(new ArrayList<>())))))));
  }

  @Test
  @DisplayName("mergeEbsAssessmentData adds EBS-sourced attributes when not selecting user-entered")
  void mergeEbsAssessmentDataAddsEbsSourcedAttributes() {
    final AssessmentDetail assessment = globalAssessment("CASE-123");
    final List<CaseAssessmentDetail> rows =
        List.of(
            ebsRow("global", "CASE-123", "GB_DECL_B_38WP3_13A_PREV", "~\t~", false),
            ebsRow("global", "CASE-123", "USER_ANSWER", "yes", true));

    assessmentService.mergeEbsAssessmentData(assessment, rows, false);

    final List<AssessmentAttributeDetail> attributes =
        assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes();
    assertEquals(1, attributes.size());
    assertEquals("GB_DECL_B_38WP3_13A_PREV", attributes.getFirst().getName());
    assertEquals("~\t~", attributes.getFirst().getValue());
    assertTrue(attributes.getFirst().getPrepopulated());
  }

  @Test
  @DisplayName("mergeEbsAssessmentData selects only user-entered attributes when requested")
  void mergeEbsAssessmentDataSelectsUserEnteredAttributes() {
    final AssessmentDetail assessment = globalAssessment("CASE-123");
    final List<CaseAssessmentDetail> rows =
        List.of(
            ebsRow("global", "CASE-123", "EBS_SOURCED", "x", false),
            ebsRow("global", "CASE-123", "USER_ANSWER", "yes", true));

    assessmentService.mergeEbsAssessmentData(assessment, rows, true);

    final List<AssessmentAttributeDetail> attributes =
        assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes();
    assertEquals(1, attributes.size());
    assertEquals("USER_ANSWER", attributes.getFirst().getName());
  }

  @Test
  @DisplayName("mergeEbsAssessmentData skips a row whose instance is not in the assessment")
  void mergeEbsAssessmentDataSkipsUnmatchedInstance() {
    final AssessmentDetail assessment = globalAssessment("CASE-123");
    final List<CaseAssessmentDetail> rows =
        List.of(ebsRow("LINKED_CASES", "OTHER-INSTANCE", "LINKED_CASE_OWNER", "SCA", false));

    assessmentService.mergeEbsAssessmentData(assessment, rows, false);

    // No LINKED_CASES entity in the assessment -> nothing merged, global untouched.
    assertTrue(
        assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes().isEmpty());
  }

  @Test
  @DisplayName("mergeEbsAssessmentData tolerates an entity whose attribute list is immutable")
  void mergeEbsAssessmentDataHandlesImmutableAttributeList() {
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name("global")
                        .entities(
                            List.of(
                                new AssessmentEntityDetail()
                                    .name("CASE-123")
                                    .attributes(List.of())))));
    final List<CaseAssessmentDetail> rows =
        List.of(ebsRow("global", "CASE-123", "EBS_SOURCED", "x", false));

    assessmentService.mergeEbsAssessmentData(assessment, rows, false);

    final List<AssessmentAttributeDetail> attributes =
        assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes();
    assertEquals(1, attributes.size());
    assertEquals("EBS_SOURCED", attributes.getFirst().getName());
  }

  @Test
  @DisplayName(
      "mergeEbsAssessmentData reconciles a single LINKED_CASES instance despite an id mismatch")
  void mergeEbsAssessmentDataReconcilesSingleLinkedCaseByPosition() {
    // caab labels its linked-case instance with the LSC case reference; EBS uses a different id.
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                new ArrayList<>(
                    List.of(
                        new AssessmentEntityTypeDetail()
                            .name("LINKED_CASES")
                            .entities(
                                new ArrayList<>(
                                    List.of(
                                        new AssessmentEntityDetail()
                                            .name("300001513050")
                                            .attributes(new ArrayList<>())))))));
    final List<CaseAssessmentDetail> rows =
        List.of(ebsRow("LINKED_CASES", "60749650", "LINKED_CASE_OWNER", "SCA", false));

    assessmentService.mergeEbsAssessmentData(assessment, rows, false);

    final AssessmentEntityDetail linkedCase =
        assessment.getEntityTypes().getFirst().getEntities().getFirst();
    assertEquals("300001513050", linkedCase.getName());
    assertEquals(1, linkedCase.getAttributes().size());
    assertEquals("LINKED_CASE_OWNER", linkedCase.getAttributes().getFirst().getName());
    assertEquals("SCA", linkedCase.getAttributes().getFirst().getValue());
  }

  @Test
  @DisplayName(
      "startNewAssessment leaves an unchanged existing prepop untouched so it is saved as an "
          + "update (preserving its OPA checkpoint), not re-mapped")
  void startNewAssessmentDoesNotRemapUnchangedExistingPrepop() {
    final String caseRef = "CASE-123";
    final String providerId = String.valueOf(user.getProvider().getId());

    final ApplicationDetail application =
        new ApplicationDetail()
            .id(1)
            .caseReferenceNumber(caseRef)
            .amendment(false)
            .proceedings(new ArrayList<>())
            .opponents(new ArrayList<>());

    // Main assessment does not exist yet -> created fresh.
    when(assessmentApiClient.getAssessments(List.of(MEANS.getName()), providerId, caseRef))
        .thenReturn(just(new AssessmentDetails().content(new ArrayList<>())));

    // Prepop already exists and the application has not changed, so it must NOT be re-mapped
    // (re-mapping a persisted prepop would rebuild its entity graph and break the update).
    final AssessmentDetail existingPrepop =
        new AssessmentDetail()
            .id(99L)
            .name(AssessmentRulebase.MEANS.getPrePopAssessmentName())
            .caseReferenceNumber(caseRef)
            .entityTypes(new ArrayList<>())
            .auditDetail(new AuditDetail().lastSaved(auditDate));
    when(assessmentApiClient.getAssessments(
            List.of(AssessmentRulebase.MEANS.getPrePopAssessmentName()), providerId, caseRef))
        .thenReturn(
            just(new AssessmentDetails().content(new ArrayList<>(List.of(existingPrepop)))));

    when(assessmentApiClient.createAssessment(any(), eq(user.getLoginId())))
        .thenReturn(Mono.empty());
    when(assessmentApiClient.updateAssessment(any(), any(), eq(user.getLoginId())))
        .thenReturn(Mono.empty());

    assessmentService.startNewAssessment(AssessmentRulebase.MEANS, application, null, user, false);

    // Only the working assessment is mapped; the unchanged prepop is left as-is.
    verify(assessmentMapper, never()).toAssessmentDetail(eq(existingPrepop), any());
    verify(assessmentMapper, times(1)).toAssessmentDetail(any(), any());
  }

  @Test
  @DisplayName(
      "startAssessment preserves a COMPLETE assessment on re-entry (data unchanged) instead of "
          + "deleting and rebuilding it")
  void startAssessmentPreservesCompleteAssessmentOnReentry() {
    final String caseRef = "CASE-123";
    final String providerId = String.valueOf(user.getProvider().getId());
    final String prepopName = AssessmentRulebase.MEANS.getPrePopAssessmentName();

    // Application has no proceedings/opponents and nothing has changed -> not stale.
    final ApplicationDetail application =
        new ApplicationDetail()
            .id(1)
            .caseReferenceNumber(caseRef)
            .amendment(false)
            .proceedings(new ArrayList<>())
            .opponents(new ArrayList<>());

    // The working assessment is already COMPLETE.
    final AssessmentDetail completeAssessment =
        new AssessmentDetail()
            .id(50L)
            .name(AssessmentRulebase.MEANS.getName())
            .caseReferenceNumber(caseRef)
            .status("COMPLETE")
            .entityTypes(new ArrayList<>())
            .auditDetail(new AuditDetail().lastSaved(auditDate));
    when(assessmentApiClient.getAssessments(
            List.of(AssessmentRulebase.MEANS.getName()), providerId, caseRef))
        .thenReturn(
            just(new AssessmentDetails().content(new ArrayList<>(List.of(completeAssessment)))));

    // An unchanged prepop exists (drives OPA RESUME).
    final AssessmentDetail existingPrepop =
        new AssessmentDetail()
            .id(99L)
            .name(prepopName)
            .caseReferenceNumber(caseRef)
            .entityTypes(new ArrayList<>())
            .auditDetail(new AuditDetail().lastSaved(auditDate));
    when(assessmentApiClient.getAssessments(List.of(prepopName), providerId, caseRef))
        .thenReturn(
            just(new AssessmentDetails().content(new ArrayList<>(List.of(existingPrepop)))));

    assessmentService.startAssessment(application, AssessmentRulebase.MEANS, null, user, false);

    // The COMPLETE assessment is preserved: nothing is deleted, re-mapped or saved.
    verify(assessmentApiClient, never())
        .deleteAssessments(
            eq(List.of(AssessmentRulebase.MEANS.getName())), any(), any(), any(), any());
    verify(assessmentMapper, never()).toAssessmentDetail(any(), any());
  }

  @Test
  @DisplayName(
      "startNewAssessment deletes and regenerates a stale existing prepop so newly added "
          + "proceedings/opponents reach OPA")
  void startNewAssessmentRegeneratesStalePrepop() {
    final String caseRef = "CASE-123";
    final String providerId = String.valueOf(user.getProvider().getId());
    final String prepopName = AssessmentRulebase.MEANS.getPrePopAssessmentName();

    // Application has a proceeding that the persisted prepop does not -> the prepop is stale.
    final ApplicationDetail application =
        new ApplicationDetail()
            .id(1)
            .caseReferenceNumber(caseRef)
            .amendment(false)
            .proceedings(new ArrayList<>(List.of(new ProceedingDetail().id(123))))
            .opponents(new ArrayList<>());

    when(assessmentApiClient.getAssessments(List.of(MEANS.getName()), providerId, caseRef))
        .thenReturn(just(new AssessmentDetails().content(new ArrayList<>())));

    final AssessmentDetail stalePrepop =
        new AssessmentDetail()
            .id(99L)
            .name(prepopName)
            .caseReferenceNumber(caseRef)
            .entityTypes(new ArrayList<>())
            .auditDetail(new AuditDetail().lastSaved(auditDate));
    // First lookup returns the stale prepop; after it is deleted the second returns none.
    when(assessmentApiClient.getAssessments(List.of(prepopName), providerId, caseRef))
        .thenReturn(just(new AssessmentDetails().content(new ArrayList<>(List.of(stalePrepop)))))
        .thenReturn(just(new AssessmentDetails().content(new ArrayList<>())));

    when(assessmentApiClient.deleteAssessments(
            eq(List.of(prepopName)), eq(providerId), eq(caseRef), any(), eq(user.getLoginId())))
        .thenReturn(Mono.empty());
    when(assessmentApiClient.createAssessment(any(), eq(user.getLoginId())))
        .thenReturn(Mono.empty());

    assessmentService.startNewAssessment(AssessmentRulebase.MEANS, application, null, user, false);

    // The stale prepop is deleted and the regenerated (fresh) prepop is mapped along with the
    // working assessment.
    verify(assessmentApiClient)
        .deleteAssessments(
            eq(List.of(prepopName)), eq(providerId), eq(caseRef), any(), eq(user.getLoginId()));
    verify(assessmentMapper, times(2)).toAssessmentDetail(any(), any());
  }

  @Test
  void testIsAssessmentCheckpointToBeDeleted_dateOfLastChangeAfterLastSaved() {
    final Date lastSaved = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago
    final Date dateOfLastChange = new Date(System.currentTimeMillis() - 5000); // 5 seconds ago

    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setAuditTrail(
        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(dateOfLastChange));
    application.addProceedingsItem(proceeding);

    final OpponentDetail opponent = new OpponentDetail();
    opponent.setAuditTrail(
        new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(dateOfLastChange));
    application.addOpponentsItem(opponent);

    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setAuditDetail(new AuditDetail().lastSaved(lastSaved));

    final boolean result =
        assessmentService.isAssessmentCheckpointToBeDeleted(application, assessment);

    // assert true, since dateOfLastChange is after assessment's last saved date
    assertTrue(result);
  }

  @Test
  void testIsProceedingsCountMismatch_proceedingsCountMismatch() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding1 = new ProceedingDetail().id(123);
    final ProceedingDetail proceeding2 = new ProceedingDetail().id(456);
    application.addProceedingsItem(proceeding1);
    application.addProceedingsItem(proceeding2);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    final AssessmentEntityDetail entityDetail = new AssessmentEntityDetail();
    entityDetail.setName("P_123");
    entityTypeDetail.setEntities(List.of(entityDetail));
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result = assessmentService.isProceedingsCountMismatch(application, assessment);

    // assert true, as the number of proceedings in the application and assessment do not match
    assertTrue(result);
  }

  @Test
  void testIsProceedingsCountMismatch_proceedingsExistInApplication() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding1 = new ProceedingDetail().id(123);
    application.addProceedingsItem(proceeding1);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    final AssessmentEntityDetail entityDetail1 = new AssessmentEntityDetail();
    entityDetail1.setName("P_123");
    final AssessmentEntityDetail entityDetail2 = new AssessmentEntityDetail();
    entityDetail2.setName("P_456");
    entityTypeDetail.setEntities(List.of(entityDetail1, entityDetail2));
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result = assessmentService.isProceedingsCountMismatch(application, assessment);

    // assert true, as the number of proceedings in the application and assessment do not match
    assertTrue(result);
  }

  @Test
  @DisplayName(
      "isProceedingsCountMismatch treats draft amendment proceedings as expected, so an unchanged "
          + "amendment is not flagged as a mismatch (preserving the OPA checkpoint)")
  void testIsProceedingsCountMismatch_amendmentWithDraftProceeding_noMismatch() {
    // Live proceeding P_123 plus a draft amendment proceeding P_456 that only exists in EBS.
    // Distinct proceeding types so the draft is not deduped against the live proceeding.
    final ProceedingDetail liveProceeding =
        new ProceedingDetail().id(123).proceedingType(new StringDisplayValue().id("PROC_A"));
    final ProceedingDetail draftProceeding =
        new ProceedingDetail().ebsId("P_456").proceedingType(new StringDisplayValue().id("PROC_B"));

    final ApplicationDetail application =
        new ApplicationDetail()
            .amendment(true)
            .proceedings(new ArrayList<>(List.of(liveProceeding)))
            .opponents(new ArrayList<>())
            .amendmentProceedingsInEbs(new ArrayList<>(List.of(draftProceeding)));

    // Assessment was built from both proceedings (live + draft).
    final AssessmentEntityDetail liveEntity =
        new AssessmentEntityDetail()
            .name("P_123")
            .addAttributesItem(
                new AssessmentAttributeDetail().name("REQUESTED_SCOPE").value("TEST"));
    final AssessmentEntityDetail draftEntity = new AssessmentEntityDetail().name("P_456");
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .entityTypes(
                List.of(
                    new AssessmentEntityTypeDetail()
                        .name(PROCEEDING.getType())
                        .entities(List.of(liveEntity, draftEntity))));

    final boolean result = assessmentService.isProceedingsCountMismatch(application, assessment);

    // No mismatch - the draft amendment proceeding is part of the expected set.
    assertFalse(result);
  }

  @Test
  void testIsOpaProceedingsMatchApplication_proceedingsDoNotExistInApplication() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail().id(123);
    application.addProceedingsItem(proceeding);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    final AssessmentEntityDetail entityDetail = new AssessmentEntityDetail();
    entityDetail.setName("P_456");
    entityTypeDetail.setEntities(List.of(entityDetail));
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result =
        assessmentService.isAssessmentProceedingsMatchingApplication(application, assessment);

    // assert true as the proceeding does not exist in the application
    assertTrue(result);
  }

  @Test
  void testIsAppProceedingsExistInOpa_proceedingsExistWithNonMatchingScope() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail().id(123);
    proceeding.addScopeLimitationsItem(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("TEST_SCOPE")));
    application.addProceedingsItem(proceeding);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    final AssessmentEntityDetail entityDetail = new AssessmentEntityDetail();
    entityDetail.setName("P_123");
    entityDetail.addAttributesItem(
        new AssessmentAttributeDetail().name("REQUESTED_SCOPE").value("DIFFERENT_SCOPE"));
    entityTypeDetail.setEntities(List.of(entityDetail));
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result =
        assessmentService.isApplicationProceedingsMatchingAssessment(application, assessment);

    // we expect true as the scope is different
    assertTrue(result);
  }

  @Test
  void testIsAppProceedingsExistInOpa_proceedingsDoNotExistInOpa() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail().id(123);
    application.addProceedingsItem(proceeding);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result =
        assessmentService.isApplicationProceedingsMatchingAssessment(application, assessment);

    // we expect true as the proceeding does not exist in OPA
    assertTrue(result);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"2, true", "10, true", "0, true"},
      nullValues = {"null"})
  void isOpaOpponentsMatchApplication(final Integer applicationOpponents, final boolean expected) {

    // An assessment contains 1 opponent, 1 proceeding
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final ApplicationDetail application = new ApplicationDetail();
    application.setOpponents(new ArrayList<>());

    for (int i = 0; i < applicationOpponents; i++) {
      final OpponentDetail opponent = new OpponentDetail();
      opponent.setId(123);
      application.addOpponentsItem(opponent);
    }

    final boolean result =
        assessmentService.isOpponentCountMatchingAssessments(application, assessment);

    assertEquals(result, expected);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "987, null, true",
        "987, OPPONENT_987, true",
        "234, null, false",
        "234, OPPONENT_234, false"
      },
      nullValues = {"null"})
  void isOpaOpponentsMatchApplication(
      final Integer opponentId, final String ebsId, final boolean expected) {

    // An assessment contains 1 opponent, 1 proceeding
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final ApplicationDetail application = new ApplicationDetail();
    final OpponentDetail opponent = new OpponentDetail();
    opponent.setId(opponentId);
    opponent.setEbsId(ebsId);

    application.setOpponents(Collections.singletonList(opponent));

    final boolean result =
        assessmentService.isAssessmentOpponentsMatchingApplication(application, assessment);

    assertEquals(result, expected);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "234, null, false",
        "1, 'OPPONENT_234', false",
        "1, null,  true",
        "1, OPPONENT_1, true"
      },
      nullValues = {"null"})
  void testIsApplicationMatchOpaOpponents(
      final Integer opponentId, final String ebsId, final boolean expected) {
    // An assessment contains 1 opponent, 1 proceeding
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final ApplicationDetail application = new ApplicationDetail();
    final OpponentDetail opponent = new OpponentDetail();
    opponent.setId(opponentId);
    opponent.setEbsId(ebsId);
    application.setOpponents(Collections.singletonList(opponent));

    final boolean result =
        assessmentService.isApplicationOpponentsMatchingAssessments(application, assessment);

    assertEquals(result, expected);
  }

  @Test
  @DisplayName("Test getAssessmentSummaryToDisplay with valid parent and child lookups")
  void testGetAssessmentSummaryToDisplay() {
    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    // Setup mock data for parent summary lookups
    final AssessmentSummaryEntityLookupValueDetail parentSummaryLookup =
        new AssessmentSummaryEntityLookupValueDetail();
    parentSummaryLookup.setName("PROCEEDING");
    parentSummaryLookup.setDisplayName("Proceeding");
    parentSummaryLookup.setEntityLevel(1);
    parentSummaryLookup.addAttributesItem(
        new AssessmentSummaryAttributeLookupValueDetail()
            .name("PROCEEDING_NAME")
            .displayName("Proceeding Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> parentSummaryLookups =
        List.of(parentSummaryLookup);

    // Setup mock data for child summary lookups
    final AssessmentSummaryEntityLookupValueDetail childSummaryLookup =
        new AssessmentSummaryEntityLookupValueDetail();
    childSummaryLookup.setName("CHILD_ENTITY");
    childSummaryLookup.setDisplayName("Child Entity");
    childSummaryLookup.setEntityLevel(2);
    childSummaryLookup.addAttributesItem(
        new AssessmentSummaryAttributeLookupValueDetail()
            .name("CHILD_NAME")
            .displayName("Child Name"));

    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups =
        List.of(childSummaryLookup);

    final List<AssessmentSummaryEntityDisplay> result =
        assessmentService.getAssessmentSummaryToDisplay(
            assessment, parentSummaryLookups, childSummaryLookups);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());

    final AssessmentSummaryEntityDisplay summaryDisplay = result.getFirst();
    assertEquals("PROCEEDING", summaryDisplay.getName());
    assertEquals("Proceeding", summaryDisplay.getDisplayName());
    assertEquals(1, summaryDisplay.getEntityLevel());
    assertNotNull(summaryDisplay.getAttributes());
    assertEquals(1, summaryDisplay.getAttributes().size());
    assertEquals("PROCEEDING_NAME", summaryDisplay.getAttributes().getFirst().getName());
    assertEquals("Proceeding Name", summaryDisplay.getAttributes().getFirst().getDisplayName());
    assertEquals("TEST", summaryDisplay.getAttributes().getFirst().getValue());
  }

  @Test
  void testCreateSummaryEntity() {
    // Initialize assessment detail
    final AssessmentDetail assessment = new AssessmentDetail();

    // Initialize summary entities to display
    final List<AssessmentSummaryEntityDisplay> summaryEntitiesToDisplay = new ArrayList<>();

    // Initialize child summary lookups
    final List<AssessmentSummaryEntityLookupValueDetail> childSummaryLookups = new ArrayList<>();

    // Initialize summary entity lookup
    final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup =
        new AssessmentSummaryEntityLookupValueDetail();
    summaryEntityLookup.setName("testEntity");
    summaryEntityLookup.setDisplayName("Test Entity");
    summaryEntityLookup.setEntityLevel(1);

    // Initialize summary attribute lookup
    final AssessmentSummaryAttributeLookupValueDetail summaryAttributeLookup =
        new AssessmentSummaryAttributeLookupValueDetail();
    summaryAttributeLookup.setName("testAttribute");
    summaryAttributeLookup.setDisplayName("Test Attribute");
    summaryEntityLookup.addAttributesItem(summaryAttributeLookup);

    // Initialize assessment entity detail
    final AssessmentEntityDetail entity = new AssessmentEntityDetail();
    entity.setName("testEntity");

    // Initialize assessment attribute detail
    final AssessmentAttributeDetail assessmentAttribute = new AssessmentAttributeDetail();
    assessmentAttribute.setName("testAttribute");
    assessmentAttribute.setValue("testValue");
    assessmentAttribute.setType("TEXT");
    entity.addAttributesItem(assessmentAttribute);

    // Call the method under test
    assessmentService.createSummaryEntity(
        assessment, summaryEntitiesToDisplay, childSummaryLookups, summaryEntityLookup, entity);

    // Verify the result
    assertFalse(summaryEntitiesToDisplay.isEmpty());
    assertEquals(1, summaryEntitiesToDisplay.size());
    assertEquals("testEntity", summaryEntitiesToDisplay.getFirst().getName());
    assertEquals("Test Entity", summaryEntitiesToDisplay.getFirst().getDisplayName());
    assertEquals(1, summaryEntitiesToDisplay.getFirst().getEntityLevel());

    final List<AssessmentSummaryAttributeDisplay> attributes =
        summaryEntitiesToDisplay.getFirst().getAttributes();
    assertFalse(attributes.isEmpty());
    assertEquals(1, attributes.size());
    assertEquals("testAttribute", attributes.getFirst().getName());
    assertEquals("Test Attribute", attributes.getFirst().getDisplayName());
    assertEquals("testValue", attributes.getFirst().getValue());
  }

  @ParameterizedTest
  @CsvSource({
    "DATE,2023-07-15,15/07/2023",
    "CURRENCY,1234.56,£1234.56",
    "NUMBER,1234.5600,1234.56",
    "BOOLEAN,true,Yes",
    "BOOLEAN,false,No",
    "TEXT,someText,someText"
  })
  void testCreateSummaryAttributeDisplay(
      final String type, final String value, final String expectedFormattedValue) {

    final AssessmentAttributeDetail attribute = new AssessmentAttributeDetail();
    attribute.setName("testAttribute");
    attribute.setType(type);
    attribute.setValue(value);
    attribute.setAsked(true);

    final AssessmentSummaryAttributeLookupValueDetail summaryAttribute =
        new AssessmentSummaryAttributeLookupValueDetail();
    summaryAttribute.setName("testAttribute");
    summaryAttribute.setDisplayName("displayName");

    final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup =
        new AssessmentSummaryEntityLookupValueDetail();
    summaryEntityLookup.setName("testEntity");
    summaryEntityLookup.setDisplayName("entityDisplayName");
    summaryEntityLookup.setEntityLevel(1);
    summaryEntityLookup.addAttributesItem(summaryAttribute);

    final AssessmentSummaryAttributeDisplay result =
        assessmentService.createSummaryAttributeDisplay(attribute, summaryEntityLookup);

    assertNotNull(result);
    assertEquals("testAttribute", result.getName());
    assertEquals("displayName", result.getDisplayName());

    assertEquals(expectedFormattedValue, result.getValue());
  }

  @ParameterizedTest
  @CsvSource({
    // Matching applicationType + matching delegated date → false
    "SUBDP, SUBDP, 15-03-2020, 15-03-2020, false",
    // Matching applicationType + mismatching delegated date → true
    "SUBDP, SUBDP, 15-03-2020, 01-01-2000, true",
    // Matching applicationType + null delegated date in assessment → true
    "SUBDP, SUBDP, 15-03-2020, , true",
    // Matching applicationType + null delegated date in app → true
    "SUBDP, SUBDP, , 15-03-2020, true",
    // Mismatched applicationType → true
    "SUBDP, DP, 15-03-2020, 15-03-2020, true",
    // Non-devolved type (SUB) with no dates → false
    "SUB, SUB, , , false",
    // Devolved type (DP) with missing date in application → true
    "DP, DP, , 15-03-2020, true",
    // Special case for ECF type with no dates → false
    "ECF, SUB, , , false",
    // Special case for ECF type not matching with no dates → dp
    "ECF, DP, , , true",
  })
  @DisplayName("applicationTypeMatches - combined type and delegated date validation")
  void testapplicationTypeMatches_combinedLogic(
      final String applicationTypeCode,
      final String assessmentTypeCode,
      final String applicationDelegatedDate,
      final String assessmentDelegatedDate,
      final boolean expected)
      throws Exception {

    final ApplicationDetail application = new ApplicationDetail();
    final var applicationType = new ApplicationType();
    applicationType.setId(applicationTypeCode);

    if (applicationDelegatedDate != null) {
      final var devolvedPowers = new DevolvedPowersDetail();
      devolvedPowers.setDateUsed(
          new SimpleDateFormat("dd-MM-yyyy").parse(applicationDelegatedDate));
      applicationType.setDevolvedPowers(devolvedPowers);
    }

    application.setApplicationType(applicationType);

    final List<AssessmentAttributeDetail> attributes = new ArrayList<>();

    // Add type attribute
    attributes.add(
        new AssessmentAttributeDetail().name("APP_AMEND_TYPE").value(assessmentTypeCode));

    // Add delegated date attribute if provided
    if (assessmentDelegatedDate != null) {
      attributes.add(
          new AssessmentAttributeDetail()
              .name("DELEGATED_FUNCTIONS_DATE")
              .value(assessmentDelegatedDate));
    }

    final AssessmentEntityDetail globalEntity =
        new AssessmentEntityDetail().name("GLOBAL").attributes(attributes);

    final AssessmentEntityTypeDetail globalType =
        new AssessmentEntityTypeDetail().name("GLOBAL").entities(List.of(globalEntity));

    final AssessmentDetail assessment = new AssessmentDetail().entityTypes(List.of(globalType));

    final boolean result = assessmentService.applicationTypeMatches(application, assessment);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({"SUBDP, true", "SUB, false"})
  @DisplayName("applicationTypeMatches handles legacy placeholder delegated date values")
  void applicationTypeMatchesHandlesPlaceholderDelegatedDate(
      final String applicationTypeCode, final boolean expected) throws Exception {

    final ApplicationDetail application = new ApplicationDetail();
    final var applicationType = new ApplicationType();
    applicationType.setId(applicationTypeCode);

    if ("SUBDP".equals(applicationTypeCode)) {
      final var devolvedPowers = new DevolvedPowersDetail();
      devolvedPowers.setDateUsed(new SimpleDateFormat("dd-MM-yyyy").parse("15-03-2020"));
      applicationType.setDevolvedPowers(devolvedPowers);
    }

    application.setApplicationType(applicationType);

    final List<AssessmentAttributeDetail> attributes =
        new ArrayList<>(
            List.of(
                new AssessmentAttributeDetail().name("APP_AMEND_TYPE").value(applicationTypeCode),
                new AssessmentAttributeDetail().name("DELEGATED_FUNCTIONS_DATE").value("~\t~")));

    final AssessmentEntityDetail globalEntity =
        new AssessmentEntityDetail().name("GLOBAL").attributes(attributes);
    final AssessmentEntityTypeDetail globalType =
        new AssessmentEntityTypeDetail().name("GLOBAL").entities(List.of(globalEntity));
    final AssessmentDetail assessment = new AssessmentDetail().entityTypes(List.of(globalType));

    final boolean result = assessmentService.applicationTypeMatches(application, assessment);

    assertEquals(expected, result);
  }

  private AssessmentResult buildEbsAssessmentResult(
      final String entityName,
      final String instanceLabel,
      final String attributeName,
      final String attributeValue) {
    return new AssessmentResult()
        .assessmentDetails(
            List.of(
                new AssessmentScreen()
                    .entity(
                        List.of(
                            new OpaEntity()
                                .entityName(entityName)
                                .instances(
                                    List.of(
                                        new OpaInstance()
                                            .instanceLabel(instanceLabel)
                                            .attributes(
                                                List.of(
                                                    new OpaAttribute()
                                                        .attribute(attributeName)
                                                        .responseType("text")
                                                        .responseValue(attributeValue)))))))));
  }

  private static AssessmentDetail buildAssessmentWithAttributes(
      final String entityTypeName, final String entityName, final String... attributeNames) {
    final AssessmentEntityDetail entity = new AssessmentEntityDetail().name(entityName);
    for (final String attributeName : attributeNames) {
      entity.addAttributesItem(new AssessmentAttributeDetail().name(attributeName).value("true"));
    }

    return new AssessmentDetail()
        .addEntityTypesItem(
            new AssessmentEntityTypeDetail().name(entityTypeName).addEntitiesItem(entity));
  }

  private static List<String> attributeNames(
      final AssessmentDetail assessment, final int entityTypeIndex) {
    return assessment
        .getEntityTypes()
        .get(entityTypeIndex)
        .getEntities()
        .getFirst()
        .getAttributes()
        .stream()
        .map(AssessmentAttributeDetail::getName)
        .toList();
  }

  @Test
  @DisplayName("removeNonReusableAttributes handles an immutable attribute list")
  void removeNonReusableAttributes_immutableAttributeList() {
    // The mapper can produce immutable attribute lists, so removal must not mutate in place.
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .addEntityTypesItem(
                new AssessmentEntityTypeDetail()
                    .name(GLOBAL.getType())
                    .addEntitiesItem(
                        new AssessmentEntityDetail()
                            .name(GLOBAL.getType())
                            .attributes(
                                List.of(
                                    new AssessmentAttributeDetail()
                                        .name("MERITS_EVIDENCE_REQD")
                                        .value("true"),
                                    new AssessmentAttributeDetail()
                                        .name("APPLICATION_CASE_REF")
                                        .value("300001")))));

    assessmentService.removeNonReusableAttributes(assessment, AssessmentRulebase.MERITS);

    assertEquals(List.of("APPLICATION_CASE_REF"), attributeNames(assessment, 0));
  }

  @Test
  @DisplayName("removeNonReusableAttributes strips the merits do-not-reuse attributes only")
  void removeNonReusableAttributes_meritsStripsEvidenceKeepsRest() {
    final AssessmentDetail assessment =
        buildAssessmentWithAttributes(
            GLOBAL.getType(),
            GLOBAL.getType(),
            "MERITS_EVIDENCE_REQD",
            "ADDITIONAL_EVIDENCE_COLLECTED",
            "APPLICATION_CASE_REF");

    assessmentService.removeNonReusableAttributes(assessment, AssessmentRulebase.MERITS);

    // The evidence answers must be re-asked on an amendment; the case reference is reusable.
    assertEquals(List.of("APPLICATION_CASE_REF"), attributeNames(assessment, 0));
  }

  @Test
  @DisplayName("removeNonReusableAttributes strips across every entity type")
  void removeNonReusableAttributes_stripsAllEntityTypes() {
    final AssessmentDetail assessment =
        buildAssessmentWithAttributes(GLOBAL.getType(), GLOBAL.getType(), "MEANS_EVIDENCE_REQD");
    assessment.addEntityTypesItem(
        new AssessmentEntityTypeDetail()
            .name(PROCEEDING.getType())
            .addEntitiesItem(
                new AssessmentEntityDetail()
                    .name("P_1")
                    .addAttributesItem(
                        new AssessmentAttributeDetail().name("BANKACC_SMOD_FLAG").value("true"))
                    .addAttributesItem(
                        new AssessmentAttributeDetail().name("MATTER_TYPE").value("TEST"))));

    assessmentService.removeNonReusableAttributes(assessment, AssessmentRulebase.MEANS);

    assertTrue(attributeNames(assessment, 0).isEmpty());
    assertEquals(List.of("MATTER_TYPE"), attributeNames(assessment, 1));
  }

  @Test
  @DisplayName("removeNonReusableAttributes does not strip the other rulebase's attributes")
  void removeNonReusableAttributes_doesNotStripAcrossRulebases() {
    final AssessmentDetail assessment =
        buildAssessmentWithAttributes(GLOBAL.getType(), GLOBAL.getType(), "MERITS_EVIDENCE_REQD");

    assessmentService.removeNonReusableAttributes(assessment, AssessmentRulebase.MEANS);

    assertEquals(List.of("MERITS_EVIDENCE_REQD"), attributeNames(assessment, 0));
  }

  @Test
  @DisplayName("removeNonReusableAttributes tolerates missing data")
  void removeNonReusableAttributes_toleratesMissingData() {
    assessmentService.removeNonReusableAttributes(null, AssessmentRulebase.MERITS);
    assessmentService.removeNonReusableAttributes(
        new AssessmentDetail(), AssessmentRulebase.MERITS);
  }
}
