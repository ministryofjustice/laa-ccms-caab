package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.NotificationConstants.REVERSE_SORT_DIRECTION;
import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_DIRECTION;
import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_FIELD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

/**
 * Controller for handling requests for actions and notifications.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {NOTIFICATION_SEARCH_CRITERIA})
public class ActionsAndNotificationsController {

  private final NotificationService notificationService;
  private final CommonLookupService commonLookupService;
  private final ProviderService providerService;
  private final NotificationSearchValidator notificationSearchValidator;
  private final UserService userService;

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
   * Displays the actions and notifications page for the user.
   *
   * @param user  Current user details.
   * @param model Model to pass attributes to the view.
   * @return Path to the view.
   */
  @GetMapping("/notifications")
  public String getNotifications(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(value = "notification_type", required = false) String notificationType,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {

    // set the criteria
    populateCriteria(sort, notificationType, user, criteria);
    // Retrieve the Notifications and actions based on the search criteria
    Notifications notificationsResponse =
        notificationService
            .getNotifications(criteria, page, size)
            .block();
    List<Notification> notifications = Optional.ofNullable(
            notificationsResponse)
        .map(Notifications::getContent)
        .orElseThrow(() -> new CaabApplicationException("Error retrieving notifications"));
    if (notifications.isEmpty()) {
      return "notifications/actions-and-notifications-no-results";
    }
    // set the sort configuration in the model
    if (StringUtils.isNotEmpty(criteria.getSort())) {
      populateModelFromCriteria(criteria, model);
    } else {
      populateModelWithDefaultValues(model);
    }

    model.addAttribute("notifications", notificationsResponse);
    return "notifications/actions-and-notifications";
  }

  /**
   * Loads the Notifications Search Page and populates the dropdowns.
   *
   * @param user     current user details.
   * @param criteria the search criteria object in the model.
   * @param model    the model.
   * @return the notifications search view.
   */
  @GetMapping("/notifications/search")
  public String notificationsSearch(
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {

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

  private void populateDropdowns(UserDetail user, Model model,
      NotificationSearchCriteria criteria) {
    Mono<List<ContactDetail>> feeEarners = providerService.getProvider(user.getProvider().getId())
        .map(providerService::getAllFeeEarners);
    // get the notification types
    Mono<CommonLookupDetail> notificationTypes = commonLookupService.getNotificationTypes();
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
    model.addAttribute("criteria", criteria);

  }

  private static void populateModelWithDefaultValues(Model model) {
    model.addAttribute(SORT_FIELD, "assignDate");
    model.addAttribute(SORT_DIRECTION, "asc");
    model.addAttribute(REVERSE_SORT_DIRECTION, "desc");
  }

  private static void populateCriteria(String sort,
      String notificationType, UserDetail user,
      NotificationSearchCriteria criteria) {
    criteria.setLoginId(user.getLoginId());
    criteria.setUserType(user.getUserType());
    criteria.setAssignedToUserId(user.getLoginId());
    criteria.setNotificationType(notificationType);
    if (StringUtils.isNotEmpty(notificationType) && notificationType.equals("all")) {
      // do not set a notification type and reset the search criteria
      resetCriteria(criteria);
    }
    // Get the sort parameters
    if (StringUtils.isNotEmpty(sort)) {
      criteria.setSort(sort);
    }
  }

  private static void resetCriteria(NotificationSearchCriteria criteria) {
    criteria.setNotificationType("");
    criteria.setProviderCaseReference("");
    criteria.setClientSurname("");
    criteria.setFeeEarnerId(null);
    criteria.setCaseReference("");
    criteria.setNotificationToDateDay("");
    criteria.setNotificationToDateMonth("");
    criteria.setNotificationToDateYear("");
    criteria.setNotificationFromDateDay("");
    criteria.setNotificationFromDateMonth("");
    criteria.setNotificationFromDateYear("");
    criteria.setIncludeClosed(false);
    criteria.setDateFrom("");
    criteria.setDateTo("");
  }

  private static void populateModelFromCriteria(NotificationSearchCriteria criteria, Model model) {
    String sort = criteria.getSort();
    String[] params = sort.split(",");
    String sortField = params[0];
    String sortDirection = params[1];
    model.addAttribute(SORT_FIELD, sortField);
    model.addAttribute(SORT_DIRECTION, sortDirection);
    model.addAttribute(REVERSE_SORT_DIRECTION,
        sortDirection.equals("asc") ? "desc" : "asc");
  }


}
