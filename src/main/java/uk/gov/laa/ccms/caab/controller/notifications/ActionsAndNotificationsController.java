package uk.gov.laa.ccms.caab.controller.notifications;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
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
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

/**
 * Controller for handling requests for actions and notifications.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {NOTIFICATION_SEARCH_CRITERIA})
public class ActionsAndNotificationsController {

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
    if (StringUtils.isNotEmpty(notificationType)) {
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
    model.addAttribute("notificationSearchCriteria", criteria);

  }

}
