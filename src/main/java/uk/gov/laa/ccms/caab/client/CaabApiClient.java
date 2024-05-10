package uk.gov.laa.ccms.caab.client;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/**
 * Client responsible for interactions with the CAAB API.
 */
@Service
@RequiredArgsConstructor
public class CaabApiClient {

  private final WebClient caabApiWebClient;
  private final CaabApiClientErrorHandler caabApiClientErrorHandler;

  public static final String RESOURCE_TYPE_APPLICATION = "applications";
  public static final String RESOURCE_TYPE_APPLICATION_TYPE = "application type";
  public static final String RESOURCE_TYPE_PROVIDER_DETAIL = "provider detail";
  public static final String RESOURCE_TYPE_CORRESPONDENCE_ADDRESS = "correspondence address";
  public static final String RESOURCE_TYPE_LINKED_CASES = "linked cases";
  public static final String RESOURCE_TYPE_COSTS = "cost structure";
  public static final String RESOURCE_TYPE_PRIOR_AUTHORITIES = "prior authorities";
  public static final String RESOURCE_TYPE_PROCEEDINGS = "proceedings";
  public static final String RESOURCE_TYPE_SCOPE_LIMITATIONS = "scope limitations";
  public static final String RESOURCE_TYPE_CLIENT = "client";
  public static final String RESOURCE_TYPE_OPPONENTS = "opponents";

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
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiCreateError(e, RESOURCE_TYPE_APPLICATION));
  }

  /**
   * Retrieves an application using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing applications detail
   */
  public Mono<ApplicationDetail> getApplication(
      final String id) {

    return caabApiWebClient
        .get()
        .uri("/applications/{id}", id)
        .retrieve()
        .bodyToMono(ApplicationDetail.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e, RESOURCE_TYPE_APPLICATION, "id", id));
  }

  /**
   * Query for applications using the CAAB API.
   *
   * @return a Mono containing applications detail
   */
  public Mono<ApplicationDetails> getApplications(
      final CaseSearchCriteria caseSearchCriteria,
      final Integer providerId,
      final Integer page,
      final Integer size) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(caseSearchCriteria.getCaseReference())
        .ifPresent(ref -> queryParams.add("case-reference-number", ref));
    Optional.ofNullable(providerId)
        .ifPresent(ref -> queryParams.add("provider-id", String.valueOf(providerId)));
    Optional.ofNullable(caseSearchCriteria.getProviderCaseReference())
        .ifPresent(ref -> queryParams.add("provider-case-ref", ref));
    Optional.ofNullable(caseSearchCriteria.getClientSurname())
        .ifPresent(surname -> queryParams.add("client-surname", surname));
    Optional.ofNullable(caseSearchCriteria.getClientReference())
        .ifPresent(ref -> queryParams.add("client-reference", ref));
    Optional.ofNullable(caseSearchCriteria.getFeeEarnerId())
        .ifPresent(id -> queryParams.add("fee-earner", String.valueOf(id)));
    Optional.ofNullable(caseSearchCriteria.getOfficeId())
        .ifPresent(id -> queryParams.add("office-id", String.valueOf(id)));
    Optional.ofNullable(caseSearchCriteria.getStatus())
        .ifPresent(status -> queryParams.add("status", status));
    Optional.ofNullable(page)
        .ifPresent(p -> queryParams.add("page", String.valueOf(p)));
    Optional.ofNullable(size)
        .ifPresent(s -> queryParams.add("size", String.valueOf(s)));

    return caabApiWebClient
        .get()
        .uri(builder -> builder.path("/applications")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(ApplicationDetails.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e, RESOURCE_TYPE_APPLICATION, queryParams));
  }


  /**
   * Patches an application with the specified details.
   *
   * @param id The ID associated with the application to be patched.
   * @param patch The application detail changes to be applied.
   * @param loginId the ID associated with the user login
   * @return A Mono indicating the completion of the patch operation.
   */
  public Mono<Void> patchApplication(
      final String id,
      final ApplicationDetail patch,
      final String loginId) {
    return caabApiWebClient
        .patch()
        .uri("/applications/{id}", id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(patch)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiUpdateError(e, RESOURCE_TYPE_APPLICATION, "id", id));
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
    return caabApiWebClient
        .put()
        .uri("/applications/{id}/" + type, id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiUpdateError(e, RESOURCE_TYPE_APPLICATION, "id", id));
  }

  /**
   * Retrieves an application's application type using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's application type
   */
  public Mono<ApplicationType> getApplicationType(
      final String id) {

    return caabApiWebClient
        .get()
        .uri("/applications/{id}/application-type", id)
        .retrieve()
        .bodyToMono(ApplicationType.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_APPLICATION_TYPE, "application id", id));
  }

  /**
   * Retrieves an application's application type using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's application type
   */
  public Mono<ApplicationProviderDetails> getProviderDetails(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/provider-details", id)
        .retrieve()
        .bodyToMono(ApplicationProviderDetails.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_PROVIDER_DETAIL, "application id", id));
  }

  /**
   * Retrieves an application's correspondence address using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono containing application's correspondence address
   */
  public Mono<AddressDetail> getCorrespondenceAddress(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/correspondence-address", id)
        .retrieve()
        .bodyToMono(AddressDetail.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_CORRESPONDENCE_ADDRESS, "application id", id));
  }

  /**
   * Retrieves a list of linked cases associated with a given application ID.
   *
   * @param id the ID of the application for which linked cases are to be retrieved
   * @return a Mono containing a list of LinkedCaseDetail objects associated with the application
   */
  public Mono<List<LinkedCaseDetail>> getLinkedCases(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/linked-cases", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<LinkedCaseDetail>>() {})
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_LINKED_CASES, "application id", id));
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
    return caabApiWebClient
        .delete()
        .uri("/linked-cases/{linkedCaseId}",
            linkedCaseId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiDeleteError(e,
                RESOURCE_TYPE_LINKED_CASES, "linked case id", linkedCaseId));
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
      final LinkedCaseDetail data,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("applications/{applicationId}/linked-cases",
            applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_LINKED_CASES));
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
      final LinkedCaseDetail data,
      final String loginId) {
    return caabApiWebClient
        .patch()
        .uri("/linked-cases/{linkedCaseId}",
            linkedCaseId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_LINKED_CASES, "linked case id", linkedCaseId));
  }

  /**
   * Fetches the cost structure associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the cost structure.
   *
   * @param id The id of the application for which the cost structure should be retrieved.
   * @return A {@code Mono<CostStructureDetail>} containing the cost structure.
   */
  public Mono<CostStructureDetail> getCostStructure(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/cost-structure", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<CostStructureDetail>() {})
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_COSTS, "application id", id));
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
      final CostStructureDetail costs,
      final String loginId) {
    return caabApiWebClient
        .put()
        .uri("/applications/{id}/cost-structure", id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON) // Set the content type to JSON
        .bodyValue(costs)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_COSTS, "application id", id));
  }

  /**
   * Fetches the proceedings associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the proceedings.
   *
   * @param id The id of the application for which proceedings should be retrieved.
   * @return A {@code Mono<List<ProceedingDetail>>} containing the proceedings.
   */
  public Mono<List<ProceedingDetail>> getProceedings(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/proceedings", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<ProceedingDetail>>() {})
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_PROCEEDINGS, "application id", id));
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
      final ProceedingDetail data,
      final String loginId) {
    return caabApiWebClient
        .patch()
        .uri("/proceedings/{proceeding-id}",
            proceedingId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_PROCEEDINGS, "id", String.valueOf(proceedingId)));
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
      final ProceedingDetail proceeding,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/proceedings", applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(proceeding)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_PROCEEDINGS));
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
    return caabApiWebClient
        .delete()
        .uri("/proceedings/{proceeding-id}",
            proceedingId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_PROCEEDINGS, "id", String.valueOf(proceedingId)));
  }

  /**
   * Retrieves the scope limitations associated with a specific proceeding id.
   * This method communicates with the CAAB API client to fetch the scope limitations.
   *
   * @param proceedingId The id of the proceeding for which scope limitations should be retrieved.
   * @return A {@code Mono<List<ScopeLimitationDetail>>} containing the scope limitations.
   * @throws RuntimeException if an error occurs during the retrieval operation
   */
  public Mono<List<ScopeLimitationDetail>> getScopeLimitations(
      final Integer proceedingId) {
    return caabApiWebClient
        .get()
        .uri("/proceedings/{id}/scope-limitations", proceedingId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<ScopeLimitationDetail>>() {})
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_SCOPE_LIMITATIONS, "proceeding id", String.valueOf(proceedingId)));
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
      final BaseClientDetail data) {
    return caabApiWebClient
        .patch()
        .uri("/applications/clients/{clientReferenceId}", clientReferenceId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_CLIENT, "client reference", clientReferenceId));
  }

  /**
   * Fetches the opponents associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the opponents.
   *
   * @param id The id of the application for which opponents should be retrieved.
   * @return A {@code Mono<List<OpponentDetail>>} containing the opponents.
   */
  public Mono<List<OpponentDetail>> getOpponents(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/opponents", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<OpponentDetail>>() {})
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_OPPONENTS, "application id", id));
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
      final OpponentDetail data,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/opponents",
            applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_OPPONENTS));
  }

  /**
   * Updates an opponent in the CAAB API for a given application.
   *
   * @param opponentId the ID of the opponent to be updated
   * @param data the new data for the opponent
   * @param loginId the login ID of the user performing the update
   * @return a Mono indicating completion of the update operation
   */
  public Mono<Void> updateOpponent(
      final Integer opponentId,
      final OpponentDetail data,
      final String loginId) {
    return caabApiWebClient
        .patch()
        .uri("/opponents/{opponent-id}",
            opponentId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_OPPONENTS, "id", String.valueOf(opponentId)));
  }

  /**
   * Deletes an opponent from the CAAB API.
   *
   * @param opponentId the ID of the opponent to be deleted
   * @param loginId the ID associated with the user login
   * @return a Mono Void indicating the completion of the delete operation
   */
  public Mono<Void> deleteOpponent(
      final Integer opponentId,
      final String loginId) {
    return caabApiWebClient
        .delete()
        .uri("/opponents/{opponent-id}",
            opponentId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_OPPONENTS, "id", String.valueOf(opponentId)));
  }

  /**
   * Fetches the prior authorities associated with a specific application id.
   * This method communicates with the CAAB API client to fetch the prior authorities.
   *
   * @param id The id of the application for which prior authorities should be retrieved.
   * @return A {@code Mono<List<PriorAuthorityDetail>>} containing the prior authorities.
   */
  public Mono<List<PriorAuthorityDetail>> getPriorAuthorities(
      final String id) {
    return caabApiWebClient
        .get()
        .uri("/applications/{id}/prior-authorities", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<PriorAuthorityDetail>>() {})
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_PRIOR_AUTHORITIES, "application id", id));
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
      final PriorAuthorityDetail priorAuthority,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/applications/{applicationId}/prior-authorities", applicationId)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(priorAuthority)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_PRIOR_AUTHORITIES));
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
      final PriorAuthorityDetail data,
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
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_PRIOR_AUTHORITIES, "id", String.valueOf(priorAuthorityId)));
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
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_PRIOR_AUTHORITIES, "id", String.valueOf(priorAuthorityId)));
  }


}
