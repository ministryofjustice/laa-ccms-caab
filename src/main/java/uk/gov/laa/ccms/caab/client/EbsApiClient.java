package uk.gov.laa.ccms.caab.client;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

/**
 * Client class responsible for interacting with the ebs-api microservice to retrieve various data
 * entities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EbsApiClient {
  private final WebClient ebsApiWebClient;

  private final EbsApiClientErrorHandler ebsApiClientErrorHandler;

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(String loginId) {

    return ebsApiWebClient
            .get()
            .uri("/users/{loginId}", loginId)
            .retrieve()
            .bodyToMono(UserDetail.class)
            .onErrorResume(e -> ebsApiClientErrorHandler.handleUserError(loginId, e));
  }

  /**
   * Retrieves details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the ProviderDetail or an error handler if an error occurs.
   */
  public Mono<ProviderDetail> getProvider(Integer providerId) {
    return ebsApiWebClient
        .get()
        .uri("/providers/{providerId}", String.valueOf(providerId))
        .retrieve()
        .bodyToMono(ProviderDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleProviderError(providerId, e));
  }

  /**
   * Retrieves common lookup values based on the type, code, and sort criteria.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @param sort The sort criteria for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type, String code, String sort) {

    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/common")
                    .queryParamIfPresent("type", Optional.ofNullable(type))
                    .queryParamIfPresent("code", Optional.ofNullable(code))
                    .queryParamIfPresent("sort", Optional.ofNullable(sort))
                    .build())
            .retrieve()
            .bodyToMono(CommonLookupDetail.class)
            .onErrorResume(e -> ebsApiClientErrorHandler.handleCommonValuesError(
                    type, code, sort, e));
  }

  /**
   * Retrieves common lookup values based on the supplied type.
   *
   * @param type The type of the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type) {
    return this.getCommonValues(type, null, null);
  }

  /**
   * Retrieves common lookup values based on the supplied type and code.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type, String code) {
    return this.getCommonValues(type, code, null);
  }


  /**
   * Retrieves the person to case relationships lookup values.
   *
   * @return A Mono containing the RelationshipToCaseLookupDetail or an error handler if an error
   *         occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getPersonRelationshipsToCaseValues() {

    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/person-to-case-relationships")
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
       .onErrorResume(ebsApiClientErrorHandler::handleToCaseRelationshipValuesError);
  }

  /**
   * Retrieves the organisation to case relationships lookup values.
   *
   * @return A Mono containing the RelationshipToCaseLookupDetail or an error handler if an error
   *         occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getOrganisationRelationshipsToCaseValues() {

    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/organisation-to-case-relationships")
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(ebsApiClientErrorHandler::handleToCaseRelationshipValuesError);
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(Boolean copyAllowed) {

    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/case-status")
                    .queryParamIfPresent("copy-allowed", Optional.ofNullable(copyAllowed))
                    .build())
            .retrieve()
            .bodyToMono(CaseStatusLookupDetail.class)
            .onErrorResume(e -> ebsApiClientErrorHandler
                    .handleCaseStatusValuesError(copyAllowed, e));
  }

  /**
   * Retrieves amendment type lookup details based on the provided application type.
   *
   * @param applicationType The application type to retrieve amendment types for.
   * @return A Mono containing the AmendmentTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(String applicationType) {
    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/amendment-types")
                    .queryParamIfPresent("application-type",
                            Optional.ofNullable(applicationType))
                    .build())
            .retrieve()
            .bodyToMono(AmendmentTypeLookupDetail.class)
            .onErrorResume(e -> ebsApiClientErrorHandler
                    .handleAmendmentTypeLookupError(applicationType, e));
  }

  /**
   * Retrieves country lookup details.
   *
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCountries() {
    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/countries")
                    .queryParam("size", 1000)
                    .build())
            .retrieve()
            .bodyToMono(CommonLookupDetail.class)
            .onErrorResume(ebsApiClientErrorHandler::handleCountryLookupError);
  }

  /**
   * Retrieve the list of users for a given Provider.
   *
   * @param providerId the provider id.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetails> getUsers(final Integer providerId) {
    return  ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/users")
            .queryParamIfPresent("provider-id",
                Optional.ofNullable(providerId))
            .build())
        .retrieve()
        .bodyToMono(UserDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler
            .handleUsersError(Integer.toString(providerId), e));

  }

}

