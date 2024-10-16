package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OPA_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_OUTCOME_DOCUMENT_CODE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.util.AssessmentModelUtils;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.Document;

@ExtendWith(MockitoExtension.class)
public class EvidenceServiceTest {

  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private EbsApiClient ebsApiClient;

  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private AssessmentService assessmentService;

  @Mock
  private CaseOutcomeService caseOutcomeService;

  @InjectMocks
  private EvidenceService evidenceService;

  private final String documentType = "docType";

  private final String fileExtension = "pdf";

  private final String documentDescription = "a doc";

  private final String applicationId = "987";

  private final String caseReferenceNumber = "123";

  private final Integer providerId = 456;

  private final String userId = "789";

  private final String userType = "EXTERNAL";

  private final CcmsModule source = CcmsModule.APPLICATION;

  private final String filename = "the file name";

  @Test
  void getEvidenceDocumentsForCase_callsApiClient() {
    EvidenceDocumentDetails evidenceDocumentDetails = new EvidenceDocumentDetails();
    when(caabApiClient.getEvidenceDocuments(
        null,
        caseReferenceNumber,
        null,
        null,
        source.getCode(),
        true)).thenReturn(Mono.just(evidenceDocumentDetails));

    Mono<EvidenceDocumentDetails> resultMono = evidenceService.getEvidenceDocumentsForCase(
        caseReferenceNumber,source);

    StepVerifier.create(resultMono)
        .expectNext(evidenceDocumentDetails)
        .verifyComplete();

    verify(caabApiClient).getEvidenceDocuments(
        null,
        caseReferenceNumber,
        null,
        null,
        source.getCode(),
        true);
  }

  @Test
  void getEvidenceDocument_callsApiClient() {
    final Integer evidenceDocumentId = 123;

    EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();
    when(caabApiClient.getEvidenceDocument(evidenceDocumentId))
        .thenReturn(Mono.just(evidenceDocumentDetail));

    Mono<EvidenceDocumentDetail> resultMono =
        evidenceService.getEvidenceDocument(evidenceDocumentId);

    StepVerifier.create(resultMono)
        .expectNext(evidenceDocumentDetail)
        .verifyComplete();

    verify(caabApiClient).getEvidenceDocument(evidenceDocumentId);
  }

  @Test
  void registerDocument_callsApiClient() {
    final String docId = "123";

    ArgumentCaptor<Document> documentArgumentCaptor =
        ArgumentCaptor.forClass(Document.class);

    final ClientTransactionResponse clientTransactionResponse = new ClientTransactionResponse();
    clientTransactionResponse.setReferenceNumber(docId);

    when(soaApiClient.registerDocument(
        any(Document.class),
        eq(userId),
        eq(userType))).thenReturn(Mono.just(clientTransactionResponse));

    final Mono<String> resultMono = evidenceService.registerDocument(
        documentType, fileExtension, documentDescription, null, userId, userType);

    StepVerifier.create(resultMono)
        .expectNext(docId)
        .verifyComplete();

    verify(soaApiClient).registerDocument(
        documentArgumentCaptor.capture(),
        eq(userId),
        eq(userType));

    assertEquals(documentType, documentArgumentCaptor.getValue().getDocumentType());
    assertEquals(fileExtension, documentArgumentCaptor.getValue().getFileExtension());
    assertEquals(documentDescription, documentArgumentCaptor.getValue().getText());
  }

  @Test
  void addDocument_callsApiClient() {
    final String docId = "123";
    EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();

    when(caabApiClient.createEvidenceDocument(
        evidenceDocumentDetail,
        userId)).thenReturn(Mono.just(docId));

    final Mono<String> resultMono = evidenceService.addDocument(
        evidenceDocumentDetail, userId);

    StepVerifier.create(resultMono)
        .expectNext(docId)
        .verifyComplete();
  }

