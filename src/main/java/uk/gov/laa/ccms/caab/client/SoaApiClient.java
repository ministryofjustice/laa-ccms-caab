package uk.gov.laa.ccms.caab.client;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.CoverSheet;
import uk.gov.laa.ccms.soa.gateway.model.Document;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetails;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestResponse;
import uk.gov.laa.ccms.soa.gateway.model.UserOptions;

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
   * Fetches the contract details for the given criteria.
   *
   * @param providerFirmId The identifier for the provider firm.
   * @param officeId The identifier for the office.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
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
        .uri(builder -> builder.path("/contract-details").queryParams(queryParams).build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(ContractDetails.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiRetrieveError(
                    e, "Contract details", queryParams));
  }

  /**
   * Fetches detailed client information based on a given client reference number.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientDetail.
   */
  public Mono<ClientDetail> getClient(
      final String clientReferenceNumber, final String loginId, final String userType) {
    return soaApiWebClient
        .get()
        .uri("/clients/{clientReferenceNumber}", clientReferenceNumber)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(ClientDetail.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiRetrieveError(
                    e, "client", "reference number", clientReferenceNumber));
  }

  /**
   * Creates a client based on a given client details.
   *
   * @param clientDetails The client's details.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientCreated transaction id.
   */
  public Mono<ClientTransactionResponse> postClient(
      final ClientDetailDetails clientDetails, final String loginId, final String userType) {
    return soaApiWebClient
        .post()
        .uri("/clients")
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(clientDetails)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiCreateError(e, "Client"));
  }

  /**
   * Updates a client based on a given client details.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param clientDetails The client's details.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
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
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiUpdateError(
                    e, "Client", "reference number", clientReferenceNumber));
  }

  /**
   * Retrieves the full detail of a single case based on the provided case reference.
   *
   * @param caseReferenceNumber The reference number for the case to fetch.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetail.
   */
  public Mono<CaseDetail> getCase(
      final String caseReferenceNumber, final String loginId, final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/cases/{case-reference}").build(caseReferenceNumber))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(CaseDetail.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiRetrieveError(
                    e, "Cases", "case reference", caseReferenceNumber));
  }

  /**
   * Creates a new case with the provided case details.
   *
   * @param loginId the login ID of the user creating the case
   * @param userType the type of user creating the case
   * @param caseDetail the details of the case to be created
   * @return a {@link Mono} emitting the {@link CaseTransactionResponse} for the created case
   */
  public Mono<CaseTransactionResponse> createCase(
      final String loginId, final String userType, final CaseDetail caseDetail) {
    return soaApiWebClient
        .post()
        .uri(builder -> builder.path("/cases").build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(caseDetail)
        .retrieve()
        .bodyToMono(CaseTransactionResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiCreateError(e, "Cases"));
  }

  /**
   * Searches and retrieves organisation details based on provided search criteria.
   *
   * @param searchCriteria The search criteria to use when fetching organisations.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @param page The page number for pagination.
   * @param size The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<OrganisationDetails> getOrganisations(
      final OrganisationSearchCriteria searchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(searchCriteria.getName()).ifPresent(name -> queryParams.add("name", name));
    Optional.ofNullable(searchCriteria.getType()).ifPresent(type -> queryParams.add("type", type));
    Optional.ofNullable(searchCriteria.getCity()).ifPresent(city -> queryParams.add("city", city));
    Optional.ofNullable(searchCriteria.getPostcode())
        .ifPresent(postcode -> queryParams.add("postcode", postcode));
    Optional.ofNullable(page).ifPresent(param -> queryParams.add("page", String.valueOf(param)));
    Optional.ofNullable(size).ifPresent(param -> queryParams.add("size", String.valueOf(param)));

    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/organisations").queryParams(queryParams).build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(OrganisationDetails.class)
        .onErrorResume(
            e -> soaApiClientErrorHandler.handleApiRetrieveError(e, "Organisations", queryParams));
  }

  /**
   * Retrieves an organisation based on provided organisation id.
   *
   * @param organisationId The organisation id.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<OrganisationDetail> getOrganisation(
      final String organisationId, final String loginId, final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/organisations/{organisation-id}").build(organisationId))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(OrganisationDetail.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiRetrieveError(
                    e, "Organisation", "id", organisationId));
  }

  /**
   * Post basic document details to register the document in EBS.
   *
   * @param document The document details to register.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientTransactionResponse with transaction id and reference number.
   */
  public Mono<ClientTransactionResponse> registerDocument(
      final Document document, final String loginId, final String userType) {
    return uploadDocument(document, null, null, loginId, userType);
  }

  /**
   * Post a complete document to EBS.
   *
   * @param document The document details to register.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientTransactionResponse with transaction id and reference number.
   */
  public Mono<ClientTransactionResponse> uploadDocument(
      final Document document,
      final String notificationId,
      final String caseReferenceNumber,
      final String loginId,
      final String userType) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(notificationId)
        .ifPresent(param -> queryParams.add("notification-reference", notificationId));
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(param -> queryParams.add("case-reference-number", caseReferenceNumber));

    return soaApiWebClient
        .post()
        .uri(builder -> builder.path("/documents").queryParams(queryParams).build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(document)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiCreateError(e, "Document"));
  }

  /**
   * Submits a provider request to the API.
   *
   * @param providerRequest the details of the provider request to be submitted
   * @param loginId the login ID of the user submitting the request
   * @param userType the role of the user submitting the request
   * @return a {@code Mono} emitting the response of the submitted provider request
   */
  public Mono<ProviderRequestResponse> submitProviderRequest(
      final ProviderRequestDetail providerRequest, final String loginId, final String userType) {

    return soaApiWebClient
        .post()
        .uri(builder -> builder.path("/provider-requests").build())
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(providerRequest)
        .retrieve()
        .bodyToMono(ProviderRequestResponse.class)
        .onErrorResume(e -> soaApiClientErrorHandler.handleApiCreateError(e, "Provider request"));
  }

  /**
   * Update an existing document registered in EBS with complete details including file content.
   *
   * @param document The document details to register.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientTransactionResponse with transaction id and reference number.
   */
  public Mono<ClientTransactionResponse> updateDocument(
      final Document document,
      final String notificationId,
      final String caseReferenceNumber,
      final String loginId,
      final String userType) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(notificationId)
        .ifPresent(param -> queryParams.add("notification-reference", notificationId));
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(param -> queryParams.add("case-reference-number", caseReferenceNumber));

    return soaApiWebClient
        .put()
        .uri(
            builder ->
                builder
                    .path("/documents/{document-id}")
                    .queryParams(queryParams)
                    .build(document.getDocumentId()))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(document)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiUpdateError(
                    e, "Document", "id", document.getDocumentId()));
  }

  /**
   * Downloads notification attachment content from EBS.
   *
   * @param documentId The document identifier for the notification attachment.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the retrieved {@link Document} with file content.
   */
  public Mono<Document> downloadDocument(
      final String documentId, final String loginId, final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/documents/{document-id}").build(documentId))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(Document.class)
        .onErrorResume(
            e -> soaApiClientErrorHandler.handleApiRetrieveError(e, "Document", "id", documentId));
  }

  /**
   * Downloads the cover sheet for a notification attachment from EBS.
   *
   * @param documentId The document identifier for the notification attachment.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping the retrieved {@link CoverSheet} with cover sheet content.
   */
  public Mono<CoverSheet> downloadCoverSheet(
      final String documentId, final String loginId, final String userType) {
    return soaApiWebClient
        .get()
        .uri(builder -> builder.path("/documents/{document-id}/cover-sheet").build(documentId))
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .retrieve()
        .bodyToMono(CoverSheet.class)
        .onErrorResume(
            e -> soaApiClientErrorHandler.handleApiRetrieveError(e, "Document", "id", documentId));
  }

  /**
   * Update user profile options in EBS.
   *
   * @param userOptions The user profile options to update.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping a {@link ClientTransactionResponse}.
   */
  public Mono<ClientTransactionResponse> updateUserOptions(
      final UserOptions userOptions, final String loginId, final String userType) {
    return soaApiWebClient
        .put()
        .uri("/users/options")
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(userOptions)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiUpdateError(
                    e, "User", "loginId", userOptions.getUserLoginId()));
  }

  /**
   * Update a notification with a response in EBS.
   *
   * @param notification The details of the notification response.
   * @param loginId The login identifier for the user.
   * @param userType Type of the user (e.g., admin, user).
   * @return A Mono wrapping a {@link ClientTransactionResponse}.
   */
  public Mono<ClientTransactionResponse> updateNotification(
      final String notificationId,
      final Notification notification,
      final String loginId,
      final String userType) {
    return soaApiWebClient
        .put()
        .uri("/notifications/{notification-id}", notificationId)
        .header(SOA_GATEWAY_USER_LOGIN_ID, loginId)
        .header(SOA_GATEWAY_USER_ROLE, userType)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(notification)
        .retrieve()
        .bodyToMono(ClientTransactionResponse.class)
        .onErrorResume(
            e ->
                soaApiClientErrorHandler.handleApiUpdateError(
                    e, "Notification", "id", notificationId));
  }
}
