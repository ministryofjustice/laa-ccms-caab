package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
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
      @ModelAttribute(USER_DETAILS) UserDetail user,
      Model model) {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setLoginId(user.getLoginId());
    criteria.setUserType(user.getUserType());
    //temporarily set the assigned to attribute until search notifications is implemented
    criteria.setAssignedToUserId(user.getLoginId());


    // Retrieve the Notifications and actions based on the search criteria
    Notifications notificationsResponse =
        notificationService
            .getNotifications(criteria, page, size)
            .block();
    List<Notification> notifications = Optional.ofNullable(
            Objects.requireNonNull(notificationsResponse).getContent())
        .orElse(Collections.emptyList());
    if (notifications.isEmpty()) {
      return "notifications/actions-and-notifications-no-results";
    }

    model.addAttribute("notifications", notificationsResponse);
    return "notifications/actions-and-notifications";
  }

}
