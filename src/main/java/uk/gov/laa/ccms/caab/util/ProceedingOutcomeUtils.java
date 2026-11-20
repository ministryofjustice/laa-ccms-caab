package uk.gov.laa.ccms.caab.util;

import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

/** Identifies persisted clear markers so they never restore an outcome through EBS fallback. */
public final class ProceedingOutcomeUtils {
  private ProceedingOutcomeUtils() {}

  public static boolean isCleared(final ProceedingOutcomeDetail outcome) {
    return outcome != null
        && outcome.getProceedingCaseId() != null
        && outcome.getDateOfFinalWork() == null
        && !hasValue(outcome.getStageEnd())
        && !StringUtils.hasText(outcome.getResolutionMethod())
        && !hasValue(outcome.getResult())
        && !StringUtils.hasText(outcome.getResultInfo())
        && !StringUtils.hasText(outcome.getAlternativeResolution())
        && !StringUtils.hasText(outcome.getAdrInfo())
        && !StringUtils.hasText(
            ProceedingOutcomeFormData.normaliseCourtValue(outcome.getCourtCode()))
        && !StringUtils.hasText(
            ProceedingOutcomeFormData.normaliseCourtValue(outcome.getCourtName()))
        && !StringUtils.hasText(outcome.getOutcomeCourtCaseNo())
        && !StringUtils.hasText(outcome.getWiderBenefits());
  }

  private static boolean hasValue(final StringDisplayValue value) {
    return value != null
        && (StringUtils.hasText(value.getId()) || StringUtils.hasText(value.getDisplayValue()));
  }
}
