package uk.gov.laa.ccms.caab.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@Slf4j
@Component
public class CaabApiServiceErrorHandler {

    public Mono<Void> handleCreateApplicationError(Throwable e) {
        log.error("Failed to create application", e);
        return Mono.empty();
    }
}
