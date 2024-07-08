package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_ATTACHMENT_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.constants.NotificationConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

/**
 * Controller for handling requests for actions and notifications.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {NOTIFICATION_SEARCH_CRITERIA, NOTIFICATIONS_SEARCH_RESULTS})
public class ActionsAndNotificationsController {

  private final LookupService lookupService;
  private final ProviderService providerService;
  private final NotificationSearchValidator notificationSearchValidator;
  private final UserService userService;
  private final NotificationConstants notificationConstants;
  private final NotificationService notificationService;

  /**
   * Provides an instance of {@link NotificationSearchCriteria} for use in the model.
   *
   * @return an instance of {@link NotificationSearchCriteria}.
   */
  @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA)
  public NotificationSearchCriteria notificationSearchCriteria() {
    return new NotificationSearchCriteria();
  }

  /**
   * Endpoint to return the user to the Notifications Search Results.
   *
   * @param notifications the Notifications search results.
   * @param user          the logged-in user
   * @param criteria      the search criteria object
   * @param model         the model
   * @return the user to the results page or the full list if the results are null.
   */
  @GetMapping("/notifications")
  public String returnToNotifications(
      @SessionAttribute(
          value = NOTIFICATIONS_SEARCH_RESULTS, required = false) Notifications notifications,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model
  ) {
    if (notifications == null) {
      model.addAttribute(USER_DETAILS, user);
      model.addAttribute(NOTIFICATION_SEARCH_CRITERIA, criteria);
      return "redirect:/notifications/search?notification_type=all";
    }
    model.addAttribute(NOTIFICATIONS_SEARCH_RESULTS, notifications);
    return "notifications/actions-and-notifications";
  }

  /**
   * Loads the Notifications Search Page and populates the dropdowns.
   *
   * @param user             current user details.
   * @param criteria         the search criteria object in the model.
   * @param notificationType the notification type
   * @param model            the model.
   * @return the notifications search view.
   */
  @GetMapping("/notifications/search")
  public String notificationsSearch(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @RequestParam(value = "notification_type", required = false) String notificationType,
      Model model) {
    if (StringUtils.hasText(notificationType)) {
      criteria.setNotificationType(notificationType.equals("all") ? "" : notificationType);
      if (notificationType.equals("all")) {
        NotificationSearchCriteria.reset(criteria);
      }
      criteria.setLoginId(user.getLoginId());
      criteria.setUserType(user.getUserType());
      criteria.setAssignedToUserId(user.getLoginId());
      model.addAttribute(NOTIFICATION_SEARCH_CRITERIA, criteria);
      return "redirect:/notifications/search-results";
    }

    populateDropdowns(user, model, criteria);
    return "notifications/actions-and-notifications-search";
  }

  /**
   * Processes the search form from the Notifications Search page.
   *
   * @param user          current user details.
   * @param criteria      the search criteria object in the model.
   * @param model         the model.
   * @param bindingResult Validation result of the search criteria form.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/notifications/search")
  public String notificationsSearch(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model, BindingResult bindingResult) {

    notificationSearchValidator.validate(criteria, bindingResult);
    if (bindingResult.hasErrors()) {
      populateDropdowns(user, model, criteria);
      return "notifications/actions-and-notifications-search";
    }

    return "redirect:/notifications/search-results";
  }

  /**
   * Get the required notification from the SOA Gateway response object.
   *
   * @param user           current user details.
   * @param criteria       the search criteria object in the model.
   * @param notifications  the notifications response from the prior call to SOA.
   * @param notificationId the ID of the notification to retrieve.
   * @param model          the model.
   * @return the notification display page or an error if not found.
   */
  @GetMapping("/notifications/{notification_id}")
  public String getNotification(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @ModelAttribute(NOTIFICATIONS_SEARCH_RESULTS) Notifications notifications,
      @PathVariable(value = "notification_id") String notificationId,
      Model model,
      HttpSession session
  ) {
    session.removeAttribute("downloadStatus");
    session.removeAttribute("file");
    Notification found = notifications.getContent()
        .stream()
        .filter(notification -> notification.getNotificationId().equals(notificationId))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Notification with id %s not found", notificationId)));
    model.addAttribute("notification", found);
    return "notifications/notification";
  }

  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/download")
  public String downloadNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(value = "file", required = false) Document file,
      @SessionAttribute(value = "downloadStatus", required = false) String downloadStatus,
      @PathVariable(value = "notification_id") String notificationId,
      @PathVariable(value = "attachment_id") String attachmentId,
      final HttpServletResponse response,
      final HttpSession session,
      final Model model) throws IOException {

    log.info("CONTROLLER THREAD: {}", Thread.currentThread().getName());

    if (downloadStatus == null) {
      CompletableFuture<Document> attachmentRequest =
          notificationService.getNotificationAttachment(attachmentId, user.getLoginId(),
              user.getUserType());

      session.setAttribute("downloadStatus", "IN_PROGRESS");
      session.removeAttribute(NOTIFICATION_ATTACHMENT_POLL_COUNT);

      attachmentRequest.thenAcceptAsync(notificationAttachment -> {
        log.info("ASYNC RESPONSE THREAD: {}", Thread.currentThread().getName());
        //session.removeAttribute("downloadStatus");
        session.setAttribute("downloadStatus", "COMPLETE");
        session.setAttribute("file", notificationAttachment);
      });
    }

    if (file != null) {
      session.removeAttribute("downloadStatus");
      return "redirect:/notifications/%s/attachments/%s/downloadComplete"
          .formatted(notificationId, attachmentId);
    }

    log.info("SESSION VALUES AFTER:\ndownloadStatus: {} {}",
        downloadStatus,
        session.getAttribute("downloadStatus"));

    model.addAttribute("attachmentId", attachmentId);
    model.addAttribute("notificationId", notificationId);

    return viewAttachment(session);
  }

  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/downloadComplete")
  public void downloadNotificationAttachment(
      @SessionAttribute(value = "file", required = false) Document file,
      final HttpSession session,
      final HttpServletResponse response,
      final Model model
  ) throws IOException {
    session.removeAttribute("file");
    session.removeAttribute("downloadStatus");

    String extension = file.getFileExtension();

    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    response.setHeader("Content-Transfer-Encoding", "binary");
    response.setHeader("Content-Disposition", "attachment; filename=\""
        + file.getDocumentId()
        + (StringUtils.hasText(extension) ? ".%s".formatted(extension) : "\""));

    InputStream is = new ByteArrayInputStream(
        Base64.getDecoder().decode(file.getFileData()));
    FileCopyUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }

  private String viewAttachment(final HttpSession session) {
    int notificationAttachmentPollCount = 0;

    if (session.getAttribute(NOTIFICATION_ATTACHMENT_POLL_COUNT) != null) {
      notificationAttachmentPollCount = (int) session
          .getAttribute(NOTIFICATION_ATTACHMENT_POLL_COUNT);
      if (notificationAttachmentPollCount >= notificationConstants.getMaxPollCount()) {
        return "redirect:/notifications/attachment/failed";
      }
    }
    notificationAttachmentPollCount = notificationAttachmentPollCount + 1;
    session.setAttribute(NOTIFICATION_ATTACHMENT_POLL_COUNT, notificationAttachmentPollCount);
    return "notifications/attachment/downloadInProgress";
  }

  private void populateDropdowns(UserDetail user, Model model,
      NotificationSearchCriteria criteria) {
    Mono<List<ContactDetail>> feeEarners = providerService.getProvider(user.getProvider().getId())
        .map(providerService::getAllFeeEarners);
    // get the notification types
    Mono<CommonLookupDetail> notificationTypes =
        lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE);
    // get the Users
    Mono<UserDetails> users = userService.getUsers(user.getProvider().getId());

    // Zip all Monos and populate the model once all results are available
    Mono.zip(feeEarners, notificationTypes, users)
        .doOnNext(tuple -> {
          model.addAttribute("feeEarners", tuple.getT1());
          model.addAttribute("notificationTypes", tuple.getT2().getContent());
          model.addAttribute("users", tuple.getT3().getContent());
        })
        .block();
    model.addAttribute("notificationSearchCriteria", criteria);

  }

}
