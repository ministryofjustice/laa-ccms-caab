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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.util.PaginationRequest;
import uk.gov.laa.ccms.caab.util.PaginationRequestUtil;
import uk.gov.laa.ccms.data.model.NotificationInfo;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for handling redirects for Notifications and Actions searches. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(
    value = {NOTIFICATION_SEARCH_CRITERIA, USER_DETAILS, NOTIFICATIONS_SEARCH_RESULTS})
public class NotificationsSearchResultsController {

  private static final String DEFAULT_SORT = "dateAssigned,asc";

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

  @InitBinder(NOTIFICATION_SEARCH_CRITERIA)
  public void initNotificationCriteriaBinder(WebDataBinder binder) {
    binder.setDisallowedFields("page", "size", "sort");
  }

  /**
   * Displays the search results of notifications.
   *
   * @param page Page number for pagination.
   * @param size Size of results per page.
   * @param pageSort Sort criteria for the search.
   * @param criteria Criteria used for the search.
   * @param user The logged-in user.
   * @param request The HTTP request.
   * @param model Model to store attributes for the view.
   * @return The appropriate view based on the search results.
   */
  @GetMapping("/notifications/search-results")
  public String notificationsSearchResults(
      @RequestParam(value = "page", required = false) Integer page,
      @RequestParam(value = "size", required = false) Integer size,
      @RequestParam(value = "pageSort", required = false) String pageSort,
      @ModelAttribute(NOTIFICATION_SEARCH_CRITERIA) NotificationSearchCriteria criteria,
      @ModelAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(value = NOTIFICATIONS_SEARCH_RESULTS, required = false)
          Notifications cachedNotifications,
      HttpServletRequest request,
      Model model) {

    // Use local variables to store original criteria state before any updates
    Integer originalPage = criteria.getPage();
    Integer originalSize = criteria.getSize();
    String originalSort = criteria.getSort();

    PaginationRequest paginationRequest =
        PaginationRequestUtil.resolve(
            request, page, size, pageSort, originalPage, originalSize, originalSort, DEFAULT_SORT);
    boolean isNewPageRequest = paginationRequest.isNewPageRequest();

    if (!isNewPageRequest && originalPage != null) {
      return "redirect:/notifications/search-results?page="
          + originalPage
          + "&size="
          + (originalSize != null ? originalSize : PaginationRequestUtil.DEFAULT_SIZE)
          + "&pageSort="
          + (StringUtils.hasText(originalSort) ? originalSort : DEFAULT_SORT);
    }

    int finalPage = paginationRequest.page();
    int finalSize = paginationRequest.size();
    String finalPageSort = paginationRequest.sort();

    if (!StringUtils.hasText(criteria.getAssignedToUserId())) {
      criteria.setAssignedToUserId(user.getLoginId());
    }

    boolean isNewSort = paginationRequest.isNewSort();
    boolean isNewPage = paginationRequest.isNewPage();
    boolean isNewSize = paginationRequest.isNewSize();

    // Check if we can use cached results
    Notifications notificationsResponse;
    if (cachedNotifications != null && isNewPageRequest && !isNewSort && !isNewPage && !isNewSize) {
      notificationsResponse = cachedNotifications;
    } else {
      criteria.setSort(finalPageSort);
      criteria.setPage(finalPage);
      criteria.setSize(finalSize);
      notificationsResponse =
          notificationService
              .getNotifications(criteria, user.getProvider().getId(), finalPage, finalSize)
              .block();
    }

    List<NotificationInfo> notifications =
        Optional.ofNullable(notificationsResponse)
            .map(Notifications::getContent)
            .orElseThrow(() -> new CaabApplicationException("Error retrieving notifications"));
    if (notifications.isEmpty()) {
      return "notifications/actions-and-notifications-no-results";
    }

    String currentUrl = request.getRequestURL().toString();
    model.addAttribute("currentUrl", currentUrl);
    String[] sortCriteria = finalPageSort.split(",");
    model.addAttribute(SORT_FIELD, sortCriteria[0]);
    model.addAttribute(SORT_DIRECTION, sortCriteria[1]);
    model.addAttribute(NOTIFICATIONS_SEARCH_RESULTS, notificationsResponse);
    return "notifications/actions-and-notifications";
  }
}
