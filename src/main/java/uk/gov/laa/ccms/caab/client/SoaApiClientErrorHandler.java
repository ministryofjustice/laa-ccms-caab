package uk.gov.laa.ccms.caab.client;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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
   * @param copyCaseSearchCriteria Criteria for copying the case.
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<CaseDetails> handleCaseDetailsError(
          CopyCaseSearchCriteria copyCaseSearchCriteria, Throwable e) {
    log.error("Failed to retrieve CaseDetails for "
                    + "caseReferenceNumber: {}, "
                    + "providerCaseReference: {}, "
                    + "caseStatus: {}, "
                    + "feeEarnerId: {}, "
                    + "officeId: {}, "
                    + "clientSurname: {}",
            copyCaseSearchCriteria.getCaseReference(),
            copyCaseSearchCriteria.getProviderCaseReference(),
            copyCaseSearchCriteria.getActualStatus(),
            copyCaseSearchCriteria.getFeeEarnerId(),
            copyCaseSearchCriteria.getOfficeId(),
            copyCaseSearchCriteria.getClientSurname(), e);
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
   * Handles errors that occur while fetching CaseReferenceSummary.
   *
   * @param e Exception thrown during operation.
   * @return An empty Mono.
   */
  public Mono<CaseReferenceSummary> handleCaseReferenceError(Throwable e) {
    log.error("Failed to retrieve CaseReferenceSummary", e);
    return Mono.empty();
  }


}
