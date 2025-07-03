package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationAttachmentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationResponseValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.SendBy;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.Notification;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.Document;

/** Controller for handling requests for actions and notifications. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {NOTIFICATION_SEARCH_CRITERIA, NOTIFICATIONS_SEARCH_RESULTS})
public class ActionsAndNotificationsController {

  public static final String NOTIFICATION = "notification";
  public static final String NOTIFICATION_ID = "notification_id";
  public static final String ATTACHMENT_ID = "attachment_id";
  private final LookupService lookupService;
  private final ProviderService providerService;
  private final NotificationSearchValidator notificationSearchValidator;
  private final NotificationAttachmentMapper notificationAttachmentMapper;
  private final UserService userService;
  private final NotificationService notificationService;
  private final NotificationAttachmentUploadValidator attachmentUploadValidator;
  private final NotificationResponseValidator notificationResponseValidator;
  private final AvScanService avScanService;

  public static final String STATUS_READY_TO_SUBMIT = "Ready to Submit";

  /**
   * Provides an instance of {@link NotificationSearchCriteria} for use in the model.
   *
   * @return an instance of {@link NotificationSearchCriteria}.
   */
  @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA)
  public NotificationSearchCriteria notificationSearchCriteria() {
    return new NotificationSearchCriteria();
  }

  @InitBinder(NOTIFICATION_SEARCH_CRITERIA)
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(notificationSearchValidator);
  }

  /**
   * Endpoint to return the user to the Notifications Search Results.
   *
   * @param notifications the Notifications search results.
   * @param user the logged-in user
   * @param criteria the search criteria object
   * @param model the model
   * @return the user to the results page or the full list if the results are null.
   */
  @GetMapping("/notifications")
  public String returnToNotifications(
      @SessionAttribute(value = NOTIFICATIONS_SEARCH_RESULTS, required = false)
          Notifications notifications,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {
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
   * @param user current user details.
   * @param criteria the search criteria object in the model.
   * @param notificationType the notification type
   * @param model the model.
   * @return the notifications search view.
   */
  @GetMapping("/notifications/search")
  public String notificationsSearch(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @RequestParam(value = "notification_type", required = false) String notificationType,
      Model model) {
    if (StringUtils.hasText(notificationType)) {
      criteria.setNotificationType("all".equals(notificationType) ? "" : notificationType);
      if ("all".equals(notificationType)) {
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

  @GetMapping("/notifications/case-search")
  public String notificationsCase(
      @SessionAttribute(CASE) ApplicationDetail ebsCase,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {

      // For notifications
      // TODO: Check this is the same in old PUI
      criteria.setNotificationType("N");

      criteria.setLoginId(user.getLoginId());
      criteria.setUserType(user.getUserType());
      criteria.setAssignedToUserId(user.getLoginId());
      criteria.setOriginatesFromCase(true);
      criteria.setCaseReference(ebsCase.getCaseReferenceNumber());
      model.addAttribute(NOTIFICATION_SEARCH_CRITERIA, criteria);
      return "redirect:/notifications/search-results";
  }



  /**
   * Processes the search form from the Notifications Search page.
   *
   * @param user current user details.
   * @param criteria the search criteria object in the model.
   * @param model the model.
   * @param bindingResult Validation result of the search criteria form.
   * @return Either redirects to the search results or reloads the form with validation errors.
   */
  @PostMapping("/notifications/search")
  public String notificationsSearch(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @Validated @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      BindingResult bindingResult,
      Model model) {

    if (bindingResult.hasErrors()) {
      populateDropdowns(user, model, criteria);
      return "notifications/actions-and-notifications-search";
    }

    return "redirect:/notifications/search-results";
  }

  /**
   * Get the required notification from the SOA Gateway response object.
   *
   * @param user current user details.
   * @param criteria the search criteria object in the model.
   * @param notifications the notifications response from the prior call to SOA.
   * @param notificationId the ID of the notification to retrieve.
   * @param model the model.
   * @param session the session.
   * @return the notification display page or an error if not found.
   */
  @GetMapping("/notifications/{notification_id}")
  public String getNotification(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @ModelAttribute(NOTIFICATIONS_SEARCH_RESULTS) Notifications notifications,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      Model model,
      HttpSession session) {

    Notification notification =
        notificationService
            .getNotification(notificationId, user.getUserId(), user.getProvider().getId())
            .blockOptional()
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Notification with id %s not found".formatted(notificationId)));

    session.setAttribute(NOTIFICATION_ID, notificationId);

    return prepareNotificationPageModel(
        notification, new NotificationResponseFormData(), model, session);
  }

  /**
   * Submit a response to a notification.
   *
   * @param user current user details.
   * @param notification the current notification.
   * @param notificationId the ID of the notification to submit a response for.
   * @param model the model.
   * @param session the session.
   * @return the notification display page or an error if not found.
   */
  @PostMapping("/notifications/{notification_id}")
  public String submitNotificationResponse(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(NOTIFICATION) Notification notification,
      @ModelAttribute(value = "notificationResponseFormData")
          NotificationResponseFormData notificationResponseFormData,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    notificationResponseValidator.validate(notificationResponseFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      prepareNotificationPageModel(notification, notificationResponseFormData, model, session);
      return "notifications/notification";
    }

    notificationService
        .submitNotificationResponse(
            notificationId,
            notificationResponseFormData.getAction(),
            notificationResponseFormData.getMessage(),
            user.getLoginId(),
            user.getUserType())
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to submit notification response"));

    return prepareNotificationPageModel(
        notification, new NotificationResponseFormData(), model, session);
  }

  private String prepareNotificationPageModel(
      Notification notification,
      NotificationResponseFormData notificationResponseFormData,
      Model model,
      HttpSession session) {

    Map<String, String> documentLinks =
        notificationService.getDocumentLinks(notification.getAttachedDocuments());

    model.addAttribute("notificationResponseFormData", notificationResponseFormData);
    model.addAttribute("documentLinks", documentLinks);
    model.addAttribute(NOTIFICATION, notification);
    session.setAttribute(NOTIFICATION, notification);
    return "notifications/notification";
  }

  /**
   * If the notification attachment does not exist in S3, retrieve it from EBS then upload to S3.
   *
   * @param user current user details.
   * @param notificationId the ID of the notification of which the attachment belongs to.
   * @param attachmentId the ID of the notification attachment to retrieve.
   * @return the notification page.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/retrieve")
  public String retrieveNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @PathVariable(ATTACHMENT_ID) String attachmentId,
      HttpServletRequest request) {

    String redirectUrl = "redirect:/notifications/%s".formatted(notificationId);
    String origin = request.getHeader(HttpHeaders.REFERER);

    if (origin != null && origin.contains("provide-documents-or-evidence")) {
      redirectUrl += "/provide-documents-or-evidence";
    }

    notificationService.retrieveNotificationAttachment(
        attachmentId, user.getLoginId(), user.getUserType());

    return redirectUrl;
  }

  /**
   * If the notification attachment does not exist in S3, retrieve it from TDS then upload to S3.
   *
   * @param user current user details.
   * @param notificationId the ID of the notification of which the attachment belongs to.
   * @param attachmentId the ID of the notification attachment to retrieve.
   * @return the notification page.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/retrieveDraft")
  public String retrieveDraftNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @PathVariable(ATTACHMENT_ID) Integer attachmentId) {

    notificationService.retrieveDraftNotificationAttachment(
        String.valueOf(attachmentId), user.getLoginId(), user.getUserType());

    return "redirect:/notifications/%s/provide-documents-or-evidence".formatted(notificationId);
  }

  /**
   * If the cover sheet does not exist in S3, retrieve it from EBS then upload to S3.
   *
   * @param user current user details.
   * @param notificationId the ID of the notification of which the attachment belongs to.
   * @param attachmentId the ID of the notification attachment to retrieve.
   * @return the notification page.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/retrieveCoverSheet")
  public String retrieveCoverSheet(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @PathVariable(ATTACHMENT_ID) Integer attachmentId) {

    notificationService.retrieveCoverSheet(
        String.valueOf(attachmentId), user.getLoginId(), user.getUserType());

    return "redirect:/notifications/%s/provide-documents-or-evidence".formatted(notificationId);
  }

  /**
   * Remove a draft notification attachment from TDS and S3.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @param attachmentId the ID of the notification attachment to remove.
   * @return the provide documents or evidence page.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/remove")
  public String removeDraftNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @PathVariable(ATTACHMENT_ID) Integer attachmentId) {

    notificationService.removeDraftNotificationAttachment(
        notificationId, attachmentId, user.getLoginId(), user.getUserId());
    return "redirect:/notifications/%s/provide-documents-or-evidence".formatted(notificationId);
  }

  /**
   * Display the edit notification attachment screen.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @param attachmentId the ID of the notification attachment.
   * @return the edit notification attachment screen.
   */
  @GetMapping("/notifications/{notification_id}/attachments/{attachment_id}/edit")
  public String editDraftNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @PathVariable(ATTACHMENT_ID) Integer attachmentId,
      RedirectAttributes redirectAttributes) {

    NotificationAttachmentDetail notificationAttachment =
        notificationService.getDraftNotificationAttachment(attachmentId).block();

    NotificationAttachmentUploadFormData formData =
        notificationAttachmentMapper.toNotificationAttachmentUploadFormData(notificationAttachment);

    redirectAttributes.addFlashAttribute(formData);

    return "redirect:/notifications/%s/attachments/upload?sendBy=%s"
        .formatted(notificationId, formData.getSendBy());
  }

  /**
   * Display the provide documents or evidence screen, with all uploaded and draft notification
   * attachments.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @param notification the notification object.
   * @param model the view model.
   * @return the provide documents or evidence page.
   */
  @GetMapping("/notifications/{notification_id}/provide-documents-or-evidence")
  public String provideDocumentsOrEvidence(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @SessionAttribute(NOTIFICATION) Notification notification,
      Model model) {

    populateModelWithNotificationAttachmentDetails(user, notificationId, notification, model);

    return "notifications/provide-documents-or-evidence";
  }

  /**
   * Submit all draft notification attachments to EBS.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @return the provide documents or evidence page.
   */
  @PostMapping("/notifications/{notification_id}/provide-documents-or-evidence")
  public String submitDraftNotificationAttachments(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @SessionAttribute(NOTIFICATION) Notification notification,
      Model model) {

    NotificationAttachmentDetails notificationAttachmentDetails =
        notificationService
            .getDraftNotificationAttachments(notificationId, user.getUserId())
            .block();

    if (notificationAttachmentDetails.getContent().isEmpty()) {
      populateModelWithNotificationAttachmentDetails(user, notificationId, notification, model);
      String errorMessage =
          "You have not provided any documents for this notification that are "
              + "ready to be submitted. Please click the relevant link to add a document then try "
              + "again.";
      model.addAttribute("errorMessage", errorMessage);
      return "notifications/provide-documents-or-evidence";
    }

    notificationService.submitNotificationAttachments(
        notificationId, user.getLoginId(), user.getUserType(), user.getUserId());

    return "redirect:/application/notification-attachments/confirmed";
  }

  /**
   * Submission confirmation for notification attachments.
   *
   * @param user the currently logged-in user.
   * @param notification the notification.
   * @return a redirect to the provide documents or evidence page.
   */
  @PostMapping("/application/notification-attachments/confirmed")
  public String notificationAttachmentsSubmitted(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(NOTIFICATION) Notification notification,
      HttpSession session) {

    String notificationId = notification.getNotificationId();
    Notification updatedNotification =
        notificationService
            .getNotification(notificationId, user.getUserId(), user.getProvider().getId())
            .blockOptional()
            .orElseThrow(
                () ->
                    new CaabApplicationException(
                        "Notification with id %s not found".formatted(notificationId)));
    session.setAttribute(NOTIFICATION, updatedNotification);

    return "redirect:/notifications/%s/provide-documents-or-evidence".formatted(notificationId);
  }

  /**
   * Display the notification attachment upload screen.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @param sendBy how the notification will be sent, e.g. by post or electronically.
   * @param model the view model.
   * @return the upload notification attachment page.
   */
  @GetMapping("/notifications/{notification_id}/attachments/upload")
  public String uploadNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(NOTIFICATION) Notification notification,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @RequestParam(value = "sendBy") SendBy sendBy,
      NotificationAttachmentUploadFormData attachmentUploadFormData,
      Model model) {

    populateNotificationAttachmentModel(model);

    model.addAttribute("attachmentUploadFormData", attachmentUploadFormData);
    model.addAttribute("notificationId", notificationId);
    return "notifications/upload-notification-attachment";
  }

  /**
   * Upload a notification attachment to TDS.
   *
   * @param user the currently logged-in user.
   * @param notificationId the ID of the notification.
   * @param attachmentUploadFormData the attachment upload form data object.
   * @param bindingResult validation result of the attachment upload form.
   * @param model the view model.
   * @return the provide documents or evidence page.
   */
  @PostMapping("/notifications/{notification_id}/attachments/upload")
  public String uploadNotificationAttachment(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(NOTIFICATION) Notification notification,
      @PathVariable(NOTIFICATION_ID) String notificationId,
      @ModelAttribute(value = "attachmentUploadFormData")
          NotificationAttachmentUploadFormData attachmentUploadFormData,
      BindingResult bindingResult,
      Model model) {

    attachmentUploadValidator.validate(attachmentUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateNotificationAttachmentModel(model);

      model.addAttribute("attachmentUploadFormData", attachmentUploadFormData);
      model.addAttribute("notificationId", notificationId);
      return "notifications/upload-notification-attachment";
    }

    // Carry out AV scan for electronic documents
    if (attachmentUploadFormData.getSendBy().equals(SendBy.ELECTRONIC)) {
      try {
        avScanService.performAvScan(
            null,
            null,
            null,
            null,
            attachmentUploadFormData.getFile().getOriginalFilename(),
            attachmentUploadFormData.getFile().getInputStream());
      } catch (AvVirusFoundException | AvScanException | IOException e) {
        bindingResult.rejectValue("file", "scan.failure", e.getMessage());

        populateNotificationAttachmentModel(model);

        attachmentUploadFormData.setFile(null);

        model.addAttribute("attachmentUploadFormData", attachmentUploadFormData);
        model.addAttribute("notificationId", notificationId);
        return "notifications/upload-notification-attachment";
      }
    }

    NotificationAttachmentDetail notificationAttachmentDetail =
        notificationAttachmentMapper.toNotificationAttachmentDetail(attachmentUploadFormData);

    if (notificationAttachmentDetail.getId() != null) {
      notificationService.updateDraftNotificationAttachment(
          notificationAttachmentDetail, user.getLoginId());
    } else {
      Long attachmentNumber = getNextAttachmentNumber(notification, user.getUserId());
      notificationAttachmentDetail.setStatus(STATUS_READY_TO_SUBMIT);
      notificationAttachmentDetail.setNotificationReference(notificationId);
      notificationAttachmentDetail.setNumber(attachmentNumber);
      notificationAttachmentDetail.setProviderId(String.valueOf(user.getUserId()));
      notificationService.addDraftNotificationAttachment(
          notificationAttachmentDetail, user.getLoginId());
    }

    return "redirect:/notifications/%s/provide-documents-or-evidence".formatted(notificationId);
  }

  /**
   * Get the next attachment number by incrementing the number of the attachment last added.
   *
   * @param notification the notification.
   * @param userId the ID of the currently logged-in user.
   * @return the next attachment number.
   */
  private Long getNextAttachmentNumber(Notification notification, Integer userId) {
    int numberOfUploadedDocs = notification.getUploadedDocuments().size();
    int numberOfDraftDocs =
        notificationService
            .getDraftNotificationAttachments(notification.getNotificationId(), userId)
            .map(notificationAttachmentDetails -> notificationAttachmentDetails.getContent().size())
            .blockOptional()
            .orElseThrow(
                () -> new CaabApplicationException("Failed to retrieve attachment numbers"));

    return numberOfUploadedDocs + numberOfDraftDocs + 1L;
  }

  private void populateDropdowns(
      UserDetail user, Model model, NotificationSearchCriteria criteria) {
    Mono<List<ContactDetail>> feeEarners =
        providerService
            .getProvider(user.getProvider().getId())
            .map(providerService::getAllFeeEarners);
    // get the notification types
    Mono<CommonLookupDetail> notificationTypes =
        lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE);
    // get the Users
    Mono<UserDetails> users = userService.getUsers(user.getProvider().getId());

    // Zip all Monos and populate the model once all results are available
    Mono.zip(feeEarners, notificationTypes, users)
        .doOnNext(
            tuple -> {
              model.addAttribute("feeEarners", tuple.getT1());
              model.addAttribute("notificationTypes", tuple.getT2().getContent());
              model.addAttribute("users", tuple.getT3().getContent());
            })
        .block();
    model.addAttribute("notificationSearchCriteria", criteria);
  }

  private void populateNotificationAttachmentModel(Model model) {
    new DropdownBuilder(model)
        .addDropdown("documentTypes", lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .build();
    model.addAttribute(
        "validExtensions", getCommaDelimitedString(attachmentUploadValidator.getValidExtensions()));
    model.addAttribute("maxFileSize", attachmentUploadValidator.getMaxFileSize());
  }

  /**
   * Populate the view model with notification attachment details and S3 links.
   *
   * @param user the logged-in user.
   * @param notificationId the ID of the notification.
   * @param notification the notification object.
   * @param model the model to populate.
   */
  private void populateModelWithNotificationAttachmentDetails(
      UserDetail user, String notificationId, Notification notification, Model model) {
    // Get document type display values
    Map<String, String> documentTypes = getDocumentTypes();

    List<BaseNotificationAttachmentDetail> uploadedNotificationAttachments =
        getSubmittedAttachments(notification, documentTypes);

    setSequences(uploadedNotificationAttachments);

    List<BaseNotificationAttachmentDetail> draftNotificationAttachments =
        notificationService
            .getDraftNotificationAttachments(notificationId, user.getUserId())
            .block()
            .getContent();

    // Combine uploaded and draft documents to display
    List<BaseNotificationAttachmentDetail> allDocuments = new ArrayList<>();
    allDocuments.addAll(uploadedNotificationAttachments);
    allDocuments.addAll(draftNotificationAttachments);

    // <Document ID, S3 Link>
    Map<String, String> documentLinks = new HashMap<>();
    documentLinks.putAll(notificationService.getDraftDocumentLinks(draftNotificationAttachments));
    documentLinks.putAll(notificationService.getDocumentLinks(notification.getUploadedDocuments()));

    model.addAttribute(NOTIFICATION, notification);
    model.addAttribute("notificationId", notificationId);
    model.addAttribute("notificationAttachments", allDocuments);
    model.addAttribute("documentLinks", documentLinks);
  }

  /**
   * Get all notification attachments that have been submitted.
   *
   * @param notification the notification to get attachments for.
   * @param documentTypes a lookup of document types.
   * @return a list of all the notification attachments that have been submitted, mapped to {@link
   *     BaseNotificationAttachmentDetail}.
   */
  private List<BaseNotificationAttachmentDetail> getSubmittedAttachments(
      Notification notification, Map<String, String> documentTypes) {
    return notification.getUploadedDocuments().stream()
        .map(
            document ->
                new Document()
                    .documentId(document.getDocumentId())
                    .channel(document.getChannel())
                    .documentLink(document.getDocumentLink())
                    .fileData(document.getFileData())
                    .status(document.getStatus())
                    .statusDescription(document.getStatusDescription())
                    .fileExtension(document.getFileExtension())
                    .text(document.getText())
                    .documentType(document.getDocumentType()))
        .map(
            document ->
                notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
                    document, documentTypes.get(document.getDocumentType())))
        .map(notificationAttachment -> notificationAttachment.status("Submitted"))
        .toList();
  }

  /**
   * Fetch document types mapped to code / display value pairs.
   *
   * @return document types mapped to code / display value pairs.
   */
  private Map<String, String> getDocumentTypes() {
    return lookupService
        .getCommonValues(COMMON_VALUE_DOCUMENT_TYPES)
        .map(CommonLookupDetail::getContent)
        .blockOptional()
        .map(
            lookupDetails ->
                lookupDetails.stream()
                    .collect(
                        Collectors.toMap(
                            CommonLookupValueDetail::getCode,
                            CommonLookupValueDetail::getDescription)))
        .orElseThrow(
            () -> new CaabApplicationException("Failed to retrieve notification type lookup"));
  }

  /**
   * Set sequences on the given list of notification attachments, starting from 1 and incrementing.
   *
   * @param notificationAttachments the list of notification attachments.
   */
  private void setSequences(List<BaseNotificationAttachmentDetail> notificationAttachments) {
    long number = 0L;
    for (BaseNotificationAttachmentDetail baseNotificationAttachmentDetail :
        notificationAttachments) {
      baseNotificationAttachmentDetail.number(++number);
    }
  }
}
