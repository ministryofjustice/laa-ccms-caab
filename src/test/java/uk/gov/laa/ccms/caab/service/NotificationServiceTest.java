package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.soa.gateway.model.BaseDocument;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;
import uk.gov.laa.ccms.soa.gateway.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private NotificationAttachmentMapper notificationAttachmentMapper;

  @Mock
  private S3ApiClient s3ApiClient;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void getNotificationsSummary_returnData() {

    String loginId = "user1";
    String userType = "userType";

    NotificationSummary mockSummary = new NotificationSummary()
        .notifications(10)
        .standardActions(5)
        .overdueActions(2);

    when(soaApiClient.getNotificationsSummary(loginId, userType)).thenReturn(Mono.just(mockSummary));

    Mono<NotificationSummary> summaryMono =
        notificationService.getNotificationsSummary(loginId, userType);

    StepVerifier.create(summaryMono)
        .expectNextMatches(summary ->
            summary.getNotifications() == 10 &&
                summary.getStandardActions() == 5 &&
                summary.getOverdueActions() == 2)
        .verifyComplete();
  }

  @Test
  void getNotifications_returnsData() {

    Notifications notificationsMock = new Notifications();
    notificationsMock
        .addContentItem(
            new Notification()
                .user(new UserDetail()
                    .userLoginId("user1")
                    .userType("user1"))
                .notificationId("234")
                .notificationType("N"));
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setAssignedToUserId("user1");

    criteria.setLoginId("user1");
    criteria.setUserType("user1");
    when(soaApiClient.getNotifications(criteria,1, 10))
        .thenReturn(Mono.just(notificationsMock));
    Mono<Notifications> notificationsMono = notificationService.getNotifications(criteria,
        1, 10);

    StepVerifier.create(notificationsMono)
        .expectNextMatches(notifications ->
            notifications.getContent().get(0).getUser().getUserLoginId().equals("user1"))
        .verifyComplete();
  }

  @Test
  void retrieveNotificationAttachment_checksS3() {
    String documentId = "documentId";
    String documentContent = "documentContent";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.of("document-url"));

    notificationService.retrieveNotificationAttachment(documentId,
        "loginId", "userType");

    verify(s3ApiClient).getDocumentUrl(documentId);
    verifyNoInteractions(soaApiClient);
    verify(s3ApiClient, never()).uploadDocument(any(), any(), any());
  }

  @Test
  void retrieveNotificationAttachment_returnsDataFromEbs() {
    String documentId = "documentId";
    String documentContent = "documentContent";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.empty());

    Document document = new Document()
        .documentId(documentId)
        .fileData(documentContent);

    when(soaApiClient.downloadDocument(documentId, "loginId", "userType"))
        .thenReturn(Mono.just(document));

    notificationService.retrieveNotificationAttachment(documentId,
        "loginId",
        "userType");

    verify(soaApiClient).downloadDocument(documentId, "loginId", "userType");
  }

  @Test
  void retrieveNotificationAttachment_uploadsDataToS3() {
    String documentId = "documentId";
    String documentContent = "documentContent";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.empty());

    Document document = new Document()
        .documentId(documentId)
        .fileData(documentContent)
        .fileExtension("pdf");

    when(soaApiClient.downloadDocument(documentId, "loginId", "userType"))
        .thenReturn(Mono.just(document));

    notificationService.retrieveNotificationAttachment(documentId,
        "loginId",
        "userType");

    verify(s3ApiClient).uploadDocument(documentId, documentContent, "pdf");
  }

  @Test
  void addDraftNotification_success() {
    String attachmentId = "123";
    String fileData = "fileData";

    NotificationAttachmentDetail notificationAttachment = new NotificationAttachmentDetail();
    notificationAttachment.setFileName("abc.pdf");
    notificationAttachment.fileData(fileData);

    when(caabApiClient.createNotificationAttachment(notificationAttachment, "loginId")).thenReturn(Mono.just(attachmentId));

    notificationService.addDraftNotificationAttachment(notificationAttachment, "loginId");

    verify(caabApiClient).createNotificationAttachment(notificationAttachment, "loginId");

    verify(s3ApiClient).uploadDraftDocument(attachmentId, fileData, "pdf");
  }

  @Test
  void submitNotificationAttachments_success() {
    String notificationId = "123";
    String loginId = "loginId";
    String userType = "userType";
    Integer notificationAttachmentId = 456;

    NotificationAttachmentDetail attachmentDetail = new NotificationAttachmentDetail();
    attachmentDetail.notificationReference(notificationId);
    attachmentDetail.id(notificationAttachmentId);
    BaseNotificationAttachmentDetail attachmentBase = new BaseNotificationAttachmentDetail();
    attachmentBase.notificationReference(notificationId);
    attachmentBase.id(notificationAttachmentId);

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachmentBase));

    when(caabApiClient.getNotificationAttachments(notificationId, null, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    when(caabApiClient.getNotificationAttachment(notificationAttachmentId)).thenReturn(Mono.just(attachmentDetail));

    BaseDocument baseDocument = new BaseDocument();
    Document document = new Document();

    when(notificationAttachmentMapper.toBaseDocument(attachmentDetail)).thenReturn(baseDocument);
    when(notificationAttachmentMapper.toDocument(attachmentDetail)).thenReturn(document);

    ClientTransactionResponse idResponse = new ClientTransactionResponse();
    idResponse.setReferenceNumber("001");

    when(soaApiClient.registerDocument(baseDocument, loginId, userType)).thenReturn(Mono.just(idResponse));
    when(soaApiClient.uploadDocument(document, loginId, userType)).thenReturn(Mono.just(idResponse));

    when(caabApiClient.deleteNotificationAttachments(notificationId, null, null, null, loginId))
        .thenReturn(Mono.empty());

    notificationService.submitNotificationAttachments(notificationId, loginId, userType);

    // Get notification attachments from TDS
    verify(caabApiClient).getNotificationAttachments(notificationId, null, null, null);
    verify(caabApiClient).getNotificationAttachment(notificationAttachmentId);

    // Register and upload to EBS
    verify(soaApiClient).registerDocument(baseDocument, loginId, userType);
    verify(soaApiClient).uploadDocument(document, loginId, userType);

    // Delete from TDS and S3
    verify(caabApiClient).deleteNotificationAttachments(notificationId, null, null, null, loginId);
    verify(s3ApiClient).removeDraftDocuments(Set.of("456"));
  }

  @Test
  void getDraftNotificationAttachment_success() {
    Integer attachmentId = 123;

    NotificationAttachmentDetail expectedAttachment = new NotificationAttachmentDetail();

    when(caabApiClient.getNotificationAttachment(attachmentId)).thenReturn(Mono.just(expectedAttachment));

    NotificationAttachmentDetail actualAttachment =
        notificationService.getDraftNotificationAttachment(attachmentId).block();

    assertEquals(expectedAttachment, actualAttachment);

    verify(caabApiClient).getNotificationAttachment(attachmentId);
  }

  @Test
  void getDraftNotificationAttachments_success() {
    String notificationId = "123";

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail();
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail();

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, null, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    NotificationAttachmentDetails actualNotificationAttachmentDetails =
        notificationService.getDraftNotificationAttachments(notificationId).block();

    assertEquals(expectedNotificationAttachmentDetails, actualNotificationAttachmentDetails);

    verify(caabApiClient).getNotificationAttachments(notificationId, null, null, null);
  }

  @Test
  void removeDraftNotificationAttachment_success() {
    String notificationId = "123";
    Integer notificationAttachmentId = 456;
    String loginId = "loginId";

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail();
    attachment1.notificationReference(notificationId);
    attachment1.id(notificationAttachmentId);
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail();
    attachment2.notificationReference(notificationId);
    attachment2.id(789);

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, null, null, null))
        .thenReturn(Mono.just(notificationAttachmentDetails));

    when(caabApiClient.deleteNotificationAttachment(notificationAttachmentId, loginId)).thenReturn(Mono.empty());

    notificationService.removeDraftNotificationAttachment(notificationId,
        notificationAttachmentId, loginId);

    verify(caabApiClient).getNotificationAttachments(notificationId, null, null, null);
    verify(caabApiClient).deleteNotificationAttachment(notificationAttachmentId, loginId);
    verify(s3ApiClient).removeDraftDocument("456");
  }

  @Test
  void removeDraftNotificationAttachment_throwsException_whenAttachmentNotRelatedToNotification() {
    String notificationId = "123";
    Integer notificationAttachmentId = 456;
    String loginId = "loginId";

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(Collections.emptyList());

    when(caabApiClient.getNotificationAttachments(notificationId, null, null, null))
        .thenReturn(Mono.just(notificationAttachmentDetails));

    assertThrows(CaabApplicationException.class, () ->
        notificationService.removeDraftNotificationAttachment(notificationId,
        notificationAttachmentId, loginId), "Expected CaabApplicationException to be thrown, but "
        + "wasn't.");
  }

  @Test
  void removeDraftNotificationAttachments_success() {
    String notificationId = "123";

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail();
    attachment1.notificationReference(notificationId);
    attachment1.id(456);
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail();
    attachment2.notificationReference(notificationId);
    attachment2.id(789);

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, null, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    when(caabApiClient.deleteNotificationAttachments(notificationId, null, null, null, "loginId"))
        .thenReturn(Mono.empty());

    notificationService.removeDraftNotificationAttachments(notificationId, "loginId");

    verify(caabApiClient).getNotificationAttachments(notificationId, null, null, null);
    verify(caabApiClient).deleteNotificationAttachments(notificationId, null, null, null,
        "loginId");
    verify(s3ApiClient).removeDraftDocuments(Set.of("456", "789"));
  }

}
