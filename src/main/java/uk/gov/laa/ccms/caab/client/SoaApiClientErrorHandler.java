package uk.gov.laa.ccms.caab.client;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

/**
 * Provides error handling capabilities for the SoaApiClient.
 */
@Slf4j
@Component
public class SoaApiClientErrorHandler {

  /**
   * Handles errors that occur while fetching NotificationSummary.
   *
   * @param loginId User's login ID.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<NotificationSummary> handleNotificationSummaryError(String loginId, Throwable e) {
    log.error("Failed to retrieve Notification count for loginId: {}", loginId, e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching ContractDetails.
   *
   * @param providerFirmId ID of the provider firm.
   * @param officeId ID of the office.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<ContractDetails> handleContractDetailsError(
          Integer providerFirmId, Integer officeId, Throwable e) {
    log.error("Failed to retrieve ContractDetails for providerFirmId: {}, officeId: {}",
            providerFirmId, officeId, e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching ClientDetails.
   *
   * @param clientSearchCriteria Criteria for searching the client.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<ClientDetails> handleClientDetailsError(
          ClientSearchCriteria clientSearchCriteria, Throwable e) {
    log.error("Failed to retrieve ClientDetails for firstName: {}, surname: {}, dob: {}, "
                    + "homeOfficeReference: {}, nationalInsuranceNumber: {}, "
                    + "caseReferenceNumber: {}",
            clientSearchCriteria.getForename(),
            clientSearchCriteria.getSurname(),
            clientSearchCriteria.getDateOfBirth(),
            clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE),
            clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER),
            clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER), e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching CaseDetails.
   *
   * @param caseSearchCriteria Criteria for copying the case.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<CaseDetails> handleCaseDetailsError(
          CaseSearchCriteria caseSearchCriteria, Throwable e) {
    log.error("Failed to retrieve CaseDetails for "
                    + "caseReferenceNumber: {}, "
                    + "providerCaseReference: {}, "
                    + "caseStatus: {}, "
                    + "feeEarnerId: {}, "
                    + "officeId: {}, "
                    + "clientSurname: {}",
            caseSearchCriteria.getCaseReference(),
            caseSearchCriteria.getProviderCaseReference(),
            caseSearchCriteria.getStatus(),
            caseSearchCriteria.getFeeEarnerId(),
            caseSearchCriteria.getOfficeId(),
            caseSearchCriteria.getClientSurname(), e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching a CaseDetail.
   *
   * @param caseReferenceNumber The requested caseReferenceNumber.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<CaseDetail> handleCaseDetailError(
      String caseReferenceNumber, Throwable e) {
    log.error("Failed to retrieve CaseDetail for "
            + "caseReferenceNumber: {}",
        caseReferenceNumber, e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching ClientDetail.
   *
   * @param clientReferenceNumber Reference number of the client.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<ClientDetail> handleClientDetailError(
          String clientReferenceNumber, Throwable e) {
    log.error("Failed to retrieve ClientDetail for clientReferenceNumber: {}",
            clientReferenceNumber,
            e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while posting a client to soa.
   *
   * @param transactionId the id for the transaction creating the client.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<ClientStatus> handleClientStatusError(String transactionId, Throwable e) {
    log.error("Failed to retrieve Client Status for: {}",
        transactionId,
        e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching Client Status.
   *
   * @param fullName client full name.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<ClientCreated> handleClientCreatedError(String fullName, Throwable e) {
    log.error("Failed to create Client: {}",
        fullName,
        e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching CaseReferenceSummary.
   *
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<CaseReferenceSummary> handleCaseReferenceError(Throwable e) {
    log.error("Failed to retrieve CaseReferenceSummary", e);
    return Mono.empty();
  }

  /**
   * Handles errors that occur while fetching Notifications.
   *
   * @param criteria the search criteria.
   * @param e Exception thrown during operation.
   * @return and empty Mono.
   */
  public Mono<Notifications> handleNotificationsError(NotificationSearchCriteria criteria,
      Throwable e) {
    log.error("Failed to retrieve Notifications for "
            + "caseReferenceNumber: {}, "
            + "providerCaseReference: {}, "
            + "assignedToUserId: {}, "
            + "clientSurname: {}"
            + "feeEarnerId: {}, "
            + "includeClosed: {}, "
            + "notificationType: {}, "
            + "dateFrom: {}, "
            + "dateTo: {}",
        criteria.getCaseReference(),
        criteria.getProviderCaseReference(),
        criteria.getAssignedToUserId(),
        criteria.getClientSurname(),
        criteria.getFeeEarnerId(),
        criteria.isIncludeClosed(),
        criteria.getNotificationType(),
        criteria.getDateFrom(),
        criteria.getDateTo(), e);
    return Mono.empty();
  }


}
