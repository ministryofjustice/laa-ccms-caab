package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROGRESS_STATUS_TYPES;
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
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryAttributeDisplay;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.data.model.AssessmentSummaryAttributeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;


@ExtendWith(MockitoExtension.class)
public class AssessmentServiceTest {

  @Mock
  private AssessmentApiClient assessmentApiClient;

  @Mock
  private LookupService lookupService;

  @InjectMocks
  private AssessmentService assessmentService;

  private static final String PROGRESS_STATUS_CODE = "TEST";
  private static final String PROGRESS_STATUS_DESC = "Test";
  private static final Long ASSESSMENT_ID = 1234567L;

  private final Date auditDate = new Date(System.currentTimeMillis());

  @Test
  public void testSaveAssessment_createAssessment() {
    when(assessmentApiClient.createAssessment(
        any(AssessmentDetail.class), anyString())).thenReturn(Mono.empty());

    final UserDetail user = new UserDetail();
    user.setLoginId("testUser");

    final AssessmentDetail assessmentWithoutId = new AssessmentDetail();

    assessmentService.saveAssessment(user, assessmentWithoutId).block();
    Mockito.verify(assessmentApiClient).createAssessment(
        assessmentWithoutId, user.getLoginId());
  }

  @Test
  public void testSaveAssessment_updateAssessment() {
    when(assessmentApiClient.updateAssessment(
        any(), any(AssessmentDetail.class), anyString())).thenReturn(Mono.empty());

    final UserDetail user = new UserDetail();
    user.setLoginId("testUser");

    final AssessmentDetail assessmentWithId = new AssessmentDetail();
    assessmentWithId.setId(123L);

    assessmentService.saveAssessment(user, assessmentWithId).block();
    Mockito.verify(assessmentApiClient).updateAssessment(
        assessmentWithId.getId(), assessmentWithId, user.getLoginId());
  }

  @Test
  void getAssessments_ReturnsAssessmentDetails_Success() {
    final String assessmentName = "meansAssessment";
    final String providerId = "1";
    final String caseReferenceNumber = "12345";

    when(assessmentApiClient.getAssessments(any(), anyString(), anyString()))
        .thenReturn(just(new AssessmentDetails()));

    final Mono<AssessmentDetails>
        result = assessmentService.getAssessments(List.of(assessmentName), providerId, caseReferenceNumber);

    StepVerifier.create(result)
        .expectNextMatches(Objects::nonNull)
        .verifyComplete();
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
            user,
            List.of(assessmentName),
            caseReferenceNumber,
            status);

    StepVerifier.create(result)
        .expectComplete()
        .verify();
  }

  @Test
  void testCalculateAssessmentStatuses_assessmentsNotStarted() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final UserDetail user = buildUserDetail();

    assessmentService.calculateAssessmentStatuses(
        application,
        null,
        null,
        user);

    assertNull(application.getMeansAssessmentStatus());
    assertNull(application.getMeritsAssessmentStatus());
  }

  @Test
  void testCalculateAssessmentStatuses_startedMeansAssessment() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final AssessmentDetail meansAssessment = new AssessmentDetail()
        .name(MEANS.getName())
        .status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(
        application,
        meansAssessment,
        null,
        user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());
  }

  @ParameterizedTest
  @CsvSource({
      "COMPLETE",
      "ERROR"
  })
  void testCalculateAssessmentStatuses_meansAssessment_reassessmentRequired(
      final String assessmentStatus) {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final AssessmentDetail meansAssessment = new AssessmentDetail()
        .id(ASSESSMENT_ID)
        .name(MEANS.getName())
        .status(assessmentStatus);

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    when(assessmentApiClient.patchAssessment(
        eq(ASSESSMENT_ID),
        eq(user.getLoginId()),
        any())).thenReturn(Mono.empty());

    assessmentService.calculateAssessmentStatuses(
        application,
        meansAssessment,
        null,
        user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());

    verify(assessmentApiClient).patchAssessment(
        eq(ASSESSMENT_ID),
        eq(user.getLoginId()),
        any());
  }

  @ParameterizedTest
  @CsvSource({
      "COMPLETE",
      "ERROR"
  })
  void testCalculateAssessmentStatuses_meritsAssessment_reassessmentRequired(
      final String assessmentStatus) {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final AssessmentDetail meritsAssessment = new AssessmentDetail()
        .id(ASSESSMENT_ID)
        .name(MERITS.getName())
        .status(assessmentStatus);

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    when(assessmentApiClient.patchAssessment(
        eq(ASSESSMENT_ID),
        eq(user.getLoginId()),
        any())).thenReturn(Mono.empty());

    assessmentService.calculateAssessmentStatuses(
        application,
        null,
        meritsAssessment,
        user);

    assertNull(application.getMeansAssessment());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);

    verify(assessmentApiClient).patchAssessment(
        eq(ASSESSMENT_ID),
        eq(user.getLoginId()),
        any());
  }

  @Test
  void testCalculateAssessmentStatuses_meansAssessment_amendment_assessmentAmended() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(true)
        .meansAssessmentAmended(true);

    final AssessmentDetail meansAssessment = new AssessmentDetail()
        .id(ASSESSMENT_ID)
        .name(MEANS.getName())
        .status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(
        application,
        meansAssessment,
        null,
        user);

    assertEquals(application.getMeansAssessmentStatus(), PROGRESS_STATUS_DESC);
    assertNull(application.getMeritsAssessmentStatus());
  }

  @Test
  void testCalculateAssessmentStatuses_meritsAssessment_amendment_assessmentAmended() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(true)
        .meritsAssessmentAmended(true);

    final AssessmentDetail meritsAssessment = new AssessmentDetail()
        .id(ASSESSMENT_ID)
        .name(MERITS.getName())
        .status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(
        application,
        null,
        meritsAssessment,
        user);

    assertNull(application.getMeansAssessment());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_entityTypeNull_assertsTrue() {
    final ApplicationDetail application = new ApplicationDetail();

    final boolean result = assessmentService.checkAssessmentForProceedingKeyChange(
        application, null);

    assertTrue(result);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application = new ApplicationDetail()
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetail();

    final boolean result = assessmentService.checkAssessmentForProceedingKeyChange(
        application,
        proceedingsEntityTypeDetail);

    assertFalse(result);
  }

  @Test
  void testCheckAssessmentForProceedingKeyChange_multipleScopeLimitations_assertsFalse() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application = new ApplicationDetail()
        .addProceedingsItem(new ProceedingDetail()
            .id(789)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation)))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetailWithMultipleScopes();

    final boolean result = assessmentService.checkAssessmentForProceedingKeyChange(
        application,
        proceedingsEntityTypeDetail);

    assertFalse(result);
  }

  @ParameterizedTest
  @CsvSource({
      //matter type difference
      "123, OTHER, TEST, TEST, TEST",

      //proceeding type difference
      "123, TEST, OTHER, TEST, TEST",

      //client involvement difference
      "123, TEST, TEST, OTHER, TEST",

      //scope limitation difference
      "123, TEST, TEST, TEST, OTHER",

      //cant find proceeding with matching id
      "789, TEST, TEST, TEST, TEST"
  })
  void testCheckAssessmentForProceedingKeyChange_assertsTrue(
      final Integer proceedingId,
      final String matterType,
      final String proceedingType,
      final String clientInvolvement,
      final String scopeLimitation) {
    final ApplicationDetail application = new ApplicationDetail()
        .addProceedingsItem(new ProceedingDetail()
            .id(proceedingId)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))));

    final AssessmentEntityTypeDetail proceedingsEntityTypeDetail =
        buildProceedingsEntityTypeDetail();

    final boolean result = assessmentService.checkAssessmentForProceedingKeyChange(
        application,
        proceedingsEntityTypeDetail);

    assertTrue(result);
  }


  @Test
  void testCalculateAssessmentStatuses_startedMeritsAssessment() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final AssessmentDetail meritsAssessment = new AssessmentDetail()
        .name(MERITS.getName())
        .status("INCOMPLETE");

    final UserDetail user = buildUserDetail();

    final CommonLookupValueDetail progressStatusTypes =
        new CommonLookupValueDetail()
            .code(PROGRESS_STATUS_CODE)
            .description(PROGRESS_STATUS_DESC);

    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROGRESS_STATUS_TYPES), any())).thenReturn(
        Mono.just(Optional.of(progressStatusTypes)));

    assessmentService.calculateAssessmentStatuses(
        application,
        null,
        meritsAssessment,
        user);

    assertNull(application.getMeansAssessmentStatus());
    assertEquals(application.getMeritsAssessmentStatus(), PROGRESS_STATUS_DESC);
  }

  //todo - test to be amended when amendment scenarios are added
  @Test
  void testIsReassessmentRequired_isAmendment() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(true);

    final AssessmentDetail assessment = new AssessmentDetail();
    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertFalse(result);
  }


  @Test
  void testIsReassessmentRequired_assessmentNull_assertsFalse() {
    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false);

    final boolean result = assessmentService.isReassessmentRequired(application, null);

    assertFalse(result);
  }

  @Test
  void testIsReassessmentRequired_assessmentHasMoreProceedingsThanApplication_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false)
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
        .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetailMultipleProceedings();

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_meritsReassessmentRequired_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application = new ApplicationDetail()
        .meritsReassessmentRequired(true)
        .amendment(false)
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
        .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_opponentsHaveBeenUpdated_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    //lased saved date = now - 11 seconds
    final long currentTime = System.currentTimeMillis();
    final Date lastSaved = new Date(currentTime - 11000);
    final Date currentDate = new Date(currentTime);

    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false)
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))))
        .addOpponentsItem(new OpponentDetail()
            .id(234)
            .type("Individual")
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(currentDate)))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
        .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(lastSaved);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertTrue(result);
  }

  @Test
  void testIsReassessmentRequired_assessmentHasMoreOpponentsThanApplication_assertsTrue() {
    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";


    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false)
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))))
        .addOpponentsItem(new OpponentDetail()
            .id(234)
            .type("Individual")
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(1000.00)))
        .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetailMultipleOpponents(auditDate);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertTrue(result);
  }

  @ParameterizedTest
  @CsvSource({
      "meritsAssessment, true",
      "meansAssessment, false"
  })
  void testIsReassessmentRequired_costLimitDifference(
      final String assessmentName,
      final boolean expectedResult) {

    final String matterType = "TEST";
    final String proceedingType = "TEST";
    final String clientInvolvement = "TEST";
    final String scopeLimitation = "TEST";

    final ApplicationDetail application = new ApplicationDetail()
        .amendment(false)
        .addProceedingsItem(new ProceedingDetail()
            .id(123)
            .matterType(new StringDisplayValue().id(matterType))
            .proceedingType(new StringDisplayValue().id(proceedingType))
            .clientInvolvement(new StringDisplayValue().id(clientInvolvement))
            .addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
                new StringDisplayValue().id(scopeLimitation))))
        .addOpponentsItem(new OpponentDetail()
            .id(234)
            .type("Individual")
            .auditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail().lastSaved(auditDate)))
        .costLimit(new CostLimitDetail().limitAtTimeOfMerits(BigDecimal.valueOf(999.00)))
        .costs(new CostStructureDetail().requestedCostLimitation(BigDecimal.valueOf(1000.00)));

    final AssessmentDetail assessment = buildAssessmentDetail(auditDate);
    assessment.setName(assessmentName);

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @CsvSource({
      "proceeding1,,opponent1,,0,0",
      "P_123,,OPPONENT_234,,1,1",
      ",123,,234,1,1"
  })
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
  void testGetAssessmentOpponentMappingContexts() {
    final ApplicationDetail application = new ApplicationDetail();
    final OpponentDetail opponent1 = new OpponentDetail().title("MR");
    final OpponentDetail opponent2 = new OpponentDetail().title("MS");
    application.addOpponentsItem(opponent1);
    application.addOpponentsItem(opponent2);

    final CommonLookupValueDetail titleLookupMr = new CommonLookupValueDetail().code("MR").description("Mr");
    final CommonLookupValueDetail titleLookupMs = new CommonLookupValueDetail().code("MS").description("Ms");

    when(lookupService.getCommonValue(eq(COMMON_VALUE_CONTACT_TITLE), eq("MR")))
        .thenReturn(Mono.just(Optional.of(titleLookupMr)));
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CONTACT_TITLE), eq("MS")))
        .thenReturn(Mono.just(Optional.of(titleLookupMs)));

    final List<AssessmentOpponentMappingContext> result = assessmentService.getAssessmentOpponentMappingContexts(application);

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

    final AssessmentDetail existingAssessment = new AssessmentDetail()
        .caseReferenceNumber(referenceId)
        .providerId(providerId)
        .name(assessmentName)
        .status(INCOMPLETE.getStatus());
    final AssessmentDetails assessmentDetails = new AssessmentDetails();
    assessmentDetails.setContent(List.of(existingAssessment));

    when(assessmentService.getAssessments(
        eq(List.of(assessmentName)),
        eq(providerId),
        eq(referenceId))).thenReturn(Mono.just(assessmentDetails));

    final AssessmentDetail result = assessmentService.findOrCreate(providerId, referenceId, assessmentName);

    assertNotNull(result);
    assertEquals(existingAssessment, result);
  }

  @Test
  void testFindOrCreate_newAssessment() {
    final String providerId = "providerId";
    final String referenceId = "referenceId";
    final String assessmentName = "assessmentName";

    when(assessmentService.getAssessments(
        eq(List.of(assessmentName)),
        eq(providerId),
        eq(referenceId))).thenReturn(Mono.just(new AssessmentDetails()));

    final AssessmentDetail result = assessmentService.findOrCreate(providerId, referenceId, assessmentName);

    assertNotNull(result);
    assertEquals(referenceId, result.getCaseReferenceNumber());
    assertEquals(providerId, result.getProviderId());
    assertEquals(assessmentName, result.getName());
    assertEquals(INCOMPLETE.getStatus(), result.getStatus());
  }


  @Test
  void testIsAssessmentCheckpointToBeDeleted_dateOfLastChangeAfterLastSaved() {
    final Date lastSaved = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago
    final Date dateOfLastChange = new Date(System.currentTimeMillis() - 5000); // 5 seconds ago

    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setAuditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail()
        .lastSaved(dateOfLastChange));
    application.addProceedingsItem(proceeding);

    final OpponentDetail opponent = new OpponentDetail();
    opponent.setAuditTrail(new uk.gov.laa.ccms.caab.model.AuditDetail()
        .lastSaved(dateOfLastChange));
    application.addOpponentsItem(opponent);

    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setAuditDetail(new AuditDetail().lastSaved(lastSaved));

    final boolean result = assessmentService.isAssessmentCheckpointToBeDeleted(application, assessment);

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

    //assert true, as the number of proceedings in the application and assessment do not match
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

    final boolean result = assessmentService.isAssessmentProceedingsMatchingApplication(application, assessment);

    // assert true as the proceeding does not exist in the application
    assertTrue(result);
  }

  @Test
  void testIsAppProceedingsExistInOpa_proceedingsExistWithNonMatchingScope() {
    final ApplicationDetail application = new ApplicationDetail();
    final ProceedingDetail proceeding = new ProceedingDetail().id(123);
    proceeding.addScopeLimitationsItem(new ScopeLimitationDetail().scopeLimitation(
        new StringDisplayValue().id("TEST_SCOPE")));
    application.addProceedingsItem(proceeding);

    final AssessmentDetail assessment = buildAssessmentDetail(new Date());

    final AssessmentEntityTypeDetail entityTypeDetail = new AssessmentEntityTypeDetail();
    entityTypeDetail.setName("PROCEEDING");
    final AssessmentEntityDetail entityDetail = new AssessmentEntityDetail();
    entityDetail.setName("P_123");
    entityDetail.addAttributesItem(new AssessmentAttributeDetail()
        .name("REQUESTED_SCOPE").value("DIFFERENT_SCOPE"));
    entityTypeDetail.setEntities(List.of(entityDetail));
    assessment.setEntityTypes(List.of(entityTypeDetail));

    final boolean result = assessmentService.isApplicationProceedingsMatchingAssessment(application, assessment);

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

    final boolean result = assessmentService.isApplicationProceedingsMatchingAssessment(application, assessment);

    //we expect true as the proceeding does not exist in OPA
    assertTrue(result);
  }


  @ParameterizedTest
  @CsvSource(value ={
      "2, true",
      "10, true",
      "0, true"
  }, nullValues = {"null"})
  void isOpaOpponentsMatchApplication(
      final Integer applicationOpponents,
      final boolean expected) {

    //An assessment contains 1 opponent, 1 proceeding
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
  @CsvSource(value ={
      "987, null, true",
      "987, OPPONENT_987, true",
      "234, null, false",
      "234, OPPONENT_234, false"
  }, nullValues = {"null"})
  void isOpaOpponentsMatchApplication(
      final Integer opponentId,
      final String ebsId,
      final boolean expected) {

    //An assessment contains 1 opponent, 1 proceeding
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
  @CsvSource(value ={
      "234, null, false",
      "1, 'OPPONENT_234', false",
      "1, null,  true",
      "1, OPPONENT_1, true"
  }, nullValues = {"null"})
  void testIsApplicationMatchOpaOpponents(
      final Integer opponentId,
      final String ebsId,
      final boolean expected) {
    //An assessment contains 1 opponent, 1 proceeding
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

    final List<AssessmentSummaryEntityDisplay> result =
        assessmentService.getAssessmentSummaryToDisplay(assessment, parentSummaryLookups, childSummaryLookups);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());

    final AssessmentSummaryEntityDisplay summaryDisplay = result.get(0);
    assertEquals("PROCEEDING", summaryDisplay.getName());
    assertEquals("Proceeding", summaryDisplay.getDisplayName());
    assertEquals(1, summaryDisplay.getEntityLevel());
    assertNotNull(summaryDisplay.getAttributes());
    assertEquals(1, summaryDisplay.getAttributes().size());
    assertEquals("PROCEEDING_NAME", summaryDisplay.getAttributes().get(0).getName());
    assertEquals("Proceeding Name", summaryDisplay.getAttributes().get(0).getDisplayName());
    assertEquals("TEST", summaryDisplay.getAttributes().get(0).getValue());
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
    final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup = new AssessmentSummaryEntityLookupValueDetail();
    summaryEntityLookup.setName("testEntity");
    summaryEntityLookup.setDisplayName("Test Entity");
    summaryEntityLookup.setEntityLevel(1);

    // Initialize summary attribute lookup
    final AssessmentSummaryAttributeLookupValueDetail summaryAttributeLookup = new AssessmentSummaryAttributeLookupValueDetail();
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
    assessmentService.createSummaryEntity(assessment, summaryEntitiesToDisplay, childSummaryLookups, summaryEntityLookup, entity);

    // Verify the result
    assertFalse(summaryEntitiesToDisplay.isEmpty());
    assertEquals(1, summaryEntitiesToDisplay.size());
    assertEquals("testEntity", summaryEntitiesToDisplay.get(0).getName());
    assertEquals("Test Entity", summaryEntitiesToDisplay.get(0).getDisplayName());
    assertEquals(1, summaryEntitiesToDisplay.get(0).getEntityLevel());

    final List<AssessmentSummaryAttributeDisplay> attributes = summaryEntitiesToDisplay.get(0).getAttributes();
    assertFalse(attributes.isEmpty());
    assertEquals(1, attributes.size());
    assertEquals("testAttribute", attributes.get(0).getName());
    assertEquals("Test Attribute", attributes.get(0).getDisplayName());
    assertEquals("testValue", attributes.get(0).getValue());
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
      final String type,
      final String value,
      final String expectedFormattedValue) {

    final AssessmentAttributeDetail attribute = new AssessmentAttributeDetail();
    attribute.setName("testAttribute");
    attribute.setType(type);
    attribute.setValue(value);
    attribute.setAsked(true);

    final AssessmentSummaryAttributeLookupValueDetail summaryAttribute = new AssessmentSummaryAttributeLookupValueDetail();
    summaryAttribute.setName("testAttribute");
    summaryAttribute.setDisplayName("displayName");

    final AssessmentSummaryEntityLookupValueDetail summaryEntityLookup = new AssessmentSummaryEntityLookupValueDetail();
    summaryEntityLookup.setName("testEntity");
    summaryEntityLookup.setDisplayName("entityDisplayName");
    summaryEntityLookup.setEntityLevel(1);
    summaryEntityLookup.addAttributesItem(summaryAttribute);

    final AssessmentSummaryAttributeDisplay
        result = assessmentService.createSummaryAttributeDisplay(attribute, summaryEntityLookup);

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
      "DP, DP, , 15-03-2020, true"
  })
  @DisplayName("applicationTypeMatches - combined type and delegated date validation")
  void testapplicationTypeMatches_combinedLogic(
      final String applicationTypeCode,
      final String assessmentTypeCode,
      final String applicationDelegatedDate,
      final String assessmentDelegatedDate,
      final boolean expected) throws Exception {

    final ApplicationDetail application = new ApplicationDetail();
    final var applicationType = new uk.gov.laa.ccms.caab.model.ApplicationType();
    applicationType.setId(applicationTypeCode);

    if (applicationDelegatedDate != null) {
      final var devolvedPowers = new uk.gov.laa.ccms.caab.model.DevolvedPowersDetail();
      devolvedPowers.setDateUsed(new SimpleDateFormat("dd-MM-yyyy").parse(applicationDelegatedDate));
      applicationType.setDevolvedPowers(devolvedPowers);
    }

    application.setApplicationType(applicationType);

    final List<AssessmentAttributeDetail> attributes = new ArrayList<>();

    // Add type attribute
    attributes.add(new AssessmentAttributeDetail()
        .name("APP_AMEND_TYPE")
        .value(assessmentTypeCode));

    // Add delegated date attribute if provided
    if (assessmentDelegatedDate != null) {
      attributes.add(new AssessmentAttributeDetail()
          .name("DELEGATED_FUNCTIONS_DATE")
          .value(assessmentDelegatedDate));
    }

    final AssessmentEntityDetail globalEntity = new AssessmentEntityDetail()
        .name("GLOBAL")
        .attributes(attributes);

    final AssessmentEntityTypeDetail globalType = new AssessmentEntityTypeDetail()
        .name("GLOBAL")
        .entities(List.of(globalEntity));

    final AssessmentDetail assessment = new AssessmentDetail()
        .entityTypes(List.of(globalType));

    final boolean result = assessmentService.applicationTypeMatches(application, assessment);

    assertEquals(expected, result);
  }




}