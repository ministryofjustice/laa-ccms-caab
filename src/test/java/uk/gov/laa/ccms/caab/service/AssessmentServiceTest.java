package uk.gov.laa.ccms.caab.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROGRESS_STATUS_TYPES;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetail;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetailMultipleOpponents;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildAssessmentDetailMultipleProceedings;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildProceedingsEntityTypeDetail;
import static uk.gov.laa.ccms.caab.util.AssessmentModelUtils.buildProceedingsEntityTypeDetailWithMultipleScopes;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;
import uk.gov.laa.ccms.caab.client.AssessmentApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
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
  private static final String ASSESSMENT_ID = "1234567";

  private final Date auditDate = new Date(System.currentTimeMillis());

  @Test
  void getAssessments_ReturnsAssessmentDetails_Success() {
    final String assessmentName = "meansAssessment";
    final String providerId = "1";
    final String caseReferenceNumber = "12345";
    final String status = "COMPLETE";

    when(assessmentApiClient.getAssessments(any(), anyString(), anyString(), anyString()))
        .thenReturn(just(new AssessmentDetails()));

    final Mono<AssessmentDetails>
        result = assessmentService.getAssessments(List.of(assessmentName), providerId, caseReferenceNumber, status);

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
  void getMostRecentAssessmentDetail_ReturnsNull_IfListIsNull() {
    assertNull(assessmentService.getMostRecentAssessmentDetail(null));
  }

  @Test
  void getMostRecentAssessmentDetail_ReturnsNull_IfListIsEmpty() {
    assertNull(assessmentService.getMostRecentAssessmentDetail(Collections.emptyList()));
  }

  @Test
  void getMostRecentAssessmentDetail_ReturnsMostRecent_IfListHasMultipleElements() {
    final AssessmentDetail oldest = new AssessmentDetail();
    oldest.setAuditDetail(new AuditDetail());
    oldest.getAuditDetail().setLastSaved(new Date(1000)); // oldest date

    final AssessmentDetail newest = new AssessmentDetail();
    newest.setAuditDetail(new AuditDetail());
    newest.getAuditDetail().setLastSaved(new Date(3000)); // newest date

    final AssessmentDetail middle = new AssessmentDetail();
    middle.setAuditDetail(new AuditDetail());
    middle.getAuditDetail().setLastSaved(new Date(2000)); // middle date

    final List<AssessmentDetail> assessments = Arrays.asList(oldest, newest, middle);

    final AssessmentDetail result = assessmentService.getMostRecentAssessmentDetail(assessments);

    assertEquals(newest, result);
  }

  @Test
  void getMostRecentAssessmentDetail_HandlesNullDates_Correctly() {
    final AssessmentDetail withDate = new AssessmentDetail();
    withDate.setAuditDetail(new AuditDetail());
    withDate.getAuditDetail().setLastSaved(new Date(2000)); // valid date

    final AssessmentDetail withoutDate = new AssessmentDetail();
    withoutDate.setAuditDetail(new AuditDetail()); // null date

    final List<AssessmentDetail> assessments = Arrays.asList(withDate, withoutDate);

    final AssessmentDetail result = assessmentService.getMostRecentAssessmentDetail(assessments);

    assertEquals(withDate, result);
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

    when(assessmentApiClient.updateAssessment(
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

    verify(assessmentApiClient).updateAssessment(
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

    when(assessmentApiClient.updateAssessment(
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

    verify(assessmentApiClient).updateAssessment(
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

  @Test
  void testIsReassessmentRequired_costLimitDifference_assertsTrue() {
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

    final boolean result = assessmentService.isReassessmentRequired(application, assessment);

    assertTrue(result);
  }

}