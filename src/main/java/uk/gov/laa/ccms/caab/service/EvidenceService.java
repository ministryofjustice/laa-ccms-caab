package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OPA_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OUTCOME_DOCUMENT_CODE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.BaseDocument;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

/**
 * Service class to handle Evidence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

  private final AssessmentService assessmentService;

  private final CaseOutcomeService caseOutcomeService;

  private final CaabApiClient caabApiClient;

  private final EbsApiClient ebsApiClient;

  private final SoaApiClient soaApiClient;

  /**
   * Get a List of uploaded evidence documents by case reference number and CCMS module.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param ccmsModule - the document's related CCMS module.
   * @return EvidenceDocumentDetails containing the list of EvidenceDocumentDetail.
   */
  public Mono<EvidenceDocumentDetails> getEvidenceDocuments(
      final String caseReferenceNumber, final CcmsModule ccmsModule) {
    return caabApiClient.getEvidenceDocuments(
        null,
        caseReferenceNumber,
        null,
        null,
        ccmsModule.getCode(),
        Boolean.TRUE);
  }

  /**
   * Get a single EvidenceDocumentDetail by its id.
   *
   * @param evidenceDocumentId - the id of the document.
   * @return EvidenceDocumentDetail for the supplied id.
   */
  public Mono<EvidenceDocumentDetail> getEvidenceDocument(final Integer evidenceDocumentId) {
    return caabApiClient.getEvidenceDocument(evidenceDocumentId);
  }

  /**
   * Get a combined List of all evidence document types required for the supplied
   * case reference and provider. This will include OPA, Prior Authority and Case Outcome evidence.
   *
   * @param applicationId  - the application id.
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @return List of EvidenceDocumentTypes which are required for the application.
   */
  public Mono<List<EvidenceDocumentTypeLookupValueDetail>> getDocumentsRequired(
      final String applicationId,
      final String caseReferenceNumber,
      final Integer providerId) {
    return Mono.zip(
            getOpaDocumentsRequired(caseReferenceNumber, providerId),
            getPriorAuthorityDocumentsRequired(applicationId),
            getCaseOutcomeDocumentsRequired(caseReferenceNumber, providerId))
            .map(combinedResult -> Streams.concat(
                combinedResult.getT1().stream(),
                combinedResult.getT2().stream(),
                combinedResult.getT3().stream()).toList());
  }

  /**
   * Register a new evidence document in EBS to get a document id.
   *
   * @param documentType - the document type.
   * @param fileExtension - the file extension.
   * @param documentDescription - the document description.
   * @param userDetail - the user detail.
   * @return Mono wrapping the EBS registered document id.
   */
  public String registerDocument(
      final String documentType,
      final String fileExtension,
      final String documentDescription,
      final UserDetail userDetail) {

    final BaseDocument baseDocument = new BaseDocument()
        .documentType(documentType)
        .fileExtension(fileExtension)
        .text(documentDescription);

    return soaApiClient.registerDocument(
            baseDocument,
            userDetail.getLoginId(),
            userDetail.getUserType())
        .mapNotNull(ClientTransactionResponse::getReferenceNumber)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to register document"));
  }

  /**
   * Store an evidence document in the TDS, prior to submission to EBS.
   *
   * @param userDetail - the user detail.
   * @return Mono wrapping the EBS registered document id.
   */
  public Mono<String> addDocument(
      final EvidenceDocumentDetail evidenceDocumentDetail,
      final UserDetail userDetail) {

    return caabApiClient.createEvidenceDocument(
            evidenceDocumentDetail,
            userDetail.getLoginId());
  }

  /**
   * Remove an evidence document from the TDS.
   *
   * @param documentId - the id of the document to remove.
   * @param userDetail - the user detail.
   * @return Mono wrapping the EBS registered document id.
   */
  public Mono<Void> removeDocument(
      final Integer documentId,
      final UserDetail userDetail) {

    return caabApiClient.deleteEvidenceDocument(
        documentId,
        userDetail.getLoginId());
  }

  /**
   * Get a list of OPA Evidence Document Types required for the supplied
   * application. This method will retrieve the completed means and merits assessments for the
   * application. The OPA Evidence Document Types will then be filtered to retain only
   * those which have a matching assessment attribute with value 'true'.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @return List of OPA EvidenceDocumentTypes which are required for the application.
   */
  protected Mono<List<EvidenceDocumentTypeLookupValueDetail>> getOpaDocumentsRequired(
      final String caseReferenceNumber,
      final Integer providerId) {
    return assessmentService.getAssessments(
            List.of(MEANS.getName(), MERITS.getName()),
            String.valueOf(providerId),
            caseReferenceNumber,
            AssessmentStatus.COMPLETE.getStatus())
        .map(assessmentDetails -> assessmentDetails.getContent().stream()
            .flatMap(this::flattenAttributes)
            .toList())
        .flatMap(allAttributes -> ebsApiClient.getEvidenceDocumentTypes(
                COMMON_VALUE_OPA_EVIDENCE_ITEMS, null)
            .map(docTypesLookup -> docTypesLookup.getContent().stream()
                .filter(docType -> isRequiredOpaEvidenceItem(
                    docType, allAttributes)).toList()));
  }

  /**
   * Get a list of Prior Authority Evidence Document Types required for the supplied
   * application id. If the application has any associated Prior Authorities, this method
   * will return the complete list of Prior Authority Evidence Document Types.
   *
   * @param applicationId - the application id
   * @return Mono containing a Lookup of Prior Authority EvidenceDocumentTypes
   *     which are required for the application.
   */
  protected Mono<List<EvidenceDocumentTypeLookupValueDetail>> getPriorAuthorityDocumentsRequired(
      final String applicationId) {

    // If the application has prior auths, return all evidence of type XXCCMS_PA_EVIDENCE_ITEMS,
    // otherwise return an empty LookupDetail.
    return caabApiClient.getPriorAuthorities(applicationId)
        .map(priorAuthorityDetails -> !priorAuthorityDetails.isEmpty())
        .flatMap(hasPriorAuthority -> hasPriorAuthority
            ? ebsApiClient.getEvidenceDocumentTypes(
                COMMON_VALUE_DOCUMENT_TYPES, COMMON_VALUE_OUTCOME_DOCUMENT_CODE) :
            Mono.just(new EvidenceDocumentTypeLookupDetail()))
        .map(EvidenceDocumentTypeLookupDetail::getContent);
  }

  /**
   * Get a list of Case Outcome Evidence Document Types required for the supplied
   * application. If the application has an outcome, this method
   * will return the complete list of Case Outcome Evidence Document Types.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @return Mono containing a Lookup of Case Outcome EvidenceDocumentTypes
   *     which are required for the application.
   */
  protected Mono<List<EvidenceDocumentTypeLookupValueDetail>> getCaseOutcomeDocumentsRequired(
      final String caseReferenceNumber,
      final Integer providerId) {

    // If the application has an outcome, return all evidence of type XXCCMS_DOCUMENT_TYPES with
    // code 'OUT_EV'. Otherwise return an empty LookupDetail.
    return caseOutcomeService.getCaseOutcome(caseReferenceNumber, providerId)
        .map(caseOutcomeDetail -> ebsApiClient.getEvidenceDocumentTypes(
            COMMON_VALUE_DOCUMENT_TYPES, COMMON_VALUE_OUTCOME_DOCUMENT_CODE))
        .orElse(Mono.just(new EvidenceDocumentTypeLookupDetail()))
        .map(EvidenceDocumentTypeLookupDetail::getContent);
  }

  private boolean isRequiredOpaEvidenceItem(
      final EvidenceDocumentTypeLookupValueDetail docType,
      final List<AssessmentAttributeDetail> allAttributes) {
    return allAttributes.stream().anyMatch(
        attribute -> attribute.getName().equals(docType.getCode())
            && Boolean.parseBoolean(attribute.getValue()));
  }

  private Stream<AssessmentAttributeDetail> flattenAttributes(AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .flatMap(entityType -> entityType.getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream()));
  }
}