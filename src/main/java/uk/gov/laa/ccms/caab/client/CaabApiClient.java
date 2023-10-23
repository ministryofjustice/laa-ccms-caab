package uk.gov.laa.ccms.caab.client;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;

/**
 * Client responsible for interactions with the CAAB API.
 */
@Service
@RequiredArgsConstructor
public class CaabApiClient {

  private final WebClient caabApiWebClient;

  private final CaabApiClientErrorHandler caabApiClientErrorHandler;

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
              HttpHeaders headers = clientResponse.headers().asHttpHeaders();
              URI locationUri = headers.getLocation();
              if (locationUri != null) {
                String path = locationUri.getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                return Mono.just(id);
              } else {
                // Handle the case where the Location header is missing or the URI is invalid
                return Mono.error(new RuntimeException("Location header missing or URI invalid"));
              }
            })
            .onErrorResume(caabApiClientErrorHandler::handleCreateApplicationError);
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
        .onErrorResume(caabApiClientErrorHandler::handleGetApplicationError);
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
        .onErrorResume(caabApiClientErrorHandler::handleGetApplicationTypeError);
  }

  /**
   * Patches an application's application type using the CAAB API.
   *
   * @param id the ID associated with the application
   * @param loginId the ID associated with the user login
   * @param applicationType the application type to amend to the application
   * @return a Mono containing application's application type
   */
  public Mono<Void> patchApplicationType(
      final String id,
      final String loginId,
      final ApplicationType applicationType) {

    return caabApiWebClient
        .patch()
        .uri("/applications/{id}/application-type", id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON) // Set the content type to JSON
        .bodyValue(applicationType) // Add the application details to the request body
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(caabApiClientErrorHandler::handlePatchApplicationTypeError);
  }
}