  @Test
  void removeDocument_correctApplicationId_removesDocument() {
    final Integer docId = 123;
    final CcmsModule ccmsModule = CcmsModule.APPLICATION;

    final BaseEvidenceDocumentDetail doc = new BaseEvidenceDocumentDetail()
        .id(docId);

    when(caabApiClient.getEvidenceDocuments(
        applicationId,
        null,
        null,
        null,
        ccmsModule.getCode(),
        true)).thenReturn(Mono.just(new EvidenceDocumentDetails().addContentItem(doc)));

    when(caabApiClient.deleteEvidenceDocument(docId, userId)).thenReturn(Mono.empty());

    evidenceService.removeDocument(
        applicationId,
        docId,
        CcmsModule.APPLICATION,
        userId);

    verify(caabApiClient).deleteEvidenceDocument(
        docId,
        userId);
  }

  @Test
  void removeDocument_incorrectApplicationId_throwsException() {
    final Integer docId = 123;
    final CcmsModule ccmsModule = CcmsModule.APPLICATION;

    final BaseEvidenceDocumentDetail doc = new BaseEvidenceDocumentDetail()
        .id(456);

    when(caabApiClient.getEvidenceDocuments(
        applicationId,
        null,
        null,
        null,
        ccmsModule.getCode(),
        true)).thenReturn(Mono.just(new EvidenceDocumentDetails().addContentItem(doc)));

    assertThrows(CaabApplicationException.class, () -> evidenceService.removeDocument(
        applicationId,
        docId,
        CcmsModule.APPLICATION,
        userId));

    verify(caabApiClient, never()).deleteEvidenceDocument(
        docId,
        userId);
  }

