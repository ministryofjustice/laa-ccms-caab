package uk.gov.laa.ccms.caab.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.constants.SendBy;

class NotificationAttachmentUploadActionRenderTest {

  @Test
  void editFormSubmitsDraftIdOutsideMultipartBody() throws IOException {
    NotificationAttachmentUploadFormData form = new NotificationAttachmentUploadFormData();
    form.setSendBy(SendBy.ELECTRONIC);
    form.setDocumentId(567);

    assertThat(renderFormAction(form)).contains("sendBy=ELECTRONIC").contains("attachmentId=567");
  }

  @Test
  void newFormDoesNotSubmitDraftId() throws IOException {
    NotificationAttachmentUploadFormData form = new NotificationAttachmentUploadFormData();
    form.setSendBy(SendBy.ELECTRONIC);

    assertThat(renderFormAction(form))
        .contains("sendBy=ELECTRONIC")
        .doesNotContain("attachmentId=");
  }

  private String renderFormAction(NotificationAttachmentUploadFormData form) throws IOException {
    String template;
    try (var stream =
        getClass()
            .getResourceAsStream("/templates/notifications/upload-notification-attachment.html")) {
      if (stream == null) {
        throw new IllegalStateException("Notification upload template not found");
      }
      template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    int start = template.indexOf("<form");
    String formTag = template.substring(start, template.indexOf('>', start) + 1) + "</form>";

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(new StringTemplateResolver());
    MockServletContext servletContext = new MockServletContext();
    IWebExchange exchange =
        JakartaServletWebApplication.buildApplication(servletContext)
            .buildExchange(
                new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
    return engine.process(
        formTag,
        new WebContext(
            exchange,
            Locale.UK,
            Map.of("notificationId", "234", "attachmentUploadFormData", form)));
  }
}
