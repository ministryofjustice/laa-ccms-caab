package uk.gov.laa.ccms.caab.client;

import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
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
        .onErrorResume(caabApiClientErrorHandler::handleGetProviderDetailsError);
  }

  /**
   * Patches an application using the CAAB API.
   *
   * @param id the ID associated with the application
   * @param loginId the ID associated with the user login
   * @param data the data to amend to the application
   * @param type the type of data being patched (e.g., "application-type", "provider-details")
   * @return a Mono containing application's data
   */
  public Mono<Void> patchApplication(
      final String id,
      final String loginId,
      final Object data,
      final String type) {

    return caabApiWebClient
        .patch()
        .uri("/applications/{id}/" + type, id)
        .header("Caab-User-Login-Id", loginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(data)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> caabApiClientErrorHandler.handlePatchApplicationError(e, type));
  }
}
