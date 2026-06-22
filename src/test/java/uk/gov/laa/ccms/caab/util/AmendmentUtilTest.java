package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

@DisplayName("Amendment util test")
class AmendmentUtilTest {

  @Test
  @DisplayName("Should empty proceedings and opponents")
  void shouldEmptyProceedingsAndOpponents() {
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getProceedings()).isEmpty();
    assertThat(amendmentDetail.getOpponents()).isEmpty();
    assertThat(amendmentDetail.getLarScopeFlag()).isNull();
    // Fields usually cleaned should not be cleaned
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNotNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNotNull();
    assertThat(amendmentDetail.getCosts()).isNotNull();
    assertThat(amendmentDetail.getProviderDetails().getSupervisor()).isNotNull();
    assertThat(amendmentDetail.getProviderDetails().getFeeEarner()).isNotNull();
    assertThat(amendmentDetail.getProviderDetails().getProviderContact()).isNotNull();
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNotNull();
    assertThat(amendmentDetail.getMeansAssessmentAmended()).isFalse();
    assertThat(amendmentDetail.getMeritsAssessmentAmended()).isFalse();
  }

  @Test
  @DisplayName("Should clean when edit type is edit provider")
  void shouldCleanWhenEditTypeEditProvider() {
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_EDIT_PROVIDER);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNotNull();
    assertThat(amendmentDetail.getCosts()).isNull();
  }

  @Test
  @DisplayName("Should clean when edit type is correspondence")
  void shouldCleanWhenEditTypeCorrespondence() {
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(
        QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getProviderDetails().getSupervisor()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getFeeEarner()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getProviderContact()).isNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNotNull();
    assertThat(amendmentDetail.getCosts()).isNull();
  }

  @Test
  @DisplayName("Should clean when edit type is allocate cost limit")
  void shouldCleanWhenEditTypeAllocateCostLimit() {
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_ALLOCATE_COST_LIMIT);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getProviderDetails().getSupervisor()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getFeeEarner()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getProviderContact()).isNull();
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNull();
  }

  @Test
  @DisplayName("Should clean when edit type is means reassessment")
  void shouldCleanWhenEditTypeMeansReassessment() {
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getMeansAssessmentAmended()).isTrue();
    assertThat(amendmentDetail.getMeritsAssessmentAmended()).isFalse();
    assertThat(amendmentDetail.getProviderDetails().getSupervisor()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getFeeEarner()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getProviderContact()).isNull();
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNull();
    assertThat(amendmentDetail.getCosts()).isNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNotNull();
  }

  private static ApplicationDetail pristineAmendment() {
    final Date created = new Date(1_000_000_000_000L);
    return new ApplicationDetail()
        .amendment(true)
        .costLimit(new CostLimitDetail().changed(false))
        .proceedings(List.of(new ProceedingDetail().status(unchanged())))
        .opponents(List.of(new OpponentDetail().auditTrail(new AuditDetail().created(created))))
        .priorAuthorities(List.of())
        .linkedCases(List.of())
        .auditTrail(new AuditDetail().created(created).lastSaved(created));
  }

  private static StringDisplayValue unchanged() {
    return new StringDisplayValue().displayValue("Unchanged");
  }

  @Test
  @DisplayName("hasChanges - null amendment is not a change")
  void hasChangesNullAmendment() {
    assertThat(AmendmentUtil.hasChanges(null, new ApplicationDetail())).isFalse();
  }

  @Test
  @DisplayName("hasChanges - pristine amendment has no changes")
  void hasChangesPristine() {
    assertThat(AmendmentUtil.hasChanges(pristineAmendment(), pristineAmendment())).isFalse();
  }

  @Test
  @DisplayName("hasChanges - changed cost limit is a change")
  void hasChangesCostLimit() {
    final ApplicationDetail amendment = pristineAmendment();
    amendment.getCostLimit().setChanged(true);
    assertThat(AmendmentUtil.hasChanges(amendment, pristineAmendment())).isTrue();
  }

  @Test
  @DisplayName("hasChanges - amended means or merits is a change")
  void hasChangesAssessmentAmended() {
    final ApplicationDetail means = pristineAmendment().meansAssessmentAmended(true);
    final ApplicationDetail merits = pristineAmendment().meritsAssessmentAmended(true);
    assertThat(AmendmentUtil.hasChanges(means, pristineAmendment())).isTrue();
    assertThat(AmendmentUtil.hasChanges(merits, pristineAmendment())).isTrue();
  }

  @Test
  @DisplayName("hasChanges - updated proceeding status is a change")
  void hasChangesProceedingStatus() {
    final ApplicationDetail amendment = pristineAmendment();
    amendment.setProceedings(
        List.of(new ProceedingDetail().status(new StringDisplayValue().displayValue("Updated"))));
    assertThat(AmendmentUtil.hasChanges(amendment, pristineAmendment())).isTrue();
  }

  @Test
  @DisplayName("hasChanges - added proceeding (count differs from case) is a change")
  void hasChangesProceedingCount() {
    final ApplicationDetail amendment = pristineAmendment();
    amendment.setProceedings(
        List.of(
            new ProceedingDetail().status(unchanged()),
            new ProceedingDetail().status(unchanged())));
    assertThat(AmendmentUtil.hasChanges(amendment, pristineAmendment())).isTrue();
  }

  @Test
  @DisplayName("hasChanges - application edited after creation is a change")
  void hasChangesApplicationEdited() {
    final ApplicationDetail amendment = pristineAmendment();
    final Date created = amendment.getAuditTrail().getCreated();
    amendment.getAuditTrail().setLastSaved(new Date(created.getTime() + 60_000L));
    assertThat(AmendmentUtil.hasChanges(amendment, pristineAmendment())).isTrue();
  }

  @Test
  @DisplayName("hasChanges - opponent edited after creation is a change")
  void hasChangesOpponentEdited() {
    final ApplicationDetail amendment = pristineAmendment();
    final Date created = amendment.getAuditTrail().getCreated();
    amendment.setOpponents(
        List.of(
            new OpponentDetail()
                .auditTrail(
                    new AuditDetail()
                        .created(created)
                        .lastSaved(new Date(created.getTime() + 60_000L)))));
    assertThat(AmendmentUtil.hasChanges(amendment, pristineAmendment())).isTrue();
  }
}
