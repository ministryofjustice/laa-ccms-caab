package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;
import uk.gov.laa.ccms.soa.gateway.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void getNotificationsSummary_returnData() {

    String loginId = "user1";
    String userType = "userType";

    NotificationSummary mockSummary = new NotificationSummary()
        .notifications(10)
        .standardActions(5)
        .overdueActions(2);

    when(soaApiClient.getNotificationsSummary(loginId, userType)).thenReturn(Mono.just(mockSummary));

    Mono<NotificationSummary> summaryMono =
        notificationService.getNotificationsSummary(loginId, userType);

    StepVerifier.create(summaryMono)
        .expectNextMatches(summary ->
            summary.getNotifications() == 10 &&
                summary.getStandardActions() == 5 &&
                summary.getOverdueActions() == 2)
        .verifyComplete();
  }

  @Test
  void getNotifications_returnsData() {


    Notifications notificationsMock = new Notifications();
    notificationsMock
        .addContentItem(
            new Notification()
                .user(new UserDetail()
                    .userLoginId("user1")
                    .userType("user1"))
                .notificationId("234")
                .notificationType("N"));
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setAssignedToUserId("user1");

    criteria.setLoginId("user1");
    criteria.setUserType("user1");
    when(soaApiClient.getNotifications(criteria,1, 10))
        .thenReturn(Mono.just(notificationsMock));
    Mono<Notifications> notificationsMono = notificationService.getNotifications(criteria,
        1, 10);

    StepVerifier.create(notificationsMono)
        .expectNextMatches(notifications ->
            notifications.getContent().get(0).getUser().getUserLoginId().equals("user1"))
        .verifyComplete();
  }
}
