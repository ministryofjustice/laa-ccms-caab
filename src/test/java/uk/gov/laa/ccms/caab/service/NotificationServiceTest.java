package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
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
}
