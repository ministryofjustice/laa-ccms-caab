package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.Document;
import uk.gov.laa.ccms.data.model.Notification;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Serves notification attachment downloads through the application so that each one is audited,
 * then redirects to the signed S3 URL.
 */
@Controller
@RequiredArgsConstructor
public class NotificationAttachmentDownloadController {

  private final NotificationService notificationService;

  /**
   * Redirects to a submitted document of a notification the user can see.
   *
   * @param user the logged-in user.
   * @param notificationId the notification the document belongs to.
   * @param attachmentId the document id.
   * @return a redirect to the signed URL.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/download")
  public ResponseEntity<Void> downloadSubmittedAttachment(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @PathVariable("notification_id") String notificationId,
      @PathVariable("attachment_id") String attachmentId) {
    Notification notification =
        notificationService
            .getNotification(notificationId, user.getUserId(), user.getProvider().getId())
            .blockOptional()
            .orElseThrow(NotificationAttachmentDownloadController::notFound);

    boolean belongsToNotification =
        Stream.of(notification.getAttachedDocuments(), notification.getUploadedDocuments())
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(Document::getDocumentId)
            .anyMatch(attachmentId::equals);
    if (!belongsToNotification) {
      throw notFound();
    }
    return redirectTo(notificationService.getDocumentLink(attachmentId));
  }

  /**
   * Redirects to a draft attachment of a notification.
   *
   * @param user the logged-in user.
   * @param notificationId the notification the draft belongs to.
   * @param attachmentId the draft attachment id.
   * @return a redirect to the signed URL.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/downloadDraft")
  public ResponseEntity<Void> downloadDraftAttachment(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @PathVariable("notification_id") String notificationId,
      @PathVariable("attachment_id") Integer attachmentId) {
    boolean belongsToNotification =
        notificationService
            .getDraftNotificationAttachments(notificationId, user.getUserId())
            .blockOptional()
            .map(NotificationAttachmentDetails::getContent)
            .stream()
            .flatMap(List::stream)
            .map(BaseNotificationAttachmentDetail::getId)
            .anyMatch(attachmentId::equals);
    if (!belongsToNotification) {
      throw notFound();
    }
    return redirectTo(notificationService.getDraftDocumentLink(String.valueOf(attachmentId)));
  }

  /**
   * Renders the error page with the exception's own status, which the global handler would
   * otherwise replace with a 200.
   *
   * @param exception the status exception thrown by a download route.
   * @param session the session, for the signed-in user shown on the error page.
   * @param response the response to set the status on.
   * @param model the model for the error page.
   * @return the error view.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public String handleStatus(
      ResponseStatusException exception,
      HttpSession session,
      HttpServletResponse response,
      Model model) {
    response.setStatus(exception.getStatusCode().value());
    model.addAttribute(USER_DETAILS, session.getAttribute(USER_DETAILS));
    model.addAttribute(
        "error", HttpStatus.valueOf(exception.getStatusCode().value()).getReasonPhrase());
    model.addAttribute("errorTime", System.currentTimeMillis());
    return "error";
  }

  private static ResponseEntity<Void> redirectTo(Optional<String> signedUrl) {
    String target = signedUrl.orElseThrow(NotificationAttachmentDownloadController::notFound);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(target))
        .cacheControl(CacheControl.noStore())
        .build();
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
