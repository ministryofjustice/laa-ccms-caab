package uk.gov.laa.ccms.caab.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
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
import java.util.Map;
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
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.S3ApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.data.model.Notification;
import uk.gov.laa.ccms.data.model.NotificationSummary;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.CoverSheet;
import uk.gov.laa.ccms.soa.gateway.model.Document;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private EbsApiClient ebsApiClient;

  @Mock
  private NotificationAttachmentMapper notificationAttachmentMapper;

  @Mock
  private S3ApiClient s3ApiClient;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void getNotificationsSummary_returnData() {

    String loginId = "user1";

    NotificationSummary mockSummary = new NotificationSummary()
        .notifications(10)
        .standardActions(5)
        .overdueActions(2);

    when(ebsApiClient.getUserNotificationSummary(loginId)).thenReturn(Mono.just(mockSummary));

    Mono<NotificationSummary> summaryMono =
        notificationService.getNotificationsSummary(loginId);

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
                    .loginId("user1")
                    .userType("user1"))
                .notificationId("234")
                .notificationType("N"));
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setAssignedToUserId("user1");

    criteria.setLoginId("user1");
    criteria.setUserType("user1");
    when(ebsApiClient.getNotifications(criteria, 1, 10))
        .thenReturn(Mono.just(notificationsMock));
    Mono<Notifications> notificationsMono = notificationService.getNotifications(criteria,
        1, 10);

    StepVerifier.create(notificationsMono)
        .expectNextMatches(notifications ->
            notifications.getContent().get(0).getUser().getLoginId().equals("user1"))
        .verifyComplete();
  }

  @Test
  void retrieveNotificationAttachment_checksS3() {
    String documentId = "documentId";

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
  void retrieveCoverSheet_checksS3() {
    String documentId = "documentId";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.of("document-url"));

    notificationService.retrieveCoverSheet(documentId,
        "loginId", "userType");

    verify(s3ApiClient).getDocumentUrl(documentId);
    verifyNoInteractions(soaApiClient);
    verify(s3ApiClient, never()).uploadDocument(any(), any(), any());
  }

  @Test
  void retrieveCoverSheet_returnsDataFromEbs() {
    String documentId = "documentId";
    String documentContent = "documentContent";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.empty());

    CoverSheet coverSheet = new CoverSheet()
        .documentId(documentId)
        .fileData(documentContent);

    when(soaApiClient.downloadCoverSheet(documentId, "loginId", "userType"))
        .thenReturn(Mono.just(coverSheet));

    notificationService.retrieveCoverSheet(documentId,
        "loginId",
        "userType");

    verify(soaApiClient).downloadCoverSheet(documentId, "loginId", "userType");
  }

  @Test
  void retrieveCoverSheet_uploadsDataToS3() {
    String documentId = "documentId";
    String documentContent = "documentContent";

    when(s3ApiClient.getDocumentUrl(eq(documentId))).thenReturn(Optional.empty());

    CoverSheet coverSheet = new CoverSheet()
        .documentId(documentId)
        .fileData(documentContent);

    when(soaApiClient.downloadCoverSheet(documentId, "loginId", "userType"))
        .thenReturn(Mono.just(coverSheet));

    notificationService.retrieveCoverSheet(documentId,
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
    notificationAttachment.setSendBy("E");
    notificationAttachment.fileData(fileData);

    when(caabApiClient.createNotificationAttachment(notificationAttachment, "loginId")).thenReturn(Mono.just(attachmentId));

    notificationService.addDraftNotificationAttachment(notificationAttachment, "loginId");

    verify(caabApiClient).createNotificationAttachment(notificationAttachment, "loginId");

    verify(s3ApiClient).uploadDraftDocument(attachmentId, fileData, "pdf");
  }

  @Test
  void updateDraftNotification_success() {
    String attachmentId = "123";
    String fileData = "fileData";

    NotificationAttachmentDetail notificationAttachment = new NotificationAttachmentDetail();
    notificationAttachment.setId(Integer.parseInt(attachmentId));
    notificationAttachment.setFileName("abc.pdf");
    notificationAttachment.setSendBy("E");
    notificationAttachment.fileData(fileData);

    when(caabApiClient.updateNotificationAttachment(notificationAttachment, "loginId")).thenReturn(Mono.empty());

    notificationService.updateDraftNotificationAttachment(notificationAttachment, "loginId");

    verify(caabApiClient).updateNotificationAttachment(notificationAttachment, "loginId");

    verify(s3ApiClient).uploadDraftDocument(attachmentId, fileData, "pdf");
  }

  @Test
  void submitNotificationAttachments_success() {
    String notificationId = "123";
    String loginId = "loginId";
    String userType = "userType";
    Integer notificationAttachmentId = 456;
    Integer providerId = 789;

    NotificationAttachmentDetail attachmentDetail = new NotificationAttachmentDetail();
    attachmentDetail.notificationReference(notificationId);
    attachmentDetail.id(notificationAttachmentId);
    BaseNotificationAttachmentDetail attachmentBase = new BaseNotificationAttachmentDetail();
    attachmentBase.notificationReference(notificationId);
    attachmentBase.id(notificationAttachmentId);

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachmentBase));

    when(caabApiClient.getNotificationAttachments(notificationId, providerId, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    when(caabApiClient.getNotificationAttachment(notificationAttachmentId)).thenReturn(Mono.just(attachmentDetail));

    Document document = new Document();

    when(notificationAttachmentMapper.toDocument(attachmentDetail)).thenReturn(document);

    ClientTransactionResponse idResponse = new ClientTransactionResponse();
    idResponse.setReferenceNumber("001");

    when(soaApiClient.uploadDocument(document, notificationId, null, loginId, userType)).thenReturn(Mono.just(idResponse));

    when(caabApiClient.deleteNotificationAttachments(notificationId, providerId, null, null, loginId))
        .thenReturn(Mono.empty());

    notificationService.submitNotificationAttachments(notificationId, loginId, userType, providerId);

    // Get notification attachments from TDS
    verify(caabApiClient).getNotificationAttachments(notificationId, providerId, null, null);
    verify(caabApiClient).getNotificationAttachment(notificationAttachmentId);

    // Upload to EBS
    verify(soaApiClient).uploadDocument(document, notificationId, null, loginId, userType);

    // Delete from TDS and S3
    verify(caabApiClient).deleteNotificationAttachments(notificationId, providerId, null, null, loginId);
    verify(s3ApiClient).removeDraftDocuments(Set.of("456"));
  }

  @Test
  void getDocumentLinks_success() {
    List<Document> documents = List.of(
        new Document().documentId("1"),
        new Document().documentId("2")
    );

    when(s3ApiClient.getDocumentUrl("1")).thenReturn(Optional.of("link 1"));
    when(s3ApiClient.getDocumentUrl("2")).thenReturn(Optional.of("link 2"));

    Map<String, String> documentLinks =
        notificationService.getDocumentLinks(documents);

    verify(s3ApiClient).getDocumentUrl("1");
    verify(s3ApiClient).getDocumentUrl("2");

    assertThat(documentLinks, hasEntry("1", "link 1"));
    assertThat(documentLinks, hasEntry("2", "link 2"));
  }

  @Test
  void getDraftDocumentLinks_success() {
    List<BaseNotificationAttachmentDetail> documents = List.of(
        new BaseNotificationAttachmentDetail().id(1),
        new BaseNotificationAttachmentDetail().id(2)
    );

    when(s3ApiClient.getDraftDocumentUrl("1")).thenReturn(Optional.of("draft link 1"));
    when(s3ApiClient.getDraftDocumentUrl("2")).thenReturn(Optional.of("draft link 2"));

    Map<String, String> documentLinks =
        notificationService.getDraftDocumentLinks(documents);

    verify(s3ApiClient).getDraftDocumentUrl("1");
    verify(s3ApiClient).getDraftDocumentUrl("2");

    assertThat(documentLinks, hasEntry("1", "draft link 1"));
    assertThat(documentLinks, hasEntry("2", "draft link 2"));
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
    Integer providerId = 456;

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail();
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail();

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, providerId, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    NotificationAttachmentDetails actualNotificationAttachmentDetails =
        notificationService.getDraftNotificationAttachments(notificationId, providerId).block();

    assertEquals(expectedNotificationAttachmentDetails, actualNotificationAttachmentDetails);

    verify(caabApiClient).getNotificationAttachments(notificationId, providerId, null, null);
  }

  @Test
  void removeDraftNotificationAttachment_success() {
    String notificationId = "123";
    Integer notificationAttachmentId = 456;
    String loginId = "loginId";
    Integer providerId = 789;

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail()
        .notificationReference(notificationId)
        .id(notificationAttachmentId)
        .fileName("file.txt")
        .sendBy("E");
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail()
        .notificationReference(notificationId)
        .id(789)
        .sendBy("E");

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, providerId, null, null))
        .thenReturn(Mono.just(notificationAttachmentDetails));

    when(caabApiClient.deleteNotificationAttachment(notificationAttachmentId, loginId)).thenReturn(Mono.empty());

    notificationService.removeDraftNotificationAttachment(notificationId,
        notificationAttachmentId, loginId, providerId);

    verify(caabApiClient).getNotificationAttachments(notificationId, providerId, null, null);
    verify(caabApiClient).deleteNotificationAttachment(notificationAttachmentId, loginId);
    verify(s3ApiClient).removeDraftDocument("456.txt");
  }

  @Test
  void removeDraftNotificationAttachment_throwsException_whenAttachmentNotRelatedToNotification() {
    String notificationId = "123";
    Integer notificationAttachmentId = 456;
    String loginId = "loginId";
    Integer providerId = 789;

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(Collections.emptyList());

    when(caabApiClient.getNotificationAttachments(notificationId, providerId, null, null))
        .thenReturn(Mono.just(notificationAttachmentDetails));

    assertThrows(CaabApplicationException.class, () ->
        notificationService.removeDraftNotificationAttachment(notificationId,
        notificationAttachmentId, loginId, providerId), "Expected CaabApplicationException to be thrown, but "
        + "wasn't.");
  }

  @Test
  void removeDraftNotificationAttachments_success() {
    String notificationId = "123";
    Integer providerId = 100;

    BaseNotificationAttachmentDetail attachment1 = new BaseNotificationAttachmentDetail();
    attachment1.notificationReference(notificationId);
    attachment1.id(456);
    BaseNotificationAttachmentDetail attachment2 = new BaseNotificationAttachmentDetail();
    attachment2.notificationReference(notificationId);
    attachment2.id(789);

    NotificationAttachmentDetails expectedNotificationAttachmentDetails =
        new NotificationAttachmentDetails();
    expectedNotificationAttachmentDetails.setContent(List.of(attachment1, attachment2));

    when(caabApiClient.getNotificationAttachments(notificationId, providerId, null, null))
        .thenReturn(Mono.just(expectedNotificationAttachmentDetails));

    when(caabApiClient.deleteNotificationAttachments(notificationId, providerId, null, null, "loginId"))
        .thenReturn(Mono.empty());

    notificationService.removeDraftNotificationAttachments(notificationId, "loginId", providerId);

    verify(caabApiClient).getNotificationAttachments(notificationId, providerId, null, null);
    verify(caabApiClient).deleteNotificationAttachments(notificationId, providerId, null, null,
        "loginId");
    verify(s3ApiClient).removeDraftDocuments(Set.of("456", "789"));
  }

}
