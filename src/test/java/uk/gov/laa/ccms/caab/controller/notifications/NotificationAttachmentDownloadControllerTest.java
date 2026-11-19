package uk.gov.laa.ccms.caab.controller.notifications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.config.LoggingInterceptor;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.Document;
import uk.gov.laa.ccms.data.model.Notification;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class NotificationAttachmentDownloadControllerTest {

  private static final String SIGNED_URL = "https://bucket.s3.example/doc?X-Amz-Signature=abc";

  private static final UserDetail USER =
      new UserDetail().userId(1).provider(new BaseProvider().id(10));

  @InjectMocks NotificationAttachmentDownloadController controller;
  @Mock NotificationService notificationService;
  @Mock SecurityEventLogger securityEventLogger;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(new LoggingInterceptor(securityEventLogger))
            .build();
  }

  @Test
  void aSubmittedDocumentOfTheNotificationRedirectsToItsSignedUrl() throws Exception {
    when(notificationService.getNotification("234", 1, 10))
        .thenReturn(Mono.just(notificationWithSubmittedDocument("567")));
    when(notificationService.getDocumentLink("567")).thenReturn(Optional.of(SIGNED_URL));

    mockMvc
        .perform(get("/notifications/234/attachments/567/download").sessionAttr("user", USER))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", SIGNED_URL))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void aSubmittedDownloadIsAuditedWithTheNotificationAndAttachmentIds() throws Exception {
    when(notificationService.getNotification("234", 1, 10))
        .thenReturn(Mono.just(notificationWithSubmittedDocument("567")));
    when(notificationService.getDocumentLink("567")).thenReturn(Optional.of(SIGNED_URL));

    mockMvc
        .perform(get("/notifications/234/attachments/567/download").sessionAttr("user", USER))
        .andExpect(status().isFound());

    verify(securityEventLogger)
        .dataAccessed(
            eq("VIEW"),
            eq(true),
            any(),
            any(),
            eq(Map.of("notification_id", "234", "attachment_id", "567")),
            any());
  }

  @Test
  void aDocumentThatIsNotOnTheNotificationIsNotFoundAndNoLinkIsSigned() throws Exception {
    when(notificationService.getNotification("234", 1, 10))
        .thenReturn(Mono.just(notificationWithSubmittedDocument("567")));

    mockMvc
        .perform(get("/notifications/234/attachments/999/download").sessionAttr("user", USER))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"))
        .andExpect(model().attribute("error", "Not Found"));

    verify(notificationService, never()).getDocumentLink(any());
    verify(securityEventLogger).dataAccessed(eq("VIEW"), eq(false), any(), any(), any(), any());
  }

  @Test
  void aNotificationTheUserCannotSeeIsNotFound() throws Exception {
    when(notificationService.getNotification("234", 1, 10)).thenReturn(Mono.empty());

    mockMvc
        .perform(get("/notifications/234/attachments/567/download").sessionAttr("user", USER))
        .andExpect(status().isNotFound());

    verify(notificationService, never()).getDocumentLink(any());
  }

  @Test
  void aDocumentMissingFromS3IsNotFound() throws Exception {
    when(notificationService.getNotification("234", 1, 10))
        .thenReturn(Mono.just(notificationWithSubmittedDocument("567")));
    when(notificationService.getDocumentLink("567")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/notifications/234/attachments/567/download").sessionAttr("user", USER))
        .andExpect(status().isNotFound());
  }

  @Test
  void aDraftAttachmentOfTheNotificationRedirectsToItsSignedUrl() throws Exception {
    when(notificationService.getDraftNotificationAttachments("234", 1))
        .thenReturn(Mono.just(draftAttachments(31)));
    when(notificationService.getDraftDocumentLink("31")).thenReturn(Optional.of(SIGNED_URL));

    mockMvc
        .perform(get("/notifications/234/attachments/31/downloadDraft").sessionAttr("user", USER))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", SIGNED_URL));

    verify(securityEventLogger)
        .dataAccessed(
            eq("VIEW"),
            eq(true),
            any(),
            any(),
            eq(Map.of("notification_id", "234", "attachment_id", "31")),
            any());
  }

  @Test
  void aDraftThatIsNotOnTheNotificationIsNotFoundAndNoLinkIsSigned() throws Exception {
    when(notificationService.getDraftNotificationAttachments("234", 1))
        .thenReturn(Mono.just(draftAttachments(31)));

    mockMvc
        .perform(get("/notifications/234/attachments/77/downloadDraft").sessionAttr("user", USER))
        .andExpect(status().isNotFound());

    verify(notificationService, never()).getDraftDocumentLink(any());
    verify(notificationService, never()).getDocumentLink(any());
  }

  @Test
  void aNotificationWithNoDocumentsIsNotFound() throws Exception {
    when(notificationService.getNotification(any(), anyInt(), anyInt()))
        .thenReturn(Mono.just(new Notification().notificationId("234")));

    mockMvc
        .perform(get("/notifications/234/attachments/567/download").sessionAttr("user", USER))
        .andExpect(status().isNotFound());
  }

  private static Notification notificationWithSubmittedDocument(String documentId) {
    return new Notification()
        .notificationId("234")
        .uploadedDocuments(List.of(new Document().documentId(documentId)));
  }

  private static NotificationAttachmentDetails draftAttachments(int id) {
    return new NotificationAttachmentDetails()
        .content(List.of(new BaseNotificationAttachmentDetail().id(id)));
  }
}
