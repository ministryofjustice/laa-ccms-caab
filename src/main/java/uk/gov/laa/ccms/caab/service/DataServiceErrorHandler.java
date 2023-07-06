package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonValueListDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

@Slf4j
@Component
public class DataServiceErrorHandler {

    public static String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";
    public static String COMMON_VALUES_ERROR_MESSAGE = "Failed to retrieve Common Values";

    public Mono<UserDetails> handleUserError(String loginId, Throwable e) {
        log.error("Failed to retrieve User with loginId: {}", loginId, e);
        return Mono.error(new DataServiceException(
                String.format(USER_ERROR_MESSAGE, loginId), e));
    }

    public Mono<CommonValueListDetails> handleCommonValuesError(Throwable e) {
        log.error(COMMON_VALUES_ERROR_MESSAGE, e);
        return Mono.error(new DataServiceException(
                COMMON_VALUES_ERROR_MESSAGE, e));
    }
}
