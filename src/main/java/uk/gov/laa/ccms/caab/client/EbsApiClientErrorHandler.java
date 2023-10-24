package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

/**
 * ErrorHandler for the EbsApiClient, providing detailed error messages
 * and logging for various types of data retrieval operations.
 */
@Slf4j
@Component
public class EbsApiClientErrorHandler {

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
  public static String PROVIDER_ERROR_MESSAGE =
          "Failed to retrieve Provider: (id: %s)";

  /**
   * The error message for Amendment Type-related errors.
   */
  public static String AMENDMENT_TYPE_ERROR_MESSAGE =
          "Failed to retrieve Amendment Types: (applicationType: %s)";

  /**
   * The error message for Country errors.
   */
  public static String COUNTRY_ERROR_MESSAGE =
          "Failed to retrieve Countries";

  public static String USERS_ERROR_MESSAGE =
      "Failed to retrieve Users for provider: (id: %s)";

  /**
   * The error message for relationships to case.
   */
  public static String RELATIONSHIP_TO_CASE_ERROR_MESSAGE =
      "Failed to retrieve relationship to case";

  /**
   * The error message for Proceeding-related errors.
   */
  public static String PROCEEDING_ERROR_MESSAGE =
      "Failed to retrieve Proceeding: (code: %s)";

  /**
   * The error message for Scope Limitations-related errors.
   */
  public static String SCOPE_LIMITATIONS_ERROR_MESSAGE =
      "Failed to retrieve ScopeLimitationsDetails with search criteria: %s";

  /**
   * The error message for Outcome Results-related errors.
   */
  public static String OUTCOME_RESULTS_ERROR_MESSAGE =
      "Failed to retrieve OutcomeResultDetails with search criteria: "
          + "proceedingCode=%s, outcomeResult=%s";

  /**
   * The error message for Stage End-related errors.
   */
  public static String STAGE_END_ERROR_MESSAGE =
      "Failed to retrieve StageEndLookupDetails with search criteria: "
          + "proceedingCode=%s, stageEnd=%s";

  /**
   * The error message for prior authority type.
   */
  public static String PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE =
      "Failed to retrieve prior authority types with code: %s and valueRequired: %s";

  /**
   * The error message for award type.
   */
  public static String AWARD_TYPE_ERROR_MESSAGE =
      "Failed to retrieve award types with code: %s and awardType: %s";

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
    return Mono.error(new EbsApiClientException(msg, e));
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
    return Mono.error(new EbsApiClientException(msg, e));
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
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to country lookup data retrieval.
   *
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<CommonLookupDetail> handleCountryLookupError(
          Throwable e) {
    final String msg = String.format(COUNTRY_ERROR_MESSAGE);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
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
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to person or organisation to case relationship.
   *
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<RelationshipToCaseLookupDetail> handleToCaseRelationshipValuesError(Throwable e) {
    final String msg = String.format(RELATIONSHIP_TO_CASE_ERROR_MESSAGE);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to provider data retrieval.
   *
   * @param providerId the ID of the provider
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<ProviderDetail> handleProviderError(
          Integer providerId,
          Throwable e) {
    final String msg = String.format(PROVIDER_ERROR_MESSAGE, providerId);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to proceeding data retrieval.
   *
   * @param proceedingCode the code of the proceeding
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<ProceedingDetail> handleProceedingError(
      String proceedingCode,
      Throwable e) {
    final String msg = String.format(PROCEEDING_ERROR_MESSAGE, proceedingCode);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to scope limitations data retrieval.
   *
   * @param scopeLimitationDetail the scope limitation params
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<ScopeLimitationDetails> handleScopeLimitationsError(
      ScopeLimitationDetail scopeLimitationDetail,
      Throwable e) {
    final String msg = String.format(SCOPE_LIMITATIONS_ERROR_MESSAGE,
        scopeLimitationDetail.toString());
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to outcome results data retrieval.
   *
   * @param proceedingCode the proceeding code.
   * @param outcomeResult the outcome result value.
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<OutcomeResultLookupDetail> handleOutcomeResultsError(
      String proceedingCode,
      String outcomeResult,
      Throwable e) {
    final String msg = String.format(OUTCOME_RESULTS_ERROR_MESSAGE, proceedingCode, outcomeResult);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to stage end data retrieval.
   *
   * @param proceedingCode the proceeding code.
   * @param stageEnd the stage end value.
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<StageEndLookupDetail> handleStageEndError(
      String proceedingCode,
      String stageEnd,
      Throwable e) {
    final String msg = String.format(STAGE_END_ERROR_MESSAGE, proceedingCode, stageEnd);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to prior authority type retrieval.
   *
   * @param code the prior auth type code.
   * @param valueRequired the value required flag.
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<PriorAuthorityTypeDetails> handlePriorAuthorityTypeError(
      String code,
      Boolean valueRequired,
      Throwable e) {
    final String msg = String.format(PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE, code, valueRequired);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }

  /**
   * Handles errors related to award type retrieval.
   *
   * @param code the award type code.
   * @param awardType the award type value.
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception
   */
  public Mono<AwardTypeLookupDetail> handleAwardTypeError(
      String code,
      String awardType,
      Throwable e) {
    final String msg = String.format(AWARD_TYPE_ERROR_MESSAGE, code, awardType);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }


  /**
   * Handles errors related to users by provider data retrieval.
   *
   * @param providerId the ID for the logged-in user's provider
   * @param e the exception encountered
   * @return a Mono error containing the specific error message and exception.
   */
  public Mono<UserDetails> handleUsersError(final String providerId, Throwable e) {
    final String msg = String.format(USERS_ERROR_MESSAGE, providerId);
    log.error(msg, e);
    return Mono.error(new EbsApiClientException(msg, e));
  }
}
