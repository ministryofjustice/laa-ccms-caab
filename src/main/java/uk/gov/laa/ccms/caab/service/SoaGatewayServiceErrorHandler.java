package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Slf4j
@Component
public class SoaGatewayServiceErrorHandler {

    public Mono<NotificationSummary> handleNotificationSummaryError(String loginId, Throwable e) {
        log.error("Failed to retrieve Notification count for loginId: {}", loginId, e);
        return Mono.empty();
    }

    public Mono<ContractDetails> handleContractDetailsError(
        Integer providerFirmId, Integer officeId, Throwable e) {
        log.error("Failed to retrieve ContractDetails for providerFirmId: {}, officeId: {}",
            providerFirmId, officeId, e);
        return Mono.empty();
    }
}