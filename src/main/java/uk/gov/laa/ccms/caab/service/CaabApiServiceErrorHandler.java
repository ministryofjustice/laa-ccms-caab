package uk.gov.laa.ccms.caab.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CaabApiServiceErrorHandler {

    public Mono<Void> handleCreateApplicationError(Throwable e) {
        final String msg = "Failed to create application";
        log.error(msg, e);
        return Mono.error(new CaabApiServiceException(msg, e));
    }
}
