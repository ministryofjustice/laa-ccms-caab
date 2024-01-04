package uk.gov.laa.ccms.caab.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;

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
