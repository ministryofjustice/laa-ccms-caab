package uk.gov.laa.ccms.caab.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.Document;
import uk.gov.laa.ccms.data.model.Notification;

class NotificationAttachmentLinksTemplateTest {

  private static final String SIGNED_URL = "https://bucket.s3.example/doc?X-Amz-Signature=abc";

  private SpringTemplateEngine engine;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
  }

  private static WebContext webContext() {
    MockServletContext servletContext = new MockServletContext();
    return new WebContext(
        JakartaServletWebApplication.buildApplication(servletContext)
            .buildExchange(
                new MockHttpServletRequest(servletContext), new MockHttpServletResponse()),
        Locale.UK);
  }

  @Test
  void aSubmittedDocumentOnTheNotificationPageLinksToTheAuditedDownload() {
    Notification notification =
        new Notification()
            .notificationId("234")
            .attachedDocuments(List.of(new Document().documentId("567").text("Letter")));
    WebContext context = webContext();
    context.setVariable("notification", notification);
    context.setVariable("documentLinks", Map.of("567", SIGNED_URL));

    String html =
        engine.process(
            new TemplateSpec(
                "notifications/notification",
                Set.of("tbody.govuk-table__body"),
                TemplateMode.HTML,
                null),
            context);

    assertThat(html)
        .contains("href=\"/notifications/234/attachments/567/download\"")
        .doesNotContain(SIGNED_URL);
  }

  @Test
  void submittedAndDraftAttachmentsLinkToTheirOwnAuditedDownloads() {
    Notification notification = new Notification().notificationId("234");
    BaseNotificationAttachmentDetail submitted =
        new BaseNotificationAttachmentDetail()
            .id(567)
            .status("Submitted")
            .sendBy("E")
            .documentType(new StringDisplayValue().displayValue("Letter"));
    BaseNotificationAttachmentDetail draft =
        new BaseNotificationAttachmentDetail()
            .id(31)
            .status("Ready to Submit")
            .sendBy("E")
            .documentType(new StringDisplayValue().displayValue("Letter"));
    Map<String, String> links = new HashMap<>();
    links.put("567", SIGNED_URL);
    links.put("31", SIGNED_URL);
    WebContext context = webContext();
    context.setVariable("notification", notification);
    context.setVariable("notificationAttachments", List.of(submitted, draft));
    context.setVariable("documentLinks", links);

    String html =
        engine.process(
            new TemplateSpec(
                "notifications/provide-documents-or-evidence",
                Set.of("tbody.govuk-table__body"),
                TemplateMode.HTML,
                null),
            context);

    assertThat(html)
        .contains("href=\"/notifications/234/attachments/567/download\"")
        .contains("href=\"/notifications/234/attachments/31/downloadDraft\"")
        .contains("form=\"remove-attachment-31\"")
        .doesNotContain("href=\"/notifications/234/attachments/31/remove\"")
        .doesNotContain(SIGNED_URL);
  }

  @Test
  void aDraftRemovalControlTargetsItsOwnPostForm() {
    WebContext context = webContext();
    context.setVariable("notification", new Notification().notificationId("234"));
    context.setVariable(
        "notificationAttachments",
        List.of(new BaseNotificationAttachmentDetail().id(31).status("Ready to Submit")));
    String html =
        engine.process(
            new TemplateSpec(
                "notifications/provide-documents-or-evidence",
                Set.of("form[th:each]"),
                TemplateMode.HTML,
                null),
            context);
    assertThat(html)
        .contains(
            "id=\"remove-attachment-31\"",
            "method=\"post\"",
            "action=\"/notifications/234/attachments/31/remove\"");
  }
}
