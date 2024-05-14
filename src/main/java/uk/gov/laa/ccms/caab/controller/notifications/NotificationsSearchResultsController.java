package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_DIRECTION;
import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_FIELD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
 * Controller for handling redirects for Notifications and Actions searches.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {NOTIFICATION_SEARCH_CRITERIA, USER_DETAILS,
    NOTIFICATIONS_SEARCH_RESULTS})
public class NotificationsSearchResultsController {

  private final NotificationService notificationService;


  /**
   * Displays the search results of notifications.
   *
   * @param page     Page number for pagination.
   * @param size     Size of results per page.
   * @param pageSort Sort criteria for the search.
   * @param criteria Criteria used for the search.
   * @param user     The logged-in user.
   * @param request  The HTTP request.
   * @param model    Model to store attributes for the view.
   * @return The appropriate view based on the search results.
   */
  @GetMapping("/notifications/search-results")
  public String notificationsSearchResults(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "pageSort", defaultValue = "assignDate,asc") String pageSort,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      HttpServletRequest request,
      Model model) {
    if (StringUtils.hasText(criteria.getAssignedToUserId())) {
      criteria.setAssignedToUserId(user.getLoginId());
    }
    if (!pageSort.equals(criteria.getSort())) {
      // redirect to first page when sort criteria changes
      page = 0;
    }
    criteria.setSort(pageSort);
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

    String currentUrl = request.getRequestURL().toString();
    model.addAttribute("currentUrl", currentUrl);
    String[] sortCriteria = pageSort.split(",");
    model.addAttribute(SORT_FIELD, sortCriteria[0]);
    model.addAttribute(SORT_DIRECTION, sortCriteria[1]);
    model.addAttribute(NOTIFICATIONS_SEARCH_RESULTS, notificationsResponse);
    return "notifications/actions-and-notifications";
  }

}
