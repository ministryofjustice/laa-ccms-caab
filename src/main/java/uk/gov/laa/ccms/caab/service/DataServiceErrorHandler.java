package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * ErrorHandler for the DataService, providing detailed error messages
 * and logging for various types of data retrieval operations.
 */
@Slf4j
@Component
public class DataServiceErrorHandler {

  /**
   * The error message for User-related errors.
   */
  public static String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";

  /**
   * The error message for Common Values-related errors.
   */
  public static String COMMON_VALUES_ERROR_MESSAGE =
          "Failed to retrieve Common Values: (type: %s, code: %s, sort: %s)";

  /**
   * The error message for Case Status Values-related errors.
   */
  public static String CASE_STATUS_VALUES_ERROR_MESSAGE =
          "Failed to retrieve Case Status Values: (copyAllowed: %s)";

  /**
   * The error message for Fee Earners-related errors.
   */
  public static String FEE_EARNERS_ERROR_MESSAGE =
          "Failed to retrieve Fee Earners: (providerId: %s)";

  /**
   * The error message for Amendment Type-related errors.
   */
  public static String AMENDMENT_TYPE_ERROR_MESSAGE =
          "Failed to retrieve Amendment Types: (applicationType: %s)";

  /**
   * Handles errors related to user data retrieval.
   *
   * @param loginId the ID used during login
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<UserDetail> handleUserError(String loginId, Throwable e) {
    final String msg = String.format(USER_ERROR_MESSAGE, loginId);
    log.error(msg, e);
    return Mono.error(new DataServiceException(msg, e));
  }

  /**
   * Handles errors related to common values data retrieval.
   *
   * @param type the type of common value
   * @param code the code of the common value
   * @param sort the sort order for the common value
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<CommonLookupDetail> handleCommonValuesError(
          String type,
          String code,
          String sort,
          Throwable e) {
    final String msg = String.format(COMMON_VALUES_ERROR_MESSAGE, type, code, sort);
    log.error(msg, e);
    return Mono.error(new DataServiceException(msg, e));
  }

  /**
   * Handles errors related to amendment type lookup data retrieval.
   *
   * @param applicationType the application type for the amendment
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<AmendmentTypeLookupDetail> handleAmendmentTypeLookupError(
          String applicationType,
          Throwable e) {
    final String msg = String.format(AMENDMENT_TYPE_ERROR_MESSAGE, applicationType);
    log.error(msg, e);
    return Mono.error(new DataServiceException(msg, e));
  }

  /**
   * Handles errors related to case status values data retrieval.
   *
   * @param copyAllowed a flag indicating if copying is allowed
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<CaseStatusLookupDetail> handleCaseStatusValuesError(
          Boolean copyAllowed,
          Throwable e) {
    final String msg = String.format(CASE_STATUS_VALUES_ERROR_MESSAGE, copyAllowed);
    log.error(msg, e);
    return Mono.error(new DataServiceException(msg, e));
  }

  /**
   * Handles errors related to fee earners data retrieval.
   *
   * @param providerId the ID of the provider
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<FeeEarnerDetail> handleFeeEarnersError(
          Integer providerId,
          Throwable e) {
    final String msg = String.format(FEE_EARNERS_ERROR_MESSAGE, providerId);
    log.error(msg, e);
    return Mono.error(new DataServiceException(msg, e));
  }
}
