package uk.gov.laa.ccms.caab.client;


import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;

/**
 * Provides error-handling capabilities for the CAAB API client interactions.
 */
@Slf4j
@Component
public class CaabApiClientErrorHandler {

  /**
   * Handles errors encountered during application creation.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<String> handleCreateApplicationError(Throwable e) {
    final String msg = "Failed to create application";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during retrieval of an application.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<ApplicationDetail> handleGetApplicationError(Throwable e) {
    final String msg = "Failed to retrieve application";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during retrieval of applications.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<ApplicationDetails> handleGetApplicationsError(Throwable e) {
    final String msg = "Failed to retrieve application";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during retrieval of an application's application type.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<ApplicationType> handleGetApplicationTypeError(Throwable e) {
    final String msg = "Failed to retrieve application type";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during retrieval of an application's provider details.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<ApplicationProviderDetails> handleGetProviderDetailsError(Throwable e) {
    final String msg = "Failed to retrieve provider details";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during retrieval of an application's correspondence address.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Address> handleGetCorrespondenceAddressError(Throwable e) {
    final String msg = "Failed to retrieve correspondence address";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during retrieval of linked cases.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<List<LinkedCase>> handleGetLinkedCasesError(Throwable e) {
    final String msg = "Failed to retrieve linked cases";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during retrieval of proceedings.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<List<Proceeding>> handleGetProceedingError(Throwable e) {
    final String msg = "Failed to retrieve proceedings";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during retrieval of costs.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<CostStructure> handleGetCostsError(Throwable e) {
    final String msg = "Failed to retrieve costs";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during the update of costs.
   *
   * @param e The exception thrown during the update operation.
   * @return A Mono error encapsulating the issue encountered.
   */
  public Mono<Void> handleUpdateCostsError(Throwable e) {
    final String msg = "Failed to update costs";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during retrieval of prior authorities.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<List<PriorAuthority>> handleGetPriorAuthorityError(Throwable e) {
    final String msg = "Failed to retrieve prior authorities";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during the deletion of a linked case.
   *
   * @param e the Throwable associated with the error
   * @param linkedCaseId the ID of the linked case attempted to be removed
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<Void> handleDeleteLinkedCaseError(final Throwable e, final String linkedCaseId) {
    final String msg = String.format("Failed to remove linked case - %s", linkedCaseId);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered when adding a linked case to an application.
   *
   * @param e the Throwable associated with the error
   * @param applicationId the ID of the application the linked case is associated with
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<Void> handleAddLinkedCaseError(final Throwable e, final String applicationId) {
    final String msg = String.format("Failed to add linked case to application: - %s",
        applicationId);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during the update of a linked case.
   *
   * @param e the Throwable associated with the error
   * @param linkedCaseId the ID of the linked case attempted to be updated
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<Void> handleUpdateLinkedCaseError(final Throwable e, final String linkedCaseId) {
    final String msg = String.format("Failed to update linked case - %s", linkedCaseId);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during the update of a proceeding.
   *
   * @param e the Throwable associated with the error
   * @param proceedingId the ID of the linked case attempted to be updated
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<Void> handleUpdateProceedingError(final Throwable e, final Integer proceedingId) {
    final String msg = String.format("Failed to update linked case - %s", proceedingId);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during updating an application.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Void> handleUpdateApplicationError(final Throwable e, final String type) {
    final String msg = String.format("Failed to update application - %s", type);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors encountered during updating clients for an application by their reference.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Void> handleUpdateClientError(final Throwable e, final String clientReference) {
    final String msg = String.format("Failed to update client with reference - %s",
        clientReference);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during retrieval of opponents.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<List<Opponent>> handleGetOpponentsError(Throwable e) {
    final String msg = "Failed to retrieve opponents";
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }

  /**
   * Handles errors during saving of a proceeding.
   *
   * @param e the Throwable associated with the error
   * @return a Mono error encapsulating the issue encountered
   */
  public Mono<Void> handleSaveProceedingError(
      final Throwable e,
      final String id) {
    final String msg = String.format("Failed to save proceeding to application: %s", id);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }
}
