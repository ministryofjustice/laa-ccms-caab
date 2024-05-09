package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OPA_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;

/**
 * Service class to handle Evidence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

//  private final AssessmentService assessmentService;

//  private final EvidenceMapper evidenceMapper;

  private final ApplicationService applicationService;

  private final EbsApiClient ebsApiClient;


  private List<EvidenceDocumentTypeLookupValueDetail> getOPADocumentsRequired(
      final ApplicationDetail applicationDetail) {
    // Retrieve COMPLETED meansAssessment from the assessment api
    Mono<AssessmentDetails> meansAssessmentMono = Mono.just(buildAssessmentDetails());
//      assessmentApiClient.getAssessmentDetails(
//        AssessmentConstants.ASSESSMENT_TYPE_MEANS,
//        applicationDetail.getProviderDetails().getProvider().getId(),
//        applicationDetail.getCaseReferenceNumber(),
//        AssessmentStatus.COMPLETE);

    /* Retrieve COMPLETED meritsAssessment from the assessment api
     * TODO: discuss whether /assessments endpoint should take array of assessmentType, rather than
     *  calling twice.
     */
    Mono<AssessmentDetails> meritsAssessmentMono = Mono.just(new AssessmentDetails());
//      assessmentApiClient.getAssessmentDetails(
//        AssessmentConstants.ASSESSMENT_TYPE_MEANS,
//        applicationDetail.getProviderDetails().getProvider().getId(),
//        applicationDetail.getCaseReferenceNumber(),
//        AssessmentStatus.COMPLETE);

    // Retrieve list of evidence doc type lookups of type XXCCMS_OPA_EVIDENCE_ITEMS
    Mono<EvidenceDocumentTypeLookupDetail> documentTypesMono =
        ebsApiClient.getEvidenceDocumentTypes(COMMON_VALUE_OPA_EVIDENCE_ITEMS, null);

    Tuple3<AssessmentDetails,
        AssessmentDetails,
        EvidenceDocumentTypeLookupDetail> combinedResult =
        Mono.zip(meansAssessmentMono, meritsAssessmentMono, documentTypesMono)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve assessment data"));

    List<AssessmentDetail> meansAssessments = combinedResult.getT1().getContent();
    List<AssessmentDetail> meritsAssessments = combinedResult.getT2().getContent();
    List<EvidenceDocumentTypeLookupValueDetail> docTypes = combinedResult.getT3().getContent();

    /*
     * Build a list of assessment attributes across both the means and merits assessments.
     */
    List<AssessmentAttributeDetail> allAttributes = Stream.concat(
            meansAssessments.stream(), meritsAssessments.stream())
        .flatMap(assessment -> assessment.getEntityTypes().stream()
            .flatMap(entityType -> entityType.getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream())))
        .toList();

    /*
     * Filter the list of OPA evidence document types.
     * Retain only those which represent a required evidence item in the list of OPA attributes.
     */
    return docTypes.stream()
        .filter(docType -> isRequiredOpaEvidenceItem(docType, allAttributes))
        .toList();
  }

  private List<EvidenceDocumentTypeLookupValueDetail> getPriorAuthorityDocumentsRequired(
      final ApplicationDetail applicationDetail) {
    boolean hasPriorAuthorities = applicationDetail.getPriorAuthorities() != null
        && !applicationDetail.getPriorAuthorities().isEmpty();

    // If the application has prior auths, return all evidence of type XXCCMS_PA_EVIDENCE_ITEMS,
    // otherwise return an empty list.
    return hasPriorAuthorities
        ? ebsApiClient.getEvidenceDocumentTypes(
            COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS, null)
        .map(EvidenceDocumentTypeLookupDetail::getContent)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve evidence lookup data"))
        : new ArrayList<>();
  }

//  private List<EvidenceDocumentTypeLookupValueDetail> getCaseOutcomeDocumentsRequired(
//      final ApplicationDetail applicationDetail) {
//    applicationDetail.getCaseOutcome().getP
//  }




  private static boolean isRequiredOpaEvidenceItem(final EvidenceDocumentTypeLookupValueDetail docType,
      final List<AssessmentAttributeDetail> allAttributes) {
    return allAttributes.stream().anyMatch(
        attribute -> attribute.getName().equals(docType.getCode())
            && Boolean.parseBoolean(attribute.getValue()));
  }

  private AssessmentDetails buildAssessmentDetails() {
    return new AssessmentDetails().addContentItem(
        new AssessmentDetail()
            .addEntityTypesItem(
                new AssessmentEntityTypeDetail()
                    .addEntitiesItem(
                        new AssessmentEntityDetail()
                            .name("entity1")
                            .addAttributesItem(
                                new AssessmentAttributeDetail()
                                    .name("GB_INPUT_B_39WP3_65A")
                                    .value("true"))
                            .addAttributesItem(
                                new AssessmentAttributeDetail()
                                    .name("GB_INPUT_B_39WP3_58A")
                                    .value("true")))
                    .addEntitiesItem(
                        new AssessmentEntityDetail()
                            .name("entity2")
                            .addAttributesItem(
                                new AssessmentAttributeDetail()
                                    .name("GB_INPUT_B_39WP3_59A")
                                    .value("false"))
                            .addAttributesItem(
                                new AssessmentAttributeDetail()
                                    .name("FAS_H_ADVOC_FORM_EVID_REQ")
                                    .value("true")))));
  }


}