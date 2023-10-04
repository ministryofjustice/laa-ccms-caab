package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
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
public class ActionsAndNotificationsController {


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
   * @param user Current user details.
   * @param model Model to pass attributes to the view.
   * @return Path to the view.
   */
  @GetMapping("/notifications")
  public String getNotifications(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(defaultValue = "assignDate,asc") String sort,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      Model model) {

    log.info("GET /notifications");

    // first time the criteria has been set?
    // set the basic attributes for a user search
    if (!criteria.isInstantiated()) {
      criteria.setInstantiated(true);
      criteria.setLoginId(user.getLoginId());
      criteria.setUserType(user.getUserType());
      criteria.setAssignedToUserId(user.getLoginId());
    }
    // Get the sort parameters

    criteria.setSort(sort);



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
    String[] params = sort.split(",");
    String sortField = params[0];
    String sortDirection = params[1];

    model.addAttribute("notifications", notificationsResponse);
    model.addAttribute("sortField", sortField);
    model.addAttribute("sortDirection", sortDirection);
    model.addAttribute("reverseSortDirection",
        sortDirection.equals("asc") ? "desc" : "asc");
    return "notifications/actions-and-notifications";
  }

}
