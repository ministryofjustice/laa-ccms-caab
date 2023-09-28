package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

/**
 * Service class to handle Notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
  private final SoaApiClient soaApiClient;

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType) {
    return soaApiClient.getNotificationsSummary(loginId, userType);
  }

  /**
   * Search and retrieve notifications based on search criteria.
   *
   * @param searchCriteria        The criteria on which to search.
   * @param page                  The page number for pagination.
   * @param size                  The size or number of records per page.
   * @return A Mono wrapping the notifications list based on search criteria.
   */
  public Mono<Notifications> getNotifications(NotificationSearchCriteria searchCriteria,
      final Integer page, final Integer size) {
    return soaApiClient.getNotifications(searchCriteria, page, size);
  }


}
