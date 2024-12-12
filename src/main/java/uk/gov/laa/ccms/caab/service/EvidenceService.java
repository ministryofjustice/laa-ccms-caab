package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OPA_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OUTCOME_DOCUMENT_CODE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.util.EvidenceUtil.isEvidenceProvided;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.util.EvidenceUtil;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.Document;

/**
 * Service class to handle Evidence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

  private final AssessmentService assessmentService;

  private final CaseOutcomeService caseOutcomeService;

  private final EvidenceMapper evidenceMapper;

  private final CaabApiClient caabApiClient;

  private final EbsApiClient ebsApiClient;

  private final SoaApiClient soaApiClient;

  /**
   * Get a List of uploaded evidence documents by application/outcome id and CCMS module.
   *
   * @param applicationOrOutcomeId - the application or outcome id.
   * @param ccmsModule - the document's related CCMS module.
   * @return EvidenceDocumentDetails containing the list of EvidenceDocumentDetail.
   */
  public Mono<EvidenceDocumentDetails> getEvidenceDocumentsForApplicationOrOutcome(
      final String applicationOrOutcomeId, final CcmsModule ccmsModule) {
    return caabApiClient.getEvidenceDocuments(
        applicationOrOutcomeId,
        null,
        null,
        null,
        ccmsModule.getCode(),
        Boolean.TRUE);
  }

  /**
   * Get a List of uploaded evidence documents by case reference number and CCMS module.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param ccmsModule - the document's related CCMS module.
   * @return EvidenceDocumentDetails containing the list of EvidenceDocumentDetail.
   */
  public Mono<EvidenceDocumentDetails> getEvidenceDocumentsForCase(
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
   * @param userId - the user registering the document.
   * @param userType - the user type.
   * @return Mono wrapping the EBS registered document id.
   */
  public Mono<String> registerDocument(
      final String documentType,
      final String fileExtension,
      final String documentDescription,
      final String channel,
      final String userId,
      final String userType) {

    final Document document = new Document()
        .channel(channel)
        .documentType(documentType)
        .fileExtension(fileExtension)
        .text(documentDescription);

    return soaApiClient.registerDocument(
            document,
            userId,
            userType)
        .mapNotNull(ClientTransactionResponse::getReferenceNumber);
  }

  /**
   * Store an evidence document in the TDS, prior to submission to EBS.
   *
   * @param evidenceDocumentDetail - the evidence document detail.
   * @param userId - the user adding the document.
   * @return Mono wrapping the EBS registered document id.
   */
  public Mono<String> addDocument(
      final EvidenceDocumentDetail evidenceDocumentDetail,
      final String userId) {

    return caabApiClient.createEvidenceDocument(evidenceDocumentDetail, userId);
  }

  public Mono<Void> updateDocument(
      final EvidenceDocumentDetail evidenceDocumentDetail,
      final String userId) {
    return caabApiClient.updateEvidenceDocument(evidenceDocumentDetail, userId);
  }

  /**
   * Remove an evidence document from the TDS.
   *
   * @param documentId - the id of the document to remove.
   * @param userId - the user removing a document.
   */
  public void removeDocument(
      final String applicationOrOutcomeId,
      final Integer documentId,
      final CcmsModule ccmsModule,
      final String userId) {

    // First ensure that the document exists and is related to the active case.
    // (We don't want to retrieve the document by its id, as that will include the (possibly)
    // 8MB of file data.
    getEvidenceDocumentsForApplicationOrOutcome(
            applicationOrOutcomeId,
            ccmsModule)
        .map(EvidenceDocumentDetails::getContent)
        .mapNotNull(baseEvidenceDocumentDetails ->
            baseEvidenceDocumentDetails.stream()
                .filter(baseEvidenceDocumentDetail -> baseEvidenceDocumentDetail
                    .getId().equals(documentId))
                .findFirst()
                .orElse(null))
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Invalid document id: %s", documentId)));

    caabApiClient.deleteEvidenceDocument(documentId, userId).block();
  }

  /**
   * Remove all untransferred evidence documents for the specified case.
   *
   * @param caseReferenceNumber - the case reference number of the documents to remove.
   * @param userId - the user removing the documents.
   */
  public Mono<Void> removeDocuments(
      final String caseReferenceNumber,
      final String userId) {

    return caabApiClient.deleteEvidenceDocuments(
        null,
        caseReferenceNumber,
        null,
        null,
        null,
        Boolean.TRUE, // Only delete documents that haven't been transferred.
        userId);
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
        .flatMap(priorAuthorityDetails -> priorAuthorityDetails.isEmpty()
            ? Mono.just(new EvidenceDocumentTypeLookupDetail()) :
            ebsApiClient.getEvidenceDocumentTypes(
                COMMON_VALUE_DOCUMENT_TYPES,
                COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS))
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
            COMMON_VALUE_DOCUMENT_TYPES,
            COMMON_VALUE_OUTCOME_DOCUMENT_CODE))
        .orElse(Mono.just(new EvidenceDocumentTypeLookupDetail()))
        .map(EvidenceDocumentTypeLookupDetail::getContent);
  }

  /**
   * Determine whether evidence documents are required for the supplied means, merits,
   * application type and prior authorities.
   *
   * @param meansAssessment - the means assessment.
   * @param meritsAssessment - the merits assessment.
   * @param applicationType - the application type.
   * @param priorAuthorities - the application prior authorities.
   * @return true, if evidence is required. False otherwise.
   */
  public boolean isEvidenceRequired(
      final AssessmentDetail meansAssessment,
      final AssessmentDetail meritsAssessment,
      final ApplicationType applicationType,
      final List<PriorAuthorityDetail> priorAuthorities) {

    return EvidenceUtil.isEvidenceRequired(
        meansAssessment,
        meritsAssessment,
        applicationType,
        priorAuthorities);
  }

  /**
   * Determine whether all required evidence has been provided for the related application.
   *
   * @param applicationId - the application id.
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @return true if all evidence has been provided, false otherwise.
   */
  public boolean isAllEvidenceProvided(
      final String applicationId,
      final String caseReferenceNumber,
      final Integer providerId) {

    // Get the list of required evidence docs for this application.
    final Mono<List<EvidenceDocumentTypeLookupValueDetail>> evidenceRequiredMono =
        getDocumentsRequired(applicationId, caseReferenceNumber, providerId);

    // Retrieve the list of previously uploaded documents.
    final Mono<EvidenceDocumentDetails> evidenceUploadedMono =
        getEvidenceDocumentsForCase(caseReferenceNumber, APPLICATION);

    final Tuple2<List<EvidenceDocumentTypeLookupValueDetail>,
        EvidenceDocumentDetails> combinedResult = Mono.zip(
            evidenceRequiredMono,
            evidenceUploadedMono)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve evidence data"));

    final List<EvidenceDocumentTypeLookupValueDetail> evidenceRequired = combinedResult.getT1();
    final List<BaseEvidenceDocumentDetail> evidenceProvided = combinedResult.getT2().getContent();

    return evidenceRequired.stream()
        .allMatch(required -> isEvidenceProvided(required.getDescription(), evidenceProvided));
  }

  private boolean isRequiredOpaEvidenceItem(
      final EvidenceDocumentTypeLookupValueDetail docType,
      final List<AssessmentAttributeDetail> allAttributes) {
    return allAttributes.stream().anyMatch(
        attribute -> attribute.getName().equals(docType.getCode())
            && Boolean.parseBoolean(attribute.getValue()));
  }

  private Stream<AssessmentAttributeDetail> flattenAttributes(final AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .flatMap(entityType -> entityType.getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream()));
  }

  /**
   * Uploads and updates the status of multiple evidence documents for a given case reference.
   *
   * @param evidenceDocumentDetails the details of the evidence documents to be uploaded and updated
   * @param caseReferenceNumber the reference number of the case to which the documents belong
   * @param user the user details for the upload operation
   * @return a {@link Mono} that completes when all uploads and updates are done
   */
  public Mono<Void> uploadAndUpdateDocuments(
      final EvidenceDocumentDetails evidenceDocumentDetails,
      final String caseReferenceNumber,
      final String notificationId,
      final UserDetail user) {

    if (evidenceDocumentDetails != null) {
      return Flux.fromIterable(evidenceDocumentDetails.getContent())
          .flatMap(evidenceDocumentDetail ->
              uploadAndUpdateDocument(
                  evidenceDocumentDetail, caseReferenceNumber, notificationId, user))
          .then();
    } else {
      return Mono.empty();
    }
  }

  /**
   * Uploads and updates the status of an evidence document for a given case reference.
   * If the document has already been successfully transferred, no action is performed.
   *
   * @param evidenceDocument the evidence document to be uploaded and updated
   * @param caseReferenceNumber the reference number of the case to which the document belongs
   * @param user the user details for the upload operation
   * @return a {@link Mono} that completes when the upload and update are done
   */
  public Mono<Void> uploadAndUpdateDocument(
      final BaseEvidenceDocumentDetail evidenceDocument,
      final String caseReferenceNumber,
      final String notificationId,
      final UserDetail user) {

    // Return an empty Mono if the evidenceDocument is null
    if (evidenceDocument == null) {
      return Mono.empty();
    }

    // Retrieve and log the transfer status of the evidence document
    final String transferStatus = evidenceDocument.getTransferStatus();
    log.debug("evidenceDocument.getStatus() - " + transferStatus);

    // If the document has already been successfully transferred, no further action is needed
    if ("Success".equalsIgnoreCase(transferStatus)) {
      return Mono.empty();
    }

    final Integer documentId = evidenceDocument.getId();

    // Proceed to upload and update the document
    return this.getEvidenceDocument(documentId)
        .flatMap(detailedEvidenceDocument ->
            uploadDocumentAndUpdateStatus(
                detailedEvidenceDocument, caseReferenceNumber, notificationId, user, documentId))
        .onErrorResume(e -> handleUploadError(e, documentId, user.getLoginId()));
  }

  /**
   * Uploads the document to the SOA API and updates its transfer status to "SUCCESS".
   *
   * @param detailedEvidenceDocument The detailed evidence document with full data.
   * @param caseReferenceNumber      The case reference number.
   * @param user                     The user details.
   * @param documentId               The ID of the document.
   * @return A Mono signaling when the operation has completed.
   */
  protected Mono<Void> uploadDocumentAndUpdateStatus(
      final EvidenceDocumentDetail detailedEvidenceDocument,
      final String caseReferenceNumber,
      final String notificationId,
      final UserDetail user,
      final Integer documentId) {

    // Map the detailed evidence document to the Document type required by the SOA API
    final Document document = evidenceMapper.toDocument(detailedEvidenceDocument);

    final boolean isDocumentPreviouslyRegisteredInEbs =
        detailedEvidenceDocument.getRegisteredDocumentId() != null;

    // Determine the API operation (upload or update) based on if the document is already
    // registered
    final Mono<ClientTransactionResponse> apiOperation = isDocumentPreviouslyRegisteredInEbs
        ? soaApiClient.updateDocument(
            document, notificationId, caseReferenceNumber, user.getLoginId(), user.getUserType())
        : soaApiClient.uploadDocument(
            document, notificationId, caseReferenceNumber, user.getLoginId(), user.getUserType());

    // Perform the API operation and update the document's transfer status after success
    return apiOperation.then(updateDocument(
        new EvidenceDocumentDetail()
            .id(documentId)
            .notificationReference(notificationId)
            .transferStatus("SUCCESS")
            .transferResponseCode("200")
            .transferResponseDescription("Successfully uploaded document"),
        user.getLoginId()));
  }

  /**
   * Handles errors that occur during the upload and updates the document's status to "FAILED".
   *
   * @param e        The exception that was thrown.
   * @param documentId The ID of the document.
   * @param loginId   The login ID of the user.
   * @return A Mono signaling when the error handling has completed.
   */
  protected Mono<Void> handleUploadError(
      final Throwable e,
      final Integer documentId,
      final String loginId) {
    // Log the error that occurred during upload or update
    log.error("Error uploading and updating document", e);

    // Update the document's transfer status to "FAILED" due to the error
    return updateDocument(
        new EvidenceDocumentDetail()
            .id(documentId)
            .transferStatus("FAILED")
            .transferResponseCode("404")
            .transferResponseDescription("WebService call to operation uploadDocument failed"),
        loginId);
  }

  /**
   * Registers previously uploaded documents if they are not already registered.
   *
   * @param evidenceDocumentDetails the details of the evidence documents to be registered
   * @param user the details of the user performing the registration
   */
  public void registerPreviouslyUploadedDocuments(
      final EvidenceDocumentDetails evidenceDocumentDetails, final UserDetail user) {
    if (evidenceDocumentDetails != null) {

      for (final BaseEvidenceDocumentDetail evidenceDocumentDetail :
          evidenceDocumentDetails.getContent()) {

        log.debug("Documents ID - " + evidenceDocumentDetail.getId() + ", - DOC_TYPE - "
            + evidenceDocumentDetail.getDocumentType().getDisplayValue());

        if (evidenceDocumentDetail.getRegisteredDocumentId() == null) {

          final String registeredDocumentId = registerDocument(
              evidenceDocumentDetail.getDocumentType().getId(),
              evidenceDocumentDetail.getFileExtension(),
              evidenceDocumentDetail.getDescription(),
              ELECTRONIC.getCode(),
              user.getLoginId(),
              user.getUserType())
              .blockOptional()
              .orElseThrow(() -> new CaabApplicationException("Failed to register document"));

          if (registeredDocumentId != null) {
            //set on the existing object in memory
            evidenceDocumentDetail.setRegisteredDocumentId(registeredDocumentId);

            //create the patch request
            final EvidenceDocumentDetail patch = new EvidenceDocumentDetail()
                .id(evidenceDocumentDetail.getId())
                .registeredDocumentId(registeredDocumentId);

            updateDocument(patch, user.getLoginId()).block();

          }
        }
      }
    }
  }
}