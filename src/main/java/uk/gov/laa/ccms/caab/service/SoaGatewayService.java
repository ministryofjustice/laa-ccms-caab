package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.UserResponse;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoaGatewayService {

    private final WebClient soaGatewayWebClient;

    private final SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler;

    public Mono<NotificationSummary> getNotificationsSummary(String loginId){

        return soaGatewayWebClient
                .get()
                .uri("/users/{loginId}/notifications/summary", loginId)
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", "EXTERNAL")
                .retrieve()
                .bodyToMono(NotificationSummary.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleNotificationSummaryError(loginId, e));
    }

}