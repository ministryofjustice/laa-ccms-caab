package uk.gov.laa.ccms.caab.client;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
 * Client class responsible for interactions with the Service-Oriented Architecture (SOA) Api.
 * Provides methods to retrieve various data like notifications, contract details, client details,
 * etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoaApiClient {
  private final WebClient soaApiWebClient;

  private final SoaApiClientErrorHandler soaApiClientErrorHandler;

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType) {
    return soaApiWebClient
            .get()
            .uri("/users/{loginId}/notifications/summary", loginId)
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(NotificationSummary.class)
            .onErrorResume(e -> soaApiClientErrorHandler
                    .handleNotificationSummaryError(loginId, e));
  }

  /**
   * Fetches the contract details for the given criteria.
   *
   * @param providerFirmId   The identifier for the provider firm.
   * @param officeId         The identifier for the office.
   * @param loginId          The login identifier for the user.
   * @param userType         Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ContractDetails.
   */
  public Mono<ContractDetails> getContractDetails(Integer providerFirmId, Integer officeId,
                                                  String loginId, String userType) {
    return soaApiWebClient
            .get()
            .uri(builder -> builder.path("/contract-details")
                    .queryParam("providerFirmId", providerFirmId)
                    .queryParam("officeId", officeId)
                    .build())
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(ContractDetails.class)
            .onErrorResume(e -> soaApiClientErrorHandler
                    .handleContractDetailsError(providerFirmId, officeId, e));
  }

  /**
   * Searches and retrieves client details based on provided search criteria.
   *
   * @param clientSearchCriteria  The search criteria to use when fetching clients.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @param page                  The page number for pagination.
   * @param size                  The size or number of records per page.
   * @return A Mono wrapping the ClientDetails.
   */
  public Mono<ClientDetails> getClients(
          ClientSearchCriteria clientSearchCriteria,
          String loginId,
          String userType,
          Integer page,
          Integer size) {
    return soaApiWebClient
            .get()
            .uri(builder -> builder.path("/clients")
                    .queryParamIfPresent("first-name",
                            Optional.ofNullable(clientSearchCriteria.getForename()))
                    .queryParamIfPresent("surname",
                            Optional.ofNullable(clientSearchCriteria.getSurname()))
                    .queryParamIfPresent("date-of-birth",
                            Optional.ofNullable(clientSearchCriteria.getDateOfBirth()))
                    .queryParamIfPresent("home-office-reference",
                            Optional.ofNullable(clientSearchCriteria
                                    .getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE)))
                    .queryParamIfPresent("national-insurance_number",
                            Optional.ofNullable(clientSearchCriteria
                                    .getUniqueIdentifier(
                                            UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER)))
                    .queryParamIfPresent("case-reference-number",
                            Optional.ofNullable(clientSearchCriteria
                                    .getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER)))
                    .queryParamIfPresent("page",
                            Optional.ofNullable(page))
                    .queryParamIfPresent("size",
                            Optional.ofNullable(size))
                    .build())
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(ClientDetails.class)
            .onErrorResume(e -> soaApiClientErrorHandler
                    .handleClientDetailsError(clientSearchCriteria, e));

  }

  /**
   * Fetches detailed client information based on a given client reference number.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientDetail.
   */
  public Mono<ClientDetail> getClient(String clientReferenceNumber, String loginId,
                                      String userType) {
    return soaApiWebClient
            .get()
            .uri("/clients/{clientReferenceNumber}", clientReferenceNumber)
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(ClientDetail.class)
            .onErrorResume(e -> soaApiClientErrorHandler
                    .handleClientDetailError(clientReferenceNumber, e));

  }

  /**
   * Searches and retrieves case details based on provided search criteria.
   *
   * @param copyCaseSearchCriteria The search criteria to use when fetching cases.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @param page                   The page number for pagination.
   * @param size                   The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<CaseDetails> getCases(CopyCaseSearchCriteria copyCaseSearchCriteria, String loginId,
                                    String userType, Integer page, Integer size) {
    return soaApiWebClient
            .get()
            .uri(builder -> builder.path("/cases")
                    .queryParamIfPresent("case-reference-number", Optional.ofNullable(
                            copyCaseSearchCriteria.getCaseReference()))
                    .queryParamIfPresent("provider-case-reference", Optional.ofNullable(
                            copyCaseSearchCriteria.getProviderCaseReference()))
                    .queryParamIfPresent("case-status",
                            Optional.ofNullable(copyCaseSearchCriteria.getActualStatus()))
                    .queryParamIfPresent("fee-earner-id",
                            Optional.ofNullable(copyCaseSearchCriteria.getFeeEarnerId()))
                    .queryParamIfPresent("office-id",
                            Optional.ofNullable(copyCaseSearchCriteria.getOfficeId()))
                    .queryParamIfPresent("client-surname",
                            Optional.ofNullable(copyCaseSearchCriteria.getClientSurname()))
                    .queryParamIfPresent("page",
                            Optional.ofNullable(page))
                    .queryParamIfPresent("size",
                            Optional.ofNullable(size))
                    .build())
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(CaseDetails.class)
            .onErrorResume(e -> soaApiClientErrorHandler.handleCaseDetailsError(
                    copyCaseSearchCriteria, e));

  }

  /**
   * Fetches a summary of case references.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseReferenceSummary.
   */
  public Mono<CaseReferenceSummary> getCaseReference(String loginId,
                                                     String userType) {
    return soaApiWebClient
            .get()
            .uri("/case-reference")
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(CaseReferenceSummary.class)
            .onErrorResume(soaApiClientErrorHandler::handleCaseReferenceError);

  }

}