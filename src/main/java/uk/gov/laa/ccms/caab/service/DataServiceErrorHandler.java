package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@Slf4j
@Component
public class DataServiceErrorHandler {

    public static String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";
    public static String COMMON_VALUES_ERROR_MESSAGE = "Failed to retrieve Common Values: (type: %s, code: %s, sort: %s)";

    public Mono<UserDetail> handleUserError(String loginId, Throwable e) {
        log.error("Failed to retrieve User with loginId: {}", loginId, e);
        return Mono.error(new DataServiceException(
                String.format(USER_ERROR_MESSAGE, loginId), e));
    }

    public Mono<CommonLookupDetail> handleCommonValuesError(String type, String code, String sort, Throwable e) {
        log.error("Failed to retrieve Common Values - (type: {}, code: {}, sort: {})", type, code, sort, e);
        return Mono.error(new DataServiceException(
                String.format(COMMON_VALUES_ERROR_MESSAGE, type, code, sort), e));
    }
}
