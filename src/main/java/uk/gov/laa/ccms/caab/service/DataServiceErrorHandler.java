package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.UserResponse;

@Slf4j
@Component
public class DataServiceErrorHandler {

    public static String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";

    public Mono<UserResponse> handleUserError(String loginId, Throwable e) {
        log.error("Failed to retrieve User with loginId: {}", loginId, e);
        return Mono.error(new DataServiceException(
                String.format(USER_ERROR_MESSAGE, loginId), e));
    }
}
