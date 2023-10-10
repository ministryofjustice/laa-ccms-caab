package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.UserDetail;
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


  private static final String SORT_FIELD = "sortField";
  private static final String SORT_DIRECTION = "sortDirection";
  private static final String REVERSE_SORT_DIRECTION = "reverseSortDirection";
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
      @RequestParam(value = "notificationType", required = false) String notificationType,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {

    // set the basic attributes for a user search
    criteria.setLoginId(user.getLoginId());
    criteria.setUserType(user.getUserType());
    criteria.setAssignedToUserId(user.getLoginId());
    criteria.setNotificationType(notificationType);

    // Get the sort parameters
    if (StringUtils.isNotEmpty(sort)) {
      criteria.setSort(sort);
    }

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
      sort = criteria.getSort();
      String[] params = sort.split(",");
      String sortField = params[0];
      String sortDirection = params[1];
      model.addAttribute(SORT_FIELD, sortField);
      model.addAttribute(SORT_DIRECTION, sortDirection);
      model.addAttribute(REVERSE_SORT_DIRECTION,
          sortDirection.equals("asc") ? "desc" : "asc");
    } else {
      model.addAttribute(SORT_FIELD, "assignDate");
      model.addAttribute(SORT_DIRECTION, "asc");
      model.addAttribute(REVERSE_SORT_DIRECTION, "desc");
    }

    model.addAttribute("notifications", notificationsResponse);

    return "notifications/actions-and-notifications";
  }

}
