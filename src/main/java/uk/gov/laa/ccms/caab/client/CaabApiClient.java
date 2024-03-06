package uk.gov.laa.ccms.caab.client;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseClient;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;

/**
 * Client responsible for interactions with the CAAB API.
 */
@Service
@RequiredArgsConstructor
public class CaabApiClient {

  private final WebClient caabApiWebClient;

  private final ApiClientErrorHandler apiClientErrorHandler;

  public static final String FAILED_CREATE_APPLICATION =
      "Failed to create application";
  private static final String FAILED_RETRIEVE_APPLICATION =
      "Failed to retrieve application with id: %s";
  private static final String FAILED_UPDATE_APPLICATION =
      "Failed to update application with id: %s";
  private static final String FAILED_RETRIEVE_APPLICATIONS =
      "Failed to retrieve applications";
  private static final String FAILED_RETRIEVE_APPLICATION_TYPE =
      "Failed to retrieve application type for application id: %s";
  private static final String FAILED_RETRIEVE_PROVIDER_DETAILS =
      "Failed to retrieve provider details for application id: %s";
  private static final String FAILED_RETRIEVE_CORRESPONDENCE_ADDRESS =
      "Failed to retrieve correspondence address for application id: %s";
  private static final String FAILED_RETRIEVE_LINKED_CASES =
      "Failed to retrieve linked cases for application id: %s";
  private static final String FAILED_UPDATE_LINKED_CASE =
      "Failed to update linked case with id: %s";
  private static final String FAILED_ADD_LINKED_CASE =
      "Failed to update linked case for application id: %s";
  private static final String FAILED_DELETE_LINKED_CASE =
      "Failed to delete linked case with id: %s";
  private static final String FAILED_RETRIEVE_COSTS =
      "Failed to retrieve cost structure for application id: %s";
  private static final String FAILED_UPDATE_COSTS =
      "Failed to update cost structure for application id: %s";
  private static final String FAILED_RETRIEVE_PRIOR_AUTHORITIES =
      "Failed to retrieve prior authorities for application id: %s";
  private static final String FAILED_RETRIEVE_PROCEEDINGS =
      "Failed to retrieve proceedings for application id: %s";
  private static final String FAILED_ADD_PROCEEDINGS =
      "Failed to add proceedings to application id: %s";
  private static final String FAILED_UPDATE_PROCEEDINGS =
      "Failed to update proceeding with id: %s";
  private static final String FAILED_DELETE_PROCEEDINGS =
      "Failed to delete proceeding with id: %s";
  private static final String FAILED_RETRIEVE_SCOPE_LIMITATIONS =
      "Failed to retrieve scope limitations for proceeding id: %s";
  private static final String FAILED_UPDATE_CLIENT =
      "Failed to update client with reference - %s";
  private static final String FAILED_RETRIEVE_OPPONENTS =
      "Failed to retrieve opponents for application id: %s";
  private static final String FAILED_ADD_OPPONENTS =
      "Failed to add opponents to application id: %s";
  private static final String FAILED_ADD_PRIOR_AUTHORITY =
      "Failed to add prior authority to application: %s";
  private static final String FAILED_UPDATE_PRIOR_AUTHORITY =
      "Failed to update prior authority with id: %s";
  private static final String FAILED_DELETE_PRIOR_AUTHORITY =
      "Failed to delete prior authority with id: %s";

  /**
   * Creates an application using the CAAB API.
   *
   * @param loginId the ID associated with the user login
   * @param application the details of the application to be created
   * @return a Mono signaling the completion of the application creation
   */
  public Mono<String> createApplication(
      final String loginId,
      final ApplicationDetail application) {

    return caabApiWebClient
            .post()
            .uri("/applications")
            .header("Caab-User-Login-Id", loginId)
            .contentType(MediaType.APPLICATION_JSON) // Set the content type to JSON
            .bodyValue(application) // Add the application details to the request body
            .exchangeToMono(clientResponse -> {
              final HttpHeaders headers = clientResponse.headers().asHttpHeaders();
              final URI locationUri = headers.getLocation();
              if (locationUri != null) {
                final String path = locationUri.getPath();
                final String id = path.substring(path.lastIndexOf('/') + 1);
                return Mono.just(id);
              } else {
                // Handle the case where the Location header is missing or the URI is invalid
                return Mono.error(new RuntimeException("Location header missing or URI invalid"));
              }
            })
        .onErrorResume(e -> apiClientErrorHandler
            .handleCaabApiError(e, FAILED_CREATE_APPLICATION));
  }

  /**
   * Retrieves an application using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing applications detail
   */
  public Mono<ApplicationDetail> getApplication(
      final String id) {

    final String errorMessage = String.format(FAILED_RETRIEVE_APPLICATION, id);

    return caabApiWebClient
        .get()
        .uri("/applications/{id}", id)
        .retrieve()
        .bodyToMono(ApplicationDetail.class)
        .onErrorResume(e -> apiClientErrorHandler
            .handleCaabApiError(e, errorMessage));
  }

  /**
   * Query for applications using the CAAB API.
   *
   * @return a Mono containing applications detail
   */
  public Mono<ApplicationDetails> getApplications(
      final CaseSearchCriteria caseSearchCriteria,
      final Integer page,
      final Integer size) {

    return caabApiWebClient
        .get()
        .uri(builder -> builder.path("/applications")
            .queryParamIfPresent("case-reference-number",
                Optional.ofNullable(caseSearchCriteria.getCaseReference()))
            .queryParamIfPresent("provider-case-ref",
                Optional.ofNullable(caseSearchCriteria.getProviderCaseReference()))
            .queryParamIfPresent("client-surname",
                Optional.ofNullable(caseSearchCriteria.getClientSurname()))
            .queryParamIfPresent("client-reference",
                Optional.ofNullable(caseSearchCriteria.getClientReference()))
            .queryParamIfPresent("fee-earner",
                Optional.ofNullable(caseSearchCriteria.getFeeEarnerId()))
            .queryParamIfPresent("office-id",
                Optional.ofNullable(caseSearchCriteria.getOfficeId()))
            .queryParamIfPresent("status",
                Optional.ofNullable(caseSearchCriteria.getStatus()))
            .queryParamIfPresent("page",
                Optional.ofNullable(page))
            .queryParamIfPresent("size",
                Optional.ofNullable(size))
            .build())
        .retrieve()
        .bodyToMono(ApplicationDetails.class)
        .onErrorResume(e -> apiClientErrorHandler
            .handleCaabApiError(e, FAILED_RETRIEVE_APPLICATIONS));
  }

