package uk.gov.laa.ccms.caab.client;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.OrganisationSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;
import uk.gov.laa.ccms.soa.gateway.model.TransactionStatus;

/**
 * Client class responsible for interactions with the Service-Oriented Architecture (SOA) Api.
 * Provides methods to retrieve various data like notifications, contract details, client details,
 * etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoaApiClient {

  private static final String SOA_GATEWAY_USER_LOGIN_ID = "SoaGateway-User-Login-Id";
  private static final String SOA_GATEWAY_USER_ROLE = "SoaGateway-User-Role";
  private static final String CASE_REFERENCE_NUMBER = "case-reference-number";

  private final WebClient soaApiWebClient;

  private final SoaApiClientErrorHandler soaApiClientErrorHandler;

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId  The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(
      final String loginId,
      final String userType) {

    return soaApiWebClient
        .get()
        .uri("/users/{loginId}/notifications/summary", loginId)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(NotificationSummary.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Notification summary", "user login id", loginId));
  }

  /**
   * Fetches the contract details for the given criteria.
   *
   * @param providerFirmId The identifier for the provider firm.
   * @param officeId       The identifier for the office.
   * @param loginId        The login identifier for the user.
   * @param userType       Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ContractDetails.
   */
  public Mono<ContractDetails> getContractDetails(
      final Integer providerFirmId,
      final Integer officeId,
      final String loginId,
      final String userType) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(providerFirmId)
        .ifPresent(param -> queryParams.add("providerFirmId", String.valueOf(param)));
    Optional.ofNullable(officeId)
        .ifPresent(param -> queryParams.add("officeId", String.valueOf(param)));

    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/contract-details")
            .queryParams(queryParams)
            .build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(ContractDetails.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Contract details", queryParams));
  }

  /**
   * Searches and retrieves client details based on provided search criteria.
   *
   * @param clientSearchCriteria The search criteria to use when fetching clients.
   * @param loginId              The login identifier for the user.
   * @param userType             Type of the user (e.g., admin, user).
   * @param page                 The page number for pagination.
   * @param size                 The size or number of records per page.
   * @return A Mono wrapping the ClientDetails.
   */
  public Mono<ClientDetails> getClients(
      final ClientSearchCriteria clientSearchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(clientSearchCriteria.getForename())
        .ifPresent(forename -> queryParams.add("first-name", forename));
    Optional.ofNullable(clientSearchCriteria.getSurname())
        .ifPresent(surname -> queryParams.add("surname", surname));
    Optional.ofNullable(clientSearchCriteria.getDateOfBirth())
        .ifPresent(dateOfBirth -> queryParams.add("date-of-birth", dateOfBirth));
    Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(
        UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE))
        .ifPresent(homeOfficeReference -> queryParams.add(
            "home-office-reference", homeOfficeReference));
    Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(
        UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER))
        .ifPresent(nationalInsuranceNumber -> queryParams.add(
            "national-insurance_number", nationalInsuranceNumber));
    Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(
        UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER))
        .ifPresent(caseReferenceNumber -> queryParams.add(
            CASE_REFERENCE_NUMBER, caseReferenceNumber));
    Optional.ofNullable(page)
        .ifPresent(param -> queryParams.add("page", String.valueOf(param)));
    Optional.ofNullable(size)
        .ifPresent(param -> queryParams.add("size", String.valueOf(param)));

    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/clients")
            .queryParams(queryParams)
            .build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(ClientDetails.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Clients", queryParams));

  }

  /**
   * Fetches detailed client information based on a given client reference number.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientDetail.
   */
  public Mono<ClientDetail> getClient(
      final String clientReferenceNumber,
      final  String loginId,
      final String userType) {
    return soaApiWebClient
        .get()
        .uri("/clients/{clientReferenceNumber}", clientReferenceNumber)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(ClientDetail.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "client", "reference number", clientReferenceNumber));

  }

  /**
   * Fetches the transaction status for a client create transaction.
   *
   * @param transactionId         The transaction id for the client create transaction in soa.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the TransactionStatus.
   */
  public Mono<TransactionStatus> getClientStatus(
      final String transactionId,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .get()
        .uri("/clients/status/{transactionId}", transactionId)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(TransactionStatus.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "client transaction status", "transaction id", transactionId));

  }

  /**
   * Creates a client based on a given client details.
   *
   * @param clientDetails         The client's details.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientCreated transaction id.
   */
  public Mono<ClientTransactionResponse> postClient(
      final ClientDetailDetails clientDetails,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .post()
        .uri("/clients")
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(clientDetails)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiCreateError(
            e, "Client"));
  }

  /**
   * Updates a client based on a given client details.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param clientDetails         The client's details.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientCreated transaction id.
   */
  public Mono<ClientTransactionResponse> putClient(
      final String clientReferenceNumber,
      final ClientDetailDetails clientDetails,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .put()
        .uri("/clients/{clientReferenceNumber}", clientReferenceNumber)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(clientDetails)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiUpdateError(
            e, "Client", "reference number", clientReferenceNumber));
  }

  /**
   * Searches and retrieves case details based on provided search criteria.
   *
   * @param caseSearchCriteria The search criteria to use when fetching cases.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @param page                   The page number for pagination.
   * @param size                   The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<CaseDetails> getCases(
      final CaseSearchCriteria caseSearchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(caseSearchCriteria.getCaseReference())
        .ifPresent(caseReference -> queryParams.add(
            CASE_REFERENCE_NUMBER, caseReference));
    Optional.ofNullable(caseSearchCriteria.getProviderCaseReference())
        .ifPresent(providerCaseReference -> queryParams.add(
            "provider-case-reference", providerCaseReference));
    Optional.ofNullable(caseSearchCriteria.getStatus())
        .ifPresent(status -> queryParams.add(
            "case-status", status));
    Optional.ofNullable(caseSearchCriteria.getFeeEarnerId())
        .ifPresent(feeEarnerId -> queryParams.add(
            "fee-earner-id", String.valueOf(feeEarnerId)));
    Optional.ofNullable(caseSearchCriteria.getOfficeId())
        .ifPresent(officeId -> queryParams.add(
            "office-id", String.valueOf(officeId)));
    Optional.ofNullable(caseSearchCriteria.getClientSurname())
        .ifPresent(clientSurname -> queryParams.add(
            "client-surname", clientSurname));
    Optional.ofNullable(page)
        .ifPresent(param -> queryParams.add("page", String.valueOf(param)));
    Optional.ofNullable(size)
        .ifPresent(param -> queryParams.add("size", String.valueOf(param)));

    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/cases")
            .queryParams(queryParams)
            .build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(CaseDetails.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Cases", queryParams));

  }

  /**
   * Retrieves the full detail of a single case based on the provided case reference.
   *
   * @param caseReferenceNumber    The reference number for the case to fetch.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetail.
   */
  public Mono<CaseDetail> getCase(
      final String caseReferenceNumber,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/cases/{case-reference}").build(caseReferenceNumber))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(CaseDetail.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Cases", "case reference", caseReferenceNumber));

  }

  /**
   * Fetches a summary of case references.
   *
   * @param loginId  The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseReferenceSummary.
   */
  public Mono<CaseReferenceSummary> getCaseReference(
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .get()
        .uri("/case-reference")
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(CaseReferenceSummary.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "case reference", null));

  }

  /**
   * Searches and retrieves notifications based on search criteria.
   *
   * @param criteria The {@link NotificationSearchCriteria} class.
   * @param page     The page number for pagination.
   * @param size     The size or number of records per page.
   * @return A Mono wrapping the Notifications
   */
  public Mono<Notifications> getNotifications(
      final NotificationSearchCriteria criteria,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(criteria.getCaseReference())
        .ifPresent(caseReference -> queryParams.add(CASE_REFERENCE_NUMBER, caseReference));
    Optional.ofNullable(criteria.getProviderCaseReference())
        .ifPresent(providerCaseReference -> queryParams.add(
            "provider-case-reference", providerCaseReference));
    Optional.ofNullable(criteria.getAssignedToUserId())
        .ifPresent(assignedToUserId -> queryParams.add("assigned-to-user-id", assignedToUserId));
    Optional.ofNullable(criteria.getClientSurname())
        .ifPresent(clientSurname -> queryParams.add("client-surname", clientSurname));
    Optional.ofNullable(criteria.getFeeEarnerId())
        .ifPresent(feeEarnerId -> queryParams.add("fee-earner-id", String.valueOf(feeEarnerId)));
    Optional.of(criteria.isIncludeClosed())
        .ifPresent(param -> queryParams.add("include-closed", String.valueOf(param)));
    Optional.ofNullable(criteria.getNotificationType())
        .ifPresent(notificationType -> queryParams.add("notification-type", notificationType));
    Optional.ofNullable(criteria.getDateFrom())
        .ifPresent(dateFrom -> queryParams.add("date-from", dateFrom));
    Optional.ofNullable(criteria.getDateTo())
        .ifPresent(dateTo -> queryParams.add("date-to", dateTo));
    Optional.ofNullable(page)
        .ifPresent(param -> queryParams.add("page", String.valueOf(param)));
    Optional.ofNullable(size)
        .ifPresent(param -> queryParams.add("size", String.valueOf(param)));
    Optional.ofNullable(criteria.getSort())
        .ifPresent(sort -> queryParams.add("sort", sort));

    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/notifications")
            .queryParams(queryParams)
            .build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, criteria.getLoginId())
        .header(SOA_GATEWAY_USER_ROLE, criteria.getUserType())
        .retrieve()
        .bodyToMono(Notifications.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Notifications", queryParams));

  }

  /**
   * Searches and retrieves organisation details based on provided search criteria.
   *
   * @param searchCriteria         The search criteria to use when fetching organisations.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @param page                   The page number for pagination.
   * @param size                   The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<OrganisationDetails> getOrganisations(
      final OrganisationSearchCriteria searchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(searchCriteria.getName())
        .ifPresent(name -> queryParams.add("name", name));
    Optional.ofNullable(searchCriteria.getType())
        .ifPresent(type -> queryParams.add("type", type));
    Optional.ofNullable(searchCriteria.getCity())
        .ifPresent(city -> queryParams.add("city", city));
    Optional.ofNullable(searchCriteria.getPostcode())
        .ifPresent(postcode -> queryParams.add("postcode", postcode));
    Optional.ofNullable(page)
        .ifPresent(param -> queryParams.add("page", String.valueOf(param)));
    Optional.ofNullable(size)
        .ifPresent(param -> queryParams.add("size", String.valueOf(param)));


    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/organisations")
            .queryParams(queryParams)
            .build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(OrganisationDetails.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Organisations", queryParams));

  }

  /**
   * Retrieves an organisation based on provided organisation id.
   *
   * @param organisationId         The organisation id.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<OrganisationDetail> getOrganisation(
      final String organisationId,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/organisation/{organisation-id}").build(organisationId))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(OrganisationDetail.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiRetrieveError(
            e, "Organisation", "id", organisationId));
  }

}
