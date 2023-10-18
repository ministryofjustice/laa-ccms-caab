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
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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
   * Retrieves common lookup values based on the type, code, description and sort criteria.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @param description The description of the common lookup values. Can be null.
   * @param sort The sort criteria for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type, String code,
      String description, String sort) {

    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/common")
            .queryParamIfPresent("type", Optional.ofNullable(type))
            .queryParamIfPresent("code", Optional.ofNullable(code))
            .queryParamIfPresent("description", Optional.ofNullable(description))
            .queryParamIfPresent("sort", Optional.ofNullable(sort))
            .build())
        .retrieve()
        .bodyToMono(CommonLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleCommonValuesError(
            type, code, sort, e));
  }

  /**
   * Retrieves common lookup values based on the type, code, and description.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @param description The description for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type, String code, String description) {
    return this.getCommonValues(type, code, description, null);
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
   * Retrieves common lookup values based on the supplied type.
   *
   * @param type The type of the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type) {
    return this.getCommonValues(type, null);
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
   * Retrieves proceeding detail.
   *
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<ProceedingDetail> getProceeding(String proceedingCode) {
    return ebsApiWebClient
        .get()
        .uri("/proceedings/{proceeding-code}", proceedingCode)
        .retrieve()
        .bodyToMono(ProceedingDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleProceedingError(proceedingCode, e));
  }

  /**
   * Retrieves scope limitation details which match the provided example ScopeLimitationDetail.
   *
   * @return A Mono containing the ScopeLimitationDetails or an error handler if an error occurs.
   */
  public Mono<ScopeLimitationDetails> getScopeLimitations(
      ScopeLimitationDetail scopeLimitationDetail) {
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/scope-limitations")
            .queryParamIfPresent("scope-limitations",
                Optional.ofNullable(scopeLimitationDetail.getScopeLimitations()))
            .queryParamIfPresent("category-of-law",
                Optional.ofNullable(scopeLimitationDetail.getCategoryOfLaw()))
            .queryParamIfPresent("matter-type",
                Optional.ofNullable(scopeLimitationDetail.getMatterType()))
            .queryParamIfPresent("proceeding-code",
                Optional.ofNullable(scopeLimitationDetail.getProceedingCode()))
            .queryParamIfPresent("level-of-service",
                Optional.ofNullable(scopeLimitationDetail.getLevelOfService()))
            .queryParamIfPresent("default-wording",
                Optional.ofNullable(scopeLimitationDetail.getDefaultWording()))
            .queryParamIfPresent("stage",
                Optional.ofNullable(scopeLimitationDetail.getStage()))
            .queryParamIfPresent("cost-limitation",
                Optional.ofNullable(scopeLimitationDetail.getCostLimitation()))
            .queryParamIfPresent("emergency-cost-limitation",
                Optional.ofNullable(scopeLimitationDetail.getEmergencyCostLimitation()))
            .queryParamIfPresent("non-standard-wording",
                Optional.ofNullable(scopeLimitationDetail.getNonStandardWordingRequired()))
            .queryParamIfPresent("emergency-scope-default",
                Optional.ofNullable(scopeLimitationDetail.getEmergencyScopeDefault()))
            .queryParamIfPresent("emergency-scope-default",
                Optional.ofNullable(scopeLimitationDetail.getEmergencyScopeDefault()))
            .queryParamIfPresent("emergency",
                Optional.ofNullable(scopeLimitationDetail.getEmergency()))
            .queryParamIfPresent("default-code",
                Optional.ofNullable(scopeLimitationDetail.getDefaultCode()))
            .queryParamIfPresent("scope-default",
                Optional.ofNullable(scopeLimitationDetail.getScopeDefault()))
            .build())
        .retrieve()
        .bodyToMono(ScopeLimitationDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleScopeLimitationsError(
            scopeLimitationDetail, e));
  }

  /**
   * Retrieves outcome result lookup detail based on the provided proceedingCode and
   * outcomeResult values.
   *
   * @param proceedingCode - the proceeding code.
   * @param outcomeResult - the outcome result value.
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<OutcomeResultLookupDetail> getOutcomeResults(String proceedingCode,
      String outcomeResult) {
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/outcome-results")
            .queryParamIfPresent("proceeding-code",
                Optional.ofNullable(proceedingCode))
            .queryParamIfPresent("outcome-result",
                Optional.ofNullable(outcomeResult))
            .build())
        .retrieve()
        .bodyToMono(OutcomeResultLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleOutcomeResultsError(proceedingCode,
            outcomeResult, e));
  }

  /**
   * Retrieves stage end lookup detail based on the provided proceedingCode and
   * stageEnd values.
   *
   * @param proceedingCode - the proceeding code.
   * @param stageEnd - the stage end value.
   * @return A Mono containing the StageEndLookupDetail or an error handler if an error occurs.
   */
  public Mono<StageEndLookupDetail> getStageEnds(String proceedingCode,
      String stageEnd) {
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/stage-ends")
            .queryParamIfPresent("proceeding-code",
                Optional.ofNullable(proceedingCode))
            .queryParamIfPresent("stage-end",
                Optional.ofNullable(stageEnd))
            .build())
        .retrieve()
        .bodyToMono(StageEndLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleStageEndError(proceedingCode,
            stageEnd, e));
  }
}

