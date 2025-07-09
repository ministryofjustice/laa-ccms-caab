package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MEANS_EVIDENCE_REQD;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MERITS_EVIDENCE_REQD;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus.COMPLETE;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntityType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;

/** Utility methods for Evidence and Document Upload. */
public final class EvidenceUtil {

  /**
   * Determine whether evidence documents are required for the supplied means, merits, application
   * type and prior authorities.
   *
   * @param meansAssessment - the means assessment.
   * @param meritsAssessment - the merits assessment.
   * @param applicationType - the application type.
   * @param priorAuthorities - the application prior authorities.
   * @return true, if evidence is required. False otherwise.
   */
  public static boolean isEvidenceRequired(
      final AssessmentDetail meansAssessment,
      final AssessmentDetail meritsAssessment,
      final ApplicationType applicationType,
      final List<PriorAuthorityDetail> priorAuthorities) {

    final boolean meansComplete =
        Optional.ofNullable(meansAssessment)
            .map(assessmentDetail -> COMPLETE.getStatus().equals(meansAssessment.getStatus()))
            .orElse(false);

    final boolean meritsComplete =
        Optional.ofNullable(meritsAssessment)
            .map(assessmentDetail -> COMPLETE.getStatus().equals(meritsAssessment.getStatus()))
            .orElse(false);

    final boolean assessmentEvidenceRequired =
        meansComplete
            && meritsComplete
            && (isAssessmentEvidenceRequired(meansAssessment, MEANS_EVIDENCE_REQD)
                || isAssessmentEvidenceRequired(meritsAssessment, MERITS_EVIDENCE_REQD));

    final boolean isEmergencyApplication = APP_TYPE_EMERGENCY.equals(applicationType.getId());

    final boolean hasPriorAuthorities =
        Optional.ofNullable(priorAuthorities)
            .map(priorAuthorityDetails -> !priorAuthorityDetails.isEmpty())
            .orElse(false);

    return assessmentEvidenceRequired || isEmergencyApplication || hasPriorAuthorities;
  }

  /**
   * Determine whether evidence is required for the supplied assessment and attribute.
   *
   * @param assessmentDetail - the assessment.
   * @param assessmentAttribute - the attribute containing the evidence flag.
   * @return true if evidence is required. False otherwise.
   */
  public static boolean isAssessmentEvidenceRequired(
      final AssessmentDetail assessmentDetail, final AssessmentAttribute assessmentAttribute) {

    AssessmentEntityTypeDetail globalEntityType =
        Optional.ofNullable(getAssessmentEntityType(assessmentDetail, GLOBAL))
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Failed to find GLOBAL entity type in assessment"));

    return globalEntityType.getEntities().stream()
        .anyMatch(
            assessmentEntity ->
                Optional.ofNullable(getAssessmentAttribute(assessmentEntity, assessmentAttribute))
                    .map(meansEvidenceAtt -> Boolean.valueOf(meansEvidenceAtt.getValue()))
                    .orElse(Boolean.FALSE));
  }

  /**
   * Check if any of the uploaded documents contains the provided evidence description. Evidence
   * descriptions are separated by a caret char.
   *
   * @param evidenceDescription - the evidence description.
   * @param evidenceUploaded - the list of uploaded evidence documents.
   * @return true if the evidence description appears in any uploaded document, false otherwise.
   */
  public static Boolean isEvidenceProvided(
      final String evidenceDescription, final List<BaseEvidenceDocumentDetail> evidenceUploaded) {
    return evidenceUploaded.stream()
        .anyMatch(
            evidence ->
                Arrays.asList(evidence.getEvidenceDescriptions().split("\\^"))
                    .contains(evidenceDescription));
  }

  private EvidenceUtil() {}
}