  /**
   * Patches an application using the CAAB API.
   *
   * @param id the ID associated with the application
   * @param loginId the ID associated with the user login
   * @param data the data to amend to the application
   * @param type the type of data being putted
   *             (examples - "application-type", "provider-details", "correspondence-address")
   * @return a Mono containing application's data
   */
  public Mono<Void> putApplication(
      final String id,
      final String loginId,
      final Object data,
      final String type) {
    final String errorMessage = String.format(FAILED_UPDATE_APPLICATION, id);
    return caabApiWebClient
        .put()
        .uri("/applications/{id}/" + type, id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Retrieves an application's application type using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's application type
   */
  public Mono<ApplicationType> getApplicationType(
      final String id) {

    final String errorMessage = String.format(FAILED_RETRIEVE_APPLICATION_TYPE, id);

    return caabApiWebClient
        .get()
        .uri("/applications/{id}/application-type", id)
        .retrieve()
        .bodyToMono(ApplicationType.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Retrieves an application's application type using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's application type
   */
  public Mono<ApplicationProviderDetails> getProviderDetails(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_PROVIDER_DETAILS, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/provider-details", id)
        .retrieve()
        .bodyToMono(ApplicationProviderDetails.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Retrieves an application's correspondence address using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's correspondence address
   */
  public Mono<Address> getCorrespondenceAddress(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_CORRESPONDENCE_ADDRESS, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/correspondence-address", id)
        .retrieve()
        .bodyToMono(Address.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Retrieves a list of linked cases associated with a given application ID.
   *
   * @param id the ID of the application for which linked cases are to be retrieved
   * @return a Mono containing a list of LinkedCase objects associated with the application
   */
  public Mono<List<LinkedCase>> getLinkedCases(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_LINKED_CASES, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/linked-cases", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<LinkedCase>>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Removes a linked case from the CAAB API for a given application.
   *
   * @param linkedCaseId the ID of the linked case to be removed
   * @param loginId the login ID of the user performing the removal
   * @return a Mono indicating completion of the removal operation
   */
  public Mono<Void> removeLinkedCase(
      final String linkedCaseId,
      final String loginId) {
    final String errorMessage = String.format(FAILED_DELETE_LINKED_CASE, linkedCaseId);
    return caabApiWebClient
        .delete()
        .uri("/linked-cases/{linkedCaseId}",
            linkedCaseId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Asynchronously adds a linked case to an application.
   *
   * @param applicationId The ID of the application to which the case is linked.
   * @param data          The linked case information.
   * @param loginId       The login ID of the user performing the operation.
   * @return A Mono that completes when the operation is done.
   */
  public Mono<Void> addLinkedCase(
      final String applicationId,
      final LinkedCase data,
      final String loginId) {
    final String errorMessage = String.format(FAILED_ADD_LINKED_CASE, applicationId);
    return caabApiWebClient
        .post()
        .uri("applications/{applicationId}/linked-cases",
            applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Updates a linked case in the CAAB API for a given application.
   *
   * @param linkedCaseId the ID of the linked case to be updated
   * @param data the new data for the linked case
   * @param loginId the login ID of the user performing the update
   * @return a Mono indicating completion of the update operation
   */
  public Mono<Void> updateLinkedCase(
      final String linkedCaseId,
      final LinkedCase data,
      final String loginId) {
    final String errorMessage = String.format(FAILED_UPDATE_LINKED_CASE, linkedCaseId);
    return caabApiWebClient
        .patch()
        .uri("/linked-cases/{linkedCaseId}",
            linkedCaseId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Fetches the cost structure associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the cost structure.
   *
   * @param id The id of the application for which the cost structure should be retrieved.
   * @return A {@code Mono<CostStructure>} containing the cost structure.
   */
  public Mono<CostStructure> getCostStructure(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_COSTS, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/cost-structure", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<CostStructure>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Updates the cost structure of an application using the CAAB API.
   *
   * @param id The ID of the application.
   * @param costs The new cost structure for the application.
   * @param loginId The ID associated with the user login.
   * @return A Mono Void indicating the completion of the update operation.
   */
  public Mono<Void> updateCostStructure(
      final String id,
      final CostStructure costs,
      final String loginId) {
    final String errorMessage = String.format(FAILED_UPDATE_COSTS, id);
    return caabApiWebClient
        .put()
        .uri("/applications/{id}/cost-structure", id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON) // Set the content type to JSON
        .bodyValue(costs)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Fetches the proceedings associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the proceedings.
   *
   * @param id The id of the application for which proceedings should be retrieved.
   * @return A {@code Mono<List<Proceeding>>} containing the proceedings.
   */
  public Mono<List<Proceeding>> getProceedings(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_PROCEEDINGS, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/proceedings", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Proceeding>>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Updates a proceeding in the CAAB API for a given application.
   *
   * @param proceedingId the ID of the proceeding to be updated
   * @param data the new data for the proceeding
   * @param loginId the login ID of the user performing the update
   * @return a Mono indicating completion of the update operation
   */
  public Mono<Void> updateProceeding(
      final Integer proceedingId,
      final Proceeding data,
      final String loginId) {
    final String errorMessage = String.format(FAILED_UPDATE_PROCEEDINGS, proceedingId);
    return caabApiWebClient
        .patch()
        .uri("/proceedings/{proceeding-id}",
            proceedingId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Adds a proceeding to an application using the CAAB API.
   *
   * @param applicationId The ID of the application.
   * @param proceeding The proceeding to be added.
   * @param loginId The ID associated with the user login.
   * @return A Mono Void indicating the completion of the add operation.
   */
  public Mono<Void> addProceeding(
      final String applicationId,
      final Proceeding proceeding,
      final String loginId) {
    final String errorMessage = String.format(FAILED_ADD_PROCEEDINGS, applicationId);
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/proceedings", applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(proceeding)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Deletes a proceeding from the CAAB API.
   *
   * @param proceedingId the ID of the proceeding to be deleted
   * @param loginId the ID associated with the user login
   * @return a Mono Void indicating the completion of the delete operation
   */
  public Mono<Void> deleteProceeding(
      final Integer proceedingId,
      final String loginId) {
    final String errorMessage = String.format(FAILED_DELETE_PROCEEDINGS, proceedingId);
    return caabApiWebClient
        .delete()
        .uri("/proceedings/{proceeding-id}",
            proceedingId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Retrieves the scope limitations associated with a specific proceeding id.
   * This method communicates with the CAAB API client to fetch the scope limitations.
   *
   * @param proceedingId The id of the proceeding for which scope limitations should be retrieved.
   * @return A {@code Mono<List<ScopeLimitation>>} containing the scope limitations.
   * @throws RuntimeException if an error occurs during the retrieval operation
   */
  public Mono<List<ScopeLimitation>> getScopeLimitations(
      final Integer proceedingId) {
    final String errorMessage = String.format(FAILED_RETRIEVE_SCOPE_LIMITATIONS, proceedingId);
    return caabApiWebClient
        .get()
        .uri("/proceedings/{id}/scope-limitations", proceedingId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<ScopeLimitation>>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Patches all application clients with the same reference id using the CAAB API.
   *
   * @param clientReferenceId the ID associated with the client
   * @param loginId the ID associated with the user login
   * @param data the client data to amend to the application
   * @return a Mono void
   */
  public Mono<Void> updateClient(
      final String clientReferenceId,
      final String loginId,
      final BaseClient data) {
    final String errorMessage = String.format(FAILED_UPDATE_CLIENT, clientReferenceId);
    return caabApiWebClient
        .patch()
        .uri("/applications/clients/{clientReferenceId}", clientReferenceId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Fetches the opponents associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the opponents.
   *
   * @param id The id of the application for which opponents should be retrieved.
   * @return A {@code Mono<List<Opponent>>} containing the opponents.
   */
  public Mono<List<Opponent>> getOpponents(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_OPPONENTS, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/opponents", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Opponent>>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Asynchronously adds an opponent to an application.
   *
   * @param applicationId The ID of the related application.
   * @param data          The opponent information.
   * @param loginId       The login ID of the user performing the operation.
   * @return A Mono that completes when the operation is done.
   */
  public Mono<Void> addOpponent(
      final String applicationId,
      final Opponent data,
      final String loginId) {
    final String errorMessage = String.format(FAILED_ADD_OPPONENTS, applicationId);
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/opponents",
            applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Fetches the prior authorities associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the prior authorities.
   *
   * @param id The id of the application for which prior authorities should be retrieved.
   * @return A {@code Mono<List<PriorAuthority>>} containing the prior authorities.
   */
  public Mono<List<PriorAuthority>> getPriorAuthorities(
      final String id) {
    final String errorMessage = String.format(FAILED_RETRIEVE_PRIOR_AUTHORITIES, id);
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/prior-authorities", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<PriorAuthority>>() {})
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Adds a priorAuthority to an application using the CAAB API.
   *
   * @param applicationId The ID of the application.
   * @param priorAuthority The priorAuthority to be added.
   * @param loginId The ID associated with the user login.
   * @return A Mono Void indicating the completion of the add operation.
   */
  public Mono<Void> addPriorAuthority(
      final String applicationId,
      final PriorAuthority priorAuthority,
      final String loginId) {
    final String errorMessage = String.format(FAILED_ADD_PRIOR_AUTHORITY, applicationId);
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/prior-authorities", applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(priorAuthority)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Updates a prior authority with new data.
   *
   * @param priorAuthorityId the ID of the prior authority to update.
   * @param data the new data for the prior authority.
   * @param loginId the login ID of the user performing the update.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> updatePriorAuthority(
      final Integer priorAuthorityId,
      final PriorAuthority data,
      final String loginId) {
    final String errorMessage = String.format(FAILED_UPDATE_PRIOR_AUTHORITY, priorAuthorityId);
    return caabApiWebClient
        .patch()
        .uri("/prior-authorities/{prior-authority-id}",
            priorAuthorityId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }

  /**
   * Deletes a specific prior authority.
   *
   * @param priorAuthorityId the ID of the prior authority to delete.
   * @param loginId the login ID of the user performing the deletion.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deletePriorAuthority(
      final Integer priorAuthorityId,
      final String loginId) {
    final String errorMessage = String.format(FAILED_DELETE_PRIOR_AUTHORITY, priorAuthorityId);
    return caabApiWebClient
        .delete()
        .uri("/prior-authorities/{prior-authority-id}",
            priorAuthorityId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> apiClientErrorHandler.handleCaabApiError(e, errorMessage));
  }


  /**
   * Adds a priorAuthority to an application using the CAAB API.
   *
   * @param applicationId The ID of the application.
   * @param priorAuthority The priorAuthority to be added.
   * @param loginId The ID associated with the user login.
   * @return A Mono Void indicating the completion of the add operation.
   */
  public Mono<Void> addPriorAuthority(
      final String applicationId,
      final PriorAuthority priorAuthority,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/prior-authorities", applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(priorAuthority)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleSavePriorAuthorityError(
            e, applicationId));
  }

  /**
   * Updates a prior authority with new data.
   *
   * @param priorAuthorityId the ID of the prior authority to update.
   * @param data the new data for the prior authority.
   * @param loginId the login ID of the user performing the update.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> updatePriorAuthority(
      final Integer priorAuthorityId,
      final PriorAuthority data,
      final String loginId) {
    return caabApiWebClient
        .patch()
        .uri("/prior-authorities/{prior-authority-id}",
            priorAuthorityId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleUpdateProceedingError(e, priorAuthorityId));
  }

  /**
   * Deletes a specific prior authority.
   *
   * @param priorAuthorityId the ID of the prior authority to delete.
   * @param loginId the login ID of the user performing the deletion.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deletePriorAuthority(
      final Integer priorAuthorityId,
      final String loginId) {
    return caabApiWebClient
        .delete()
        .uri("/prior-authorities/{prior-authority-id}",
            priorAuthorityId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleUpdatePriorAuthorityError(e, priorAuthorityId));
  }


}
