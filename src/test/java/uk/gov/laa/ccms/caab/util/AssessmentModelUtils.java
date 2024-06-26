package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MATTER_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MEANS_EVIDENCE_REQD;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MERITS_EVIDENCE_REQD;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROCEEDING_NAME;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.REQUESTED_SCOPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.OPPONENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.PROCEEDING;

import java.util.Date;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AuditDetail;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;

public class AssessmentModelUtils {

  public static AssessmentDetail buildAssessmentDetail(final Date date) {
    return new AssessmentDetail()
        .addEntityTypesItem(buildProceedingsEntityTypeDetail())
        .addEntityTypesItem(buildOpponentsEntityTypeDetail())
        .auditDetail(new AuditDetail()
            .createdBy("test")
            .created(date)
            .lastSavedBy("test")
            .lastSaved(date));
  }

  public static AssessmentDetail buildAssessmentDetailMultipleProceedings() {
    return new AssessmentDetail()
        .addEntityTypesItem(buildProceedingsEntityTypeDetailMultipleProceedings());
  }

  public static AssessmentDetail buildAssessmentDetailMultipleOpponents(final Date date) {
    return new AssessmentDetail()
        .addEntityTypesItem(buildProceedingsEntityTypeDetail())
        .addEntityTypesItem(buildOpponentsEntityTypeDetailMultipleOpponents())
        .auditDetail(new AuditDetail()
            .createdBy("test")
            .created(date)
            .lastSavedBy("test")
            .lastSaved(date));
  }

  public static AssessmentEntityTypeDetail buildMeansGlobalEntityTypeDetailWithEvidenceReqd(
      final boolean isEvidenceRequired) {
    return new AssessmentEntityTypeDetail()
        .name(GLOBAL.getType())
        .addEntitiesItem(buildGlobalEntityDetailWithMeansEvidenceReqd(isEvidenceRequired));
  }

  public static AssessmentEntityTypeDetail buildMeritsGlobalEntityTypeDetailWithEvidenceReqd(
      final boolean isEvidenceRequired) {
    return new AssessmentEntityTypeDetail()
        .name(GLOBAL.getType())
        .addEntitiesItem(buildGlobalEntityDetailWithMeritsEvidenceReqd(isEvidenceRequired));
  }

  public static AssessmentEntityTypeDetail buildProceedingsEntityTypeDetail() {
    return new AssessmentEntityTypeDetail()
        .name(PROCEEDING.getType())
        .addEntitiesItem(buildProceedingEntityDetail());
  }

  public static AssessmentEntityTypeDetail buildProceedingsEntityTypeDetailMultipleProceedings() {
    return new AssessmentEntityTypeDetail()
        .name(PROCEEDING.getType())
        .addEntitiesItem(buildProceedingEntityDetail())
        .addEntitiesItem(buildProceedingEntityDetailWithMultipleScopes());
  }

  public static AssessmentEntityTypeDetail buildOpponentsEntityTypeDetail() {
    return new AssessmentEntityTypeDetail()
        .name(OPPONENT.getType())
        .addEntitiesItem(buildOpponentEntityDetail());
  }

  public static AssessmentEntityTypeDetail buildOpponentsEntityTypeDetailMultipleOpponents() {
    return new AssessmentEntityTypeDetail()
        .name(OPPONENT.getType())
        .addEntitiesItem(buildOpponentEntityDetail())
        .addEntitiesItem(buildOpponentEntityDetail());
  }

  public static AssessmentEntityTypeDetail buildProceedingsEntityTypeDetailWithMultipleScopes() {
    return new AssessmentEntityTypeDetail()
        .name(PROCEEDING.getType())
        .addEntitiesItem(buildProceedingEntityDetailWithMultipleScopes());
  }

  public static AssessmentEntityDetail buildProceedingEntityDetail() {
    return new AssessmentEntityDetail()
        .name(String.format("%s123", InstanceMappingPrefix.PROCEEDING.getPrefix()))
        .addAttributesItem(buildMatterTypeAttribute())
        .addAttributesItem(buildProceedingNameAttribute())
        .addAttributesItem(buildClientInvolvementAttribute())
        .addAttributesItem(buildRequestedScopeAttribute("TEST"));
  }

  public static AssessmentEntityDetail buildGlobalEntityDetailWithMeansEvidenceReqd(
      final boolean isEvidenceRequired) {
    return new AssessmentEntityDetail()
        .name(GLOBAL.getType())
        .addAttributesItem(buildMeansEvidenceReqdAttribute(String.valueOf(isEvidenceRequired)));
  }

  public static AssessmentEntityDetail buildGlobalEntityDetailWithMeritsEvidenceReqd(
      final boolean isEvidenceRequired) {
    return new AssessmentEntityDetail()
        .name(GLOBAL.getType())
        .addAttributesItem(buildMeritsEvidenceReqdAttribute(String.valueOf(isEvidenceRequired)));
  }

  public static AssessmentEntityDetail buildOpponentEntityDetail() {
    return new AssessmentEntityDetail()
        .name(String.format("%s234", InstanceMappingPrefix.OPPONENT.getPrefix()));
  }

  public static AssessmentEntityDetail buildProceedingEntityDetailWithMultipleScopes() {
    return new AssessmentEntityDetail()
        .name(String.format("%s789", InstanceMappingPrefix.PROCEEDING.getPrefix()))
        .addAttributesItem(buildMatterTypeAttribute())
        .addAttributesItem(buildProceedingNameAttribute())
        .addAttributesItem(buildClientInvolvementAttribute())
        .addAttributesItem(buildRequestedScopeAttribute("MULTIPLE"));
  }

  public static AssessmentAttributeDetail buildMatterTypeAttribute() {
    return new AssessmentAttributeDetail()
        .name(MATTER_TYPE.name())
        .value("TEST");
  }

  public static AssessmentAttributeDetail buildProceedingNameAttribute() {
    return new AssessmentAttributeDetail()
        .name(PROCEEDING_NAME.name())
        .value("TEST");
  }

  public static AssessmentAttributeDetail buildClientInvolvementAttribute() {
    return new AssessmentAttributeDetail()
        .name(CLIENT_INVOLVEMENT_TYPE.name())
        .value("TEST");
  }

  public static AssessmentAttributeDetail buildRequestedScopeAttribute(
      final String value) {
    return new AssessmentAttributeDetail()
        .name(REQUESTED_SCOPE.name())
        .value(value);
  }

  public static AssessmentAttributeDetail buildMeansEvidenceReqdAttribute(
      final String value) {
    return new AssessmentAttributeDetail()
        .name(MEANS_EVIDENCE_REQD.name())
        .value(value);
  }

  public static AssessmentAttributeDetail buildMeritsEvidenceReqdAttribute(
      final String value) {
    return new AssessmentAttributeDetail()
        .name(MERITS_EVIDENCE_REQD.name())
        .value(value);
  }

}
