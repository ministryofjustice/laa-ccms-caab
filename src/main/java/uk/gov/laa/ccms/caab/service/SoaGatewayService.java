package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Service
@Slf4j
public class SoaGatewayService {
    private final WebClient soaGatewayWebClient;

    private final SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler;

    public SoaGatewayService(@Qualifier("soaGatewayWebClient") WebClient soaGatewayWebClient,
        SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler) {
        this.soaGatewayWebClient = soaGatewayWebClient;
        this.soaGatewayServiceErrorHandler = soaGatewayServiceErrorHandler;
    }

    public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType){
        return soaGatewayWebClient
                .get()
                .uri("/users/{loginId}/notifications/summary", loginId)
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(NotificationSummary.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleNotificationSummaryError(loginId, e));
    }

    public Mono<Object> getContractDetails(String loginId, String userType){
        return soaGatewayWebClient
                .get()
                .uri("/users/{loginId}/notifications/summary", loginId)
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(Object.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleNotificationSummaryError(loginId, e));
    }

}