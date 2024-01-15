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
import uk.gov.laa.ccms.caab.model.LinkedCase;

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
   * Handles errors encountered during updating an application.
   *
   * @param e the encountered error
   * @return a Mono signaling the error wrapped in a {@code CaabApiServiceException}
   */
  public Mono<Void> handleUpdateApplicationError(Throwable e, String type) {
    final String msg = String.format("Failed to update application - %s", type);
    log.error(msg, e);
    return Mono.error(new CaabApiClientException(msg, e));
  }
}
