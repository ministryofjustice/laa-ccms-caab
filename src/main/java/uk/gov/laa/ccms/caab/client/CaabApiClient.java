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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetails;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
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
  public static final String RESOURCE_TYPE_CASE_OUTCOME = "case outcome";
  public static final String RESOURCE_TYPE_EVIDENCE = "evidence";
  public static final String RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS = "notification attachments";

  /**
   * Creates an application using the CAAB API.
   *
   * @param loginId     the ID associated with the user login
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
        .contentType(MediaType.APPLICATION_JSON)// Set the content type to JSON
        .bodyValue(application)// Add the application details to the request body
        .exchangeToMono(CaabApiClient::getIdResponse)
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
   * @param id      The ID associated with the application to be patched.
   * @param patch   The application detail changes to be applied.
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
   * @param id      the ID associated with the application
   * @param loginId the ID associated with the user login
   * @param data    the data to amend to the application
   * @param type    the type of data being putted (examples - "application-type",
   *                "provider-details", "correspondence-address")
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
   * Deletes an application using the CAAB API.
   *
   * @param id the ID associated with the application
   * @return a Mono Void indicating the completion of the delete operation
   */
  public Mono<Void> deleteApplication(
      final String id, final String loginId) {

    return caabApiWebClient
        .delete()
        .uri("/applications/{id}", id)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_APPLICATION, "id", String.valueOf(e)));
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
  public Mono<Void> putApplicationType(final Integer id, final String loginId,
                                                  final ApplicationType data) {
    return caabApiWebClient
        .put()
        .uri("/applications/{id}/application-type", id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_APPLICATION_TYPE, "application id", String.valueOf(id)));
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
        .bodyToMono(new ParameterizedTypeReference<List<LinkedCaseDetail>>() {
        })
        .onErrorResume(e -> caabApiClientErrorHandler
            .handleApiRetrieveError(e,
                RESOURCE_TYPE_LINKED_CASES, "application id", id));
  }

  /**
   * Removes a linked case from the CAAB API for a given application.
   *
   * @param linkedCaseId the ID of the linked case to be removed
   * @param loginId      the login ID of the user performing the removal
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
        .uri("/applications/{applicationId}/linked-cases",
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
   * @param data         the new data for the linked case
   * @param loginId      the login ID of the user performing the update
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
   * Fetches the cost structure associated with a specific application id. This method communicates
   * with the CAAB API client to fetch the cost structure.
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
        .bodyToMono(new ParameterizedTypeReference<CostStructureDetail>() {
        })
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_COSTS, "application id", id));
  }

  /**
   * Updates the cost structure of an application using the CAAB API.
   *
   * @param id      The ID of the application.
   * @param costs   The new cost structure for the application.
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
        .contentType(MediaType.APPLICATION_JSON)// Set the content type to JSON
        .bodyValue(costs)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_COSTS, "application id", id));
  }

  /**
   * Fetches the proceedings associated with a specific application id. This method communicates
   * with the CAAB API client to fetch the proceedings.
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
        .bodyToMono(new ParameterizedTypeReference<List<ProceedingDetail>>() {
        })
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_PROCEEDINGS, "application id", id));
  }

  /**
   * Updates a proceeding in the CAAB API for a given application.
   *
   * @param proceedingId the ID of the proceeding to be updated
   * @param data         the new data for the proceeding
   * @param loginId      the login ID of the user performing the update
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
   * @param proceeding    The proceeding to be added.
   * @param loginId       The ID associated with the user login.
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
   * @param loginId      the ID associated with the user login
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
   * Retrieves the scope limitations associated with a specific proceeding id. This method
   * communicates with the CAAB API client to fetch the scope limitations.
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
        .bodyToMono(new ParameterizedTypeReference<List<ScopeLimitationDetail>>() {
        })
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_SCOPE_LIMITATIONS, "proceeding id", String.valueOf(proceedingId)));
  }

  /**
   * Patches all application clients with the same reference id using the CAAB API.
   *
   * @param clientReferenceId the ID associated with the client
   * @param loginId           the ID associated with the user login
   * @param data              the client data to amend to the application
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
   * Fetches the opponents associated with a specific application id. This method communicates with
   * the CAAB API client to fetch the opponents.
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
        .bodyToMono(new ParameterizedTypeReference<List<OpponentDetail>>() {
        })
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
   * @param data       the new data for the opponent
   * @param loginId    the login ID of the user performing the update
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
   * @param loginId    the ID associated with the user login
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
   * Fetches the prior authorities associated with a specific application id. This method
   * communicates with the CAAB API client to fetch the prior authorities.
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
        .bodyToMono(new ParameterizedTypeReference<List<PriorAuthorityDetail>>() {
        })
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_PRIOR_AUTHORITIES, "application id", id));
  }

  /**
   * Adds a priorAuthority to an application using the CAAB API.
   *
   * @param applicationId  The ID of the application.
   * @param priorAuthority The priorAuthority to be added.
   * @param loginId        The ID associated with the user login.
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
   * @param data             the new data for the prior authority.
   * @param loginId          the login ID of the user performing the update.
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
   * @param loginId          the login ID of the user performing the deletion.
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

  /**
   * Fetches the case outcomes with the specified case reference number and provider id. This method
   * communicates with the CAAB API client to fetch the case outcomes.
   *
   * @param caseReferenceNumber The case reference number of the case outcome.
   * @param providerId          The provider id of the case outcome.
   * @return A {@code Mono<CaseOutcomeDetails>} containing the case outcomes.
   */
  public Mono<CaseOutcomeDetails> getCaseOutcomes(
      final String caseReferenceNumber,
      final Integer providerId) {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(ref -> queryParams.add("case-reference-number", caseReferenceNumber));
    Optional.ofNullable(providerId)
        .ifPresent(ref -> queryParams.add("provider-id", String.valueOf(providerId)));

    return caabApiWebClient
        .get()
        .uri(builder -> builder.path("/case-outcomes")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CaseOutcomeDetails.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_CASE_OUTCOME, queryParams));
  }

  /**
   * Fetches the case outcome with the specified id. This method communicates with the CAAB API
   * client to fetch the case outcome.
   *
   * @param id The id of the case outcome.
   * @return A {@code Mono<CaseOutcomeDetail>} containing the case outcome.
   */
  public Mono<CaseOutcomeDetail> getCaseOutcome(
      final String id) {

    return caabApiWebClient
        .get()
        .uri("/case-outcomes/{case-outcome-id}", id)
        .retrieve()
        .bodyToMono(CaseOutcomeDetail.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_CASE_OUTCOME, "id", id));
  }

  /**
   * Creates a case outcome using the CAAB API.
   *
   * @param loginId     the ID associated with the user login
   * @param caseOutcome the details of the case outcome to be created
   * @return a Mono signaling the completion of the application creation
   */
  public Mono<String> createCaseOutcome(
      final String loginId,
      final CaseOutcomeDetail caseOutcome) {

    return caabApiWebClient
        .post()
        .uri("/case-outcomes")
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)// Set the content type to JSON
        .bodyValue(caseOutcome)// Add the case outcome detail to the request body
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
            .handleApiCreateError(e, RESOURCE_TYPE_CASE_OUTCOME));
  }

  /**
   * Deletes a case outcome from the CAAB API.
   *
   * @param caseOutcomeId the ID of the case outcome to be deleted
   * @param loginId       the ID associated with the user login
   * @return a Mono Void indicating the completion of the delete operation
   */
  public Mono<Void> deleteCaseOutcome(
      final Integer caseOutcomeId,
      final String loginId) {
    return caabApiWebClient
        .delete()
        .uri("/case-outcomes/{case-outcome-id}",
            caseOutcomeId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_CASE_OUTCOME, "id", String.valueOf(caseOutcomeId)));
  }

  /**
   * Deletes all case outcomes with the specified case reference number and/or provider id. This
   * method communicates with the CAAB API client to delete the case outcomes.
   *
   * @param caseReferenceNumber The case reference number criteria.
   * @param providerId          The provider id criteria.
   * @param loginId             the ID associated with the user login
   * @return a Mono Void indicating the completion of the delete operation
   */
  public Mono<Void> deleteCaseOutcomes(
      final String caseReferenceNumber,
      final Integer providerId,
      final String loginId) {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(ref -> queryParams.add("case-reference-number", caseReferenceNumber));
    Optional.ofNullable(providerId)
        .ifPresent(ref -> queryParams.add("provider-id", String.valueOf(providerId)));

    return caabApiWebClient
        .delete()
        .uri(builder -> builder.path("/case-outcomes")
            .queryParams(queryParams)
            .build())
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_CASE_OUTCOME, queryParams));
  }

  /**
   * Asynchronously creates an evidence document.
   *
   * @param data    The evidence document information.
   * @param loginId The login ID of the user performing the operation.
   * @return A Mono wrapping the id of the newly created evidence document.
   */
  public Mono<String> createEvidenceDocument(
      final EvidenceDocumentDetail data,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/evidence")
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .exchangeToMono(CaabApiClient::getIdResponse)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_EVIDENCE));
  }


  /**
   * Updates an existing evidence document with the provided data.
   *
   * @param data the evidence document details to be updated
   * @param loginId the login ID of the user performing the update
   * @return a {@link Mono} that completes when the update is finished, or emits an error if the
   *         document has no ID
   */
  public Mono<Void> updateEvidenceDocument(
      final EvidenceDocumentDetail data,
      final String loginId) {

    if (data.getId() == null) {
      return Mono.error(new IllegalArgumentException("EvidenceDocumentDetail must have an ID"));
    }

    return caabApiWebClient
        .patch()
        .uri("/evidence/{evidence-document-id}", data.getId())
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(
            e, RESOURCE_TYPE_EVIDENCE, "id", String.valueOf(data.getId())));
  }

  /**
   * Fetches the uploaded evidence documents based on the supplied search criteria.
   * This method communicates with the CAAB API client to fetch the evidence documents.
   *
   * @param applicationOrOutcomeId The id of the related application or outcome.
   * @param caseReferenceNumber    the reference of the related case.
   * @param providerId             The id of the related provider.
   * @param documentType           The type of evidence document.
   * @param ccmsModule             The ccms module for the evidence.
   * @param transferPending        whether transfer has been attempted for the evidence document.
   * @return A {@code Mono<EvidenceDocumentDetails} containing the evidence documents.
   */
  public Mono<EvidenceDocumentDetails> getEvidenceDocuments(
      final String applicationOrOutcomeId,
      final String caseReferenceNumber,
      final Integer providerId,
      final String documentType,
      final String ccmsModule,
      final Boolean transferPending) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(applicationOrOutcomeId)
        .ifPresent(param -> queryParams.add("application-or-outcome-id", param));
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(param -> queryParams.add("case-reference-number", param));
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", String.valueOf(param)));
    Optional.ofNullable(documentType)
        .ifPresent(param -> queryParams.add("document-type", param));
    Optional.ofNullable(ccmsModule)
        .ifPresent(param -> queryParams.add("ccms-module", param));
    Optional.ofNullable(transferPending)
        .ifPresent(param -> queryParams.add("transfer-pending", param.toString()));

    return caabApiWebClient
        .get()
        .uri(builder -> builder.path("/evidence")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(EvidenceDocumentDetails.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_EVIDENCE, queryParams));
  }

  /**
   * Fetches a single evidence document by id. This method communicates with the CAAB API client to
   * fetch the evidence document.
   *
   * @param evidenceDocumentId The id of the evidence document to be retrieved.
   * @return A {@code Mono<EvidenceDocumentDetail>} containing the evidence document data.
   */
  public Mono<EvidenceDocumentDetail> getEvidenceDocument(final Integer evidenceDocumentId) {
    return caabApiWebClient
        .get()
        .uri("/evidence/{evidence-document-id}", evidenceDocumentId)
        .retrieve()
        .bodyToMono(EvidenceDocumentDetail.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_EVIDENCE, "evidence document id",
            String.valueOf(evidenceDocumentId)));
  }

  /**
   * Deletes a specific evidence document.
   *
   * @param evidenceDocumentId the ID of the evidence document to delete.
   * @param loginId            the login ID of the user performing the deletion.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deleteEvidenceDocument(
      final Integer evidenceDocumentId,
      final String loginId) {
    return caabApiWebClient
        .delete()
        .uri("/evidence/{evidence-document-id}",
            evidenceDocumentId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_EVIDENCE, "id", String.valueOf(evidenceDocumentId)));
  }

  /**
   * Asynchronously creates a notification attachment.
   *
   * @param data    The notification attachment information.
   * @param loginId The login ID of the user performing the operation.
   * @return A Mono wrapping the id of the newly created notification attachment.
   */
  public Mono<String> createNotificationAttachment(
      final NotificationAttachmentDetail data,
      final String loginId) {
    return caabApiWebClient
        .post()
        .uri("/notification-attachments")
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .exchangeToMono(CaabApiClient::getIdResponse)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiCreateError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS));
  }

  /**
   * Asynchronously updates a notification attachment.
   *
   * @param data    The notification attachment information.
   * @param loginId The login ID of the user performing the operation.
   * @return A Mono wrapping the id of the newly created notification attachment.
   */
  public Mono<Void> updateNotificationAttachment(
      final NotificationAttachmentDetail data,
      final String loginId) {
    return caabApiWebClient
        .put()
        .uri("/notification-attachments/{notification-attachment-id}", data.getId())
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiUpdateError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS, "id", String.valueOf(data.getId())));
  }


  /**
   * Fetches the uploaded notification attachment based on the supplied search criteria. This method
   * communicates with the CAAB API client to fetch the notification attachments.
   *
   * @param notificationReference The id of the related notification.
   * @param providerId            The id of the related provider.
   * @param documentType          The type of notification attachment.
   * @param sendBy                Whether the document is electronic or postal.
   * @return A {@code Mono<NotificationAttachmentDetails>} containing the notification attachments.
   */
  public Mono<NotificationAttachmentDetails> getNotificationAttachments(
      final String notificationReference,
      final Integer providerId,
      final String documentType,
      final String sendBy) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(notificationReference)
        .ifPresent(param -> queryParams.add("notification-reference", param));
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", String.valueOf(param)));
    Optional.ofNullable(documentType)
        .ifPresent(param -> queryParams.add("document-type", param));
    Optional.ofNullable(sendBy)
        .ifPresent(param -> queryParams.add("send-by", param));

    return caabApiWebClient
        .get()
        .uri(builder -> builder.path("/notification-attachments")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(NotificationAttachmentDetails.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS, queryParams));
  }

  /**
   * Fetches a single notification attachment by id. This method communicates with the CAAB API
   * client to fetch the notification attachment.
   *
   * @param notificationAttachmentId The id of the notification attachment to be retrieved.
   * @return A {@code Mono<NotificationAttachmentDetail>} containing the notification attachment
   *         data.
   */
  public Mono<NotificationAttachmentDetail> getNotificationAttachment(
      final Integer notificationAttachmentId) {
    return caabApiWebClient
        .get()
        .uri("/notification-attachments/{notification-attachment-id}", notificationAttachmentId)
        .retrieve()
        .bodyToMono(NotificationAttachmentDetail.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiRetrieveError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS, "notification-attachment-id",
            String.valueOf(notificationAttachmentId)));
  }

  /**
   * Deletes a specific notification attachment.
   *
   * @param notificationAttachmentId the ID of the notification attachment to delete.
   * @param loginId                  the login ID of the user performing the deletion.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deleteNotificationAttachment(
      final Integer notificationAttachmentId,
      final String loginId) {
    return caabApiWebClient
        .delete()
        .uri("/notification-attachments/{notification-attachment-id}",
            notificationAttachmentId)
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS, "id", String.valueOf(e)));
  }

  /**
   * Deletes all notification attachments based on the supplied search criteria.
   *
   * @param notificationReference The id of the related notification.
   * @param providerId            The id of the related provider.
   * @param documentType          The type of notification attachment.
   * @param sendBy                Whether the document is electronic or postal.
   * @param loginId               the login ID of the user performing the deletion.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deleteNotificationAttachments(
      final String notificationReference,
      final Integer providerId,
      final String documentType,
      final String sendBy,
      final String loginId) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(notificationReference)
        .ifPresent(param -> queryParams.add("notification-reference", param));
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", String.valueOf(param)));
    Optional.ofNullable(documentType)
        .ifPresent(param -> queryParams.add("document-type", param));
    Optional.ofNullable(sendBy)
        .ifPresent(param -> queryParams.add("send-by", param));

    return caabApiWebClient
        .delete()
        .uri(builder -> builder.path("/notification-attachments")
            .queryParams(queryParams)
            .build())
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_NOTIFICATION_ATTACHMENTS, queryParams));
  }

  /**
   * Deletes all documents based on the supplied search criteria.
   *
   * @param applicationOrOutcomeId The id of the related application or outcome.
   * @param caseReferenceNumber the reference of the related case.
   * @param providerId The id of the related provider.
   * @param documentType The type of evidence document.
   * @param ccmsModule The ccms module for the evidence.
   * @param transferPending whether transfer has been attempted for the evidence document.
   * @return a Mono signaling completion or error handling.
   */
  public Mono<Void> deleteEvidenceDocuments(
      final String applicationOrOutcomeId,
      final String caseReferenceNumber,
      final Integer providerId,
      final String documentType,
      final String ccmsModule,
      final Boolean transferPending,
      final String loginId) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(applicationOrOutcomeId)
        .ifPresent(param -> queryParams.add("application-or-outcome-id", param));
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(param -> queryParams.add("case-reference-number", param));
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", String.valueOf(param)));
    Optional.ofNullable(documentType)
        .ifPresent(param -> queryParams.add("document-type", param));
    Optional.ofNullable(ccmsModule)
        .ifPresent(param -> queryParams.add("ccms-module", param));
    Optional.ofNullable(transferPending)
        .ifPresent(param -> queryParams.add("transfer-pending", param.toString()));

    return caabApiWebClient
        .delete()
        .uri(builder -> builder.path("/evidence")
            .queryParams(queryParams)
            .build())
        .header("Caab-User-Login-Id", loginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handleApiDeleteError(e,
            RESOURCE_TYPE_EVIDENCE, queryParams));
  }

  private MultiValueMap<String, String> createDefaultQueryParams() {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("size", "1000");
    return queryParams;
  }

  private static Mono<String> getIdResponse(ClientResponse clientResponse) {
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
  }

}
