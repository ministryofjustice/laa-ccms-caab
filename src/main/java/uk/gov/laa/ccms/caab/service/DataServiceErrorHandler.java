package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@Slf4j
@Component
public class DataServiceErrorHandler {

    /**
     * The error message for User-related errors
     */
    public static String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";

    /**
     * The error message for Common Values-related errors
     */
    public static String COMMON_VALUES_ERROR_MESSAGE = "Failed to retrieve Common Values: (type: %s, code: %s, sort: %s)";

    /**
     * The error message for Fee Earners-related errors
     */
    public static String FEE_EARNERS_ERROR_MESSAGE = "Failed to retrieve Fee Earners: (providerId: %s)";

    /**
     * The error message for Amendment Type-related errors
     */
    public static String AMENDMENT_TYPE_ERROR_MESSAGE = "Failed to retrieve Amendment Types: (applicationType: %s)";

    public Mono<UserDetail> handleUserError(String loginId, Throwable e) {
        final String msg = String.format(USER_ERROR_MESSAGE, loginId);
        log.error(msg, e);
        return Mono.error(new DataServiceException(msg, e));
    }

    public Mono<CommonLookupDetail> handleCommonValuesError(String type, String code, String sort, Throwable e) {
        final String msg = String.format(COMMON_VALUES_ERROR_MESSAGE, type, code, sort);
        log.error(msg, e);
        return Mono.error(new DataServiceException(msg, e));
    }

    public Mono<AmendmentTypeLookupDetail> handleAmendmentTypeLookupError(String applicationType, String sort, Throwable e) {
        final String msg = String.format(AMENDMENT_TYPE_ERROR_MESSAGE, applicationType, sort);
        log.error(msg, e);
        return Mono.error(new DataServiceException(msg, e));
    }

    public Mono<FeeEarnerDetail> handleFeeEarnersError(Integer providerId, Throwable e) {
        final String msg = String.format(FEE_EARNERS_ERROR_MESSAGE, providerId);
        log.error(msg, e);
        return Mono.error(new DataServiceException(msg, e));
    }
}
