package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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
}