  @Test
  void getOpaDocumentsRequired_buildsCorrectDocTypeList() {
    final AssessmentDetails assessmentDetails = new AssessmentDetails();
    AssessmentDetail meansAssessment = AssessmentModelUtils.buildAssessmentDetail(new Date());
    meansAssessment.setName(MEANS.getName());

    // Grab the first means attribute in the first entity type, and set its value to 'true'
    AssessmentAttributeDetail meansFirstAttribute = meansAssessment
        .getEntityTypes().getFirst()
        .getEntities().getFirst()
        .getAttributes().getFirst();
    meansFirstAttribute.setValue("true");

    // Grab the second means attribute in the first entity type, and set its value to 'false'
    AssessmentAttributeDetail meansSecondAttribute = meansAssessment
        .getEntityTypes().getFirst()
        .getEntities().getFirst()
        .getAttributes().get(1);
    meansSecondAttribute.setValue("false");

    AssessmentDetail meritsAssessment = AssessmentModelUtils.buildAssessmentDetail(new Date());
    meansAssessment.setName(MERITS.getName());

    // Grab the first merits attribute in the first entity type, and set its value to 'true'
    AssessmentAttributeDetail meritsFirstAttribute = meritsAssessment
        .getEntityTypes().getFirst()
        .getEntities().getFirst()
        .getAttributes().getFirst();
    meritsFirstAttribute.setValue("true");

    assessmentDetails.addContentItem(meansAssessment);
    assessmentDetails.addContentItem(meritsAssessment);

    when(assessmentService.getAssessments(
        List.of(MEANS.getName(), MERITS.getName()),
        String.valueOf(providerId),
        caseReferenceNumber,
        AssessmentStatus.COMPLETE.getStatus())).thenReturn(Mono.just(assessmentDetails));

    EvidenceDocumentTypeLookupValueDetail requiredDocType1 = new EvidenceDocumentTypeLookupValueDetail()
        .code(meansFirstAttribute.getName())
        .description("required evidence 1");
    EvidenceDocumentTypeLookupValueDetail matchedButNotRequiredDocType = new EvidenceDocumentTypeLookupValueDetail()
        .code(meansSecondAttribute.getName())
        .description("not required evidence 1");
    EvidenceDocumentTypeLookupValueDetail requiredDocType2 = new EvidenceDocumentTypeLookupValueDetail()
        .code(meritsFirstAttribute.getName())
        .description("required evidence 2");
    EvidenceDocumentTypeLookupValueDetail unmatchedNotRequiredDocType = new EvidenceDocumentTypeLookupValueDetail()
        .code("other")
        .description("not required evidence 2");

    final EvidenceDocumentTypeLookupDetail evidenceDocumentTypeLookupDetail =
        new EvidenceDocumentTypeLookupDetail();
    evidenceDocumentTypeLookupDetail.addContentItem(requiredDocType1);
    evidenceDocumentTypeLookupDetail.addContentItem(matchedButNotRequiredDocType);
    evidenceDocumentTypeLookupDetail.addContentItem(requiredDocType2);
    evidenceDocumentTypeLookupDetail.addContentItem(unmatchedNotRequiredDocType);

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_OPA_EVIDENCE_ITEMS, null)).thenReturn(
            Mono.just(evidenceDocumentTypeLookupDetail));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getOpaDocumentsRequired(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> result.size() == 2
            && result.contains(requiredDocType1)
            && result.contains(requiredDocType2) )
        .verifyComplete();

  }

  @Test
  void getOpaDocumentsRequired_noAssessments_returnsEmptyList() {
    when(assessmentService.getAssessments(
        List.of(MEANS.getName(), MERITS.getName()),
        String.valueOf(providerId),
        caseReferenceNumber,
        AssessmentStatus.COMPLETE.getStatus())).thenReturn(Mono.just(new AssessmentDetails()));

    final EvidenceDocumentTypeLookupDetail evidenceDocumentTypeLookupDetail =
        new EvidenceDocumentTypeLookupDetail();
    evidenceDocumentTypeLookupDetail.addContentItem(new EvidenceDocumentTypeLookupValueDetail()
        .code("code1")
        .description("doc 1"));

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_OPA_EVIDENCE_ITEMS, null)).thenReturn(
        Mono.just(evidenceDocumentTypeLookupDetail));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getOpaDocumentsRequired(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(List::isEmpty)
        .verifyComplete();
  }

  @Test
  void getOpaDocumentsRequired_noRequiredAttributes_returnsEmptyList() {
    final AssessmentDetails assessmentDetails = new AssessmentDetails()
        .addContentItem(AssessmentModelUtils.buildAssessmentDetail(new Date()).name(MEANS.getName()));

    // Grab the first means attribute in the first entity type, and set its value to 'true'
    AssessmentAttributeDetail meansFirstAttribute = assessmentDetails.getContent().getFirst()
        .getEntityTypes().getFirst()
        .getEntities().getFirst()
        .getAttributes().getFirst();

    when(assessmentService.getAssessments(
        List.of(MEANS.getName(), MERITS.getName()),
        String.valueOf(providerId),
        caseReferenceNumber,
        AssessmentStatus.COMPLETE.getStatus())).thenReturn(
            Mono.just(assessmentDetails));

    // Create an evidence doc type with code matching the first attribute of the means assessment
    // (which isn't set to 'true')
    final EvidenceDocumentTypeLookupDetail evidenceDocumentTypeLookupDetail =
        new EvidenceDocumentTypeLookupDetail();
    evidenceDocumentTypeLookupDetail.addContentItem(new EvidenceDocumentTypeLookupValueDetail()
        .code(meansFirstAttribute.getName())
        .description("doc 1"));

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_OPA_EVIDENCE_ITEMS, null)).thenReturn(
        Mono.just(evidenceDocumentTypeLookupDetail));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getOpaDocumentsRequired(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(List::isEmpty)
        .verifyComplete();
  }

  @Test
  void getPriorAuthDocumentsRequired_hasPriorAuth_returnsPriorAuthDocTypes() {

    // Return a prior authority for the application.
    when(caabApiClient.getPriorAuthorities(applicationId)).thenReturn(
        Mono.just(List.of(new PriorAuthorityDetail())));

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_DOCUMENT_TYPES,
        COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS)).thenReturn(
        Mono.just(new EvidenceDocumentTypeLookupDetail().addContentItem(
            new EvidenceDocumentTypeLookupValueDetail())));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getPriorAuthorityDocumentsRequired(applicationId);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> !result.isEmpty() )
        .verifyComplete();

    verify(caabApiClient).getPriorAuthorities(applicationId);
    verify(ebsApiClient).getEvidenceDocumentTypes(
        COMMON_VALUE_DOCUMENT_TYPES,
        COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS);
  }

  @Test
  void getPriorAuthDocumentsRequired_noPriorAuth_returnsEmptyPriorAuthDocTypes() {

    when(caabApiClient.getPriorAuthorities(applicationId)).thenReturn(
        Mono.just(Collections.emptyList()));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getPriorAuthorityDocumentsRequired(applicationId);

    StepVerifier.create(resultMono)
        .expectNextMatches(List::isEmpty)
        .verifyComplete();

    verify(caabApiClient).getPriorAuthorities(applicationId);
    verifyNoInteractions(ebsApiClient);
  }

  @Test
  void getCaseOutcomeDocumentsRequired_hasCaseOutcome_returnsOutcomeDocs() {

    // Return a prior authority for the application.
    when(caseOutcomeService.getCaseOutcome(caseReferenceNumber, providerId)).thenReturn(
        Optional.of(new CaseOutcomeDetail()));

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_DOCUMENT_TYPES,
        COMMON_VALUE_OUTCOME_DOCUMENT_CODE)).thenReturn(
        Mono.just(new EvidenceDocumentTypeLookupDetail().addContentItem(
            new EvidenceDocumentTypeLookupValueDetail())));

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getCaseOutcomeDocumentsRequired(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> !result.isEmpty() )
        .verifyComplete();

    verify(caseOutcomeService).getCaseOutcome(caseReferenceNumber, providerId);
    verify(ebsApiClient).getEvidenceDocumentTypes(
        COMMON_VALUE_DOCUMENT_TYPES,
        COMMON_VALUE_OUTCOME_DOCUMENT_CODE);
  }

  @Test
  void getCaseOutcomeDocumentsRequired_noCaseOutcome_returnsEmptyOutcomeDocs() {

    // No prior auth
    when(caseOutcomeService.getCaseOutcome(caseReferenceNumber, providerId)).thenReturn(
        Optional.empty());

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getCaseOutcomeDocumentsRequired(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(List::isEmpty)
        .verifyComplete();

    verify(caseOutcomeService).getCaseOutcome(caseReferenceNumber, providerId);
    verifyNoInteractions(ebsApiClient);
  }

  @Test
  void getDocumentsRequired_combinesThreeEvidenceTypes() {
    // Return empty responses - this has been tested separately.
    when(assessmentService.getAssessments(
        List.of(MEANS.getName(), MERITS.getName()),
        String.valueOf(providerId),
        caseReferenceNumber,
        AssessmentStatus.COMPLETE.getStatus())).thenReturn(Mono.just(new AssessmentDetails()));

    when(ebsApiClient.getEvidenceDocumentTypes(
        COMMON_VALUE_OPA_EVIDENCE_ITEMS, null)).thenReturn(
        Mono.just(new EvidenceDocumentTypeLookupDetail()));

    when(caabApiClient.getPriorAuthorities(applicationId)).thenReturn(
        Mono.just(Collections.emptyList()));

    when(caseOutcomeService.getCaseOutcome(caseReferenceNumber, providerId)).thenReturn(
        Optional.empty());

    Mono<List<EvidenceDocumentTypeLookupValueDetail>> resultMono =
        evidenceService.getDocumentsRequired(applicationId, caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNextMatches(List::isEmpty)
        .verifyComplete();

    verify(assessmentService).getAssessments(
        List.of(MEANS.getName(), MERITS.getName()),
        String.valueOf(providerId),
        caseReferenceNumber,
        AssessmentStatus.COMPLETE.getStatus());

    verify(caabApiClient).getPriorAuthorities(applicationId);

    verify(caseOutcomeService).getCaseOutcome(caseReferenceNumber, providerId);
  }


}