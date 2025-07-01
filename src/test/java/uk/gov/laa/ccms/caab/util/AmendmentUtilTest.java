package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

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
  void shouldCleanWhenEditTypeEditProvider(){
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_EDIT_PROVIDER);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getCorrespondenceAddress()).isNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNull();
    assertThat(amendmentDetail.getCosts()).isNull();
  }

  @Test
  @DisplayName("Should clean when edit type is correspondence")
  void shouldCleanWhenEditTypeCorrespondence(){
    // Given
    ApplicationDetail amendmentDetail = ApplicationDetailUtils.buildFullApplicationDetail();
    amendmentDetail.setQuickEditType(QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE);
    // When
    AmendmentUtil.cleanAppForQuickAmendSubmit(amendmentDetail);
    // Then
    assertThat(amendmentDetail.getProviderDetails().getSupervisor()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getFeeEarner()).isNull();
    assertThat(amendmentDetail.getProviderDetails().getProviderContact()).isNull();
    assertThat(amendmentDetail.getCategoryOfLaw()).isNull();
    assertThat(amendmentDetail.getCosts()).isNull();
  }

  @Test
  @DisplayName("Should clean when edit type is allocate cost limit")
  void shouldCleanWhenEditTypeAllocateCostLimit(){
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
  void shouldCleanWhenEditTypeMeansReassessment(){
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
    assertThat(amendmentDetail.getCategoryOfLaw()).isNull();
  }
}