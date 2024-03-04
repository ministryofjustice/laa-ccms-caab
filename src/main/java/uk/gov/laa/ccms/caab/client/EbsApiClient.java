package uk.gov.laa.ccms.caab.client;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
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

  private static final String USER_ERROR_MESSAGE = "Failed to retrieve User with loginId: %s";
  private static final String COMMON_VALUES_ERROR_MESSAGE =
      "Failed to retrieve Common Values: (type: %s, code: %s, sort: %s)";
  private static final String CASE_STATUS_VALUES_ERROR_MESSAGE =
      "Failed to retrieve Case Status Values: (copyAllowed: %s)";
  private static final String PROVIDER_ERROR_MESSAGE =
      "Failed to retrieve Provider: (id: %s)";
  private static final String AMENDMENT_TYPE_ERROR_MESSAGE =
      "Failed to retrieve Amendment Types: (applicationType: %s)";
  private static final String COUNTRY_ERROR_MESSAGE =
      "Failed to retrieve Countries";
  private static final String USERS_ERROR_MESSAGE =
      "Failed to retrieve Users for provider: (id: %s)";
  private static final String RELATIONSHIP_TO_CASE_ERROR_MESSAGE =
      "Failed to retrieve relationship to case";
  private static final String MATTER_TYPE_ERROR_MESSAGE =
      "Failed to retrieve matter types: (categoryOfLaw: %s)";
  private static final String PROCEEDING_ERROR_MESSAGE =
      "Failed to retrieve Proceeding: (code: %s)";
  private static final String PROCEEDINGS_ERROR_MESSAGE =
      "Failed to retrieve Proceedings: (categoryOfLaw: %s, matterType: %s, amendmentOnly: %s, "
          + "larScopeFlag: %s, applicationType: %s, isLead: %s)";
  private static final String CLIENT_INVOLVEMENT_ERROR_MESSAGE =
      "Failed to retrieve client involvement types: (proceedingCode: %s)";
  private static final String LEVEL_OF_SERVICE_ERROR_MESSAGE =
      "Failed to retrieve level of service types: (proceedingCode: %s, categoryOfLaw: %s, "
          + "matterType: %s)";
  private static final String SCOPE_LIMITATIONS_ERROR_MESSAGE =
      "Failed to retrieve ScopeLimitationsDetails: (scopeLimitations: %s, categoryOfLaw: %s, "
          + "matterType: %s, proceedingCode: %s, levelOfService: %s, defaultWording: %s, "
          + "stage: %s, costLimitation: %s, emergencyCostLimitation: %s, "
          + "nonStandardWordingRequired: %s, emergencyScopeDefault: %s, emergency: %s, "
          + "defaultCode: %s, scopeDefault: %s)";
  private static final String OUTCOME_RESULTS_ERROR_MESSAGE =
      "Failed to retrieve OutcomeResultDetails with search criteria: (proceedingCode: %s, "
          + "outcomeResult: %s)";
  private static final String STAGE_END_ERROR_MESSAGE =
      "Failed to retrieve StageEndLookupDetails with search criteria: (proceedingCode: %s, "
          + "stageEnd: %s)";
  private static final String PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE =
      "Failed to retrieve prior authority types: (code: %s, valueRequired: %s)";
  private static final String AWARD_TYPE_ERROR_MESSAGE =
      "Failed to retrieve award types: (code: %s, awardType: %s)";
  private static final String CATEGORIES_OF_LAW_ERROR_MESSAGE =
      "Failed to retrieve categories of law: (code: %s, matterType: %s, copyCostLimit: %s)";
  private static final String PERSON_RELATIONSHIP_TO_CASE_ERROR_MESSAGE =
      "Failed to retrieve person relationship to case: (code: %s, description: %s)";
  private static final String ORGANISATION_RELATIONSHIP_TO_CASE_ERROR_MESSAGE =
      "Failed to retrieve organisation relationship to case: (code: %s, description: %s)";

  private final WebClient ebsApiWebClient;

  private final ApiClientErrorHandler apiClientErrorHandler;

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(final String loginId) {
    final String errorMessage = String.format(USER_ERROR_MESSAGE, loginId);
    return ebsApiWebClient
            .get()
            .uri("/users/{loginId}", loginId)
            .retrieve()
            .bodyToMono(UserDetail.class)
            .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the ProviderDetail or an error handler if an error occurs.
   */
  public Mono<ProviderDetail> getProvider(final Integer providerId) {
    final String errorMessage = String.format(PROVIDER_ERROR_MESSAGE, providerId);
    return ebsApiWebClient
        .get()
        .uri("/providers/{providerId}", String.valueOf(providerId))
        .retrieve()
        .bodyToMono(ProviderDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
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
  public Mono<CommonLookupDetail> getCommonValues(final String type, final String code,
      final String description, final String sort) {
    final String errorMessage = String.format(COMMON_VALUES_ERROR_MESSAGE, type, code, sort);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/common")
            .queryParamIfPresent("type", Optional.ofNullable(type))
            .queryParamIfPresent("code", Optional.ofNullable(code))
            .queryParamIfPresent("description", Optional.ofNullable(description))
            .queryParamIfPresent("sort", Optional.ofNullable(sort))
            .queryParam("size", 1000)
            .build())
        .retrieve()
        .bodyToMono(CommonLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves common lookup values based on the type, code, and description.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @param description The description for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(final String type, final String code,
      final String description) {
    return this.getCommonValues(type, code, description, null);
  }

  /**
   * Retrieves common lookup values based on the supplied type and code.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(final String type, final String code) {
    return this.getCommonValues(type, code, null);
  }

  /**
   * Retrieves common lookup values based on the supplied type.
   *
   * @param type The type of the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(final String type) {
    return this.getCommonValues(type, null);
  }

  /**
   * Retrieves the matter type lookup values.
   *
   * @return A Mono containing the Matter types or an error handler if an error occurs.
   */
  public Mono<MatterTypeLookupDetail> getMatterTypes(final String categoryOfLaw) {
    final String errorMessage = String.format(MATTER_TYPE_ERROR_MESSAGE, categoryOfLaw);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/matter-types")
            .queryParamIfPresent("category-of-law", Optional.ofNullable(categoryOfLaw))
            .build())
        .retrieve()
        .bodyToMono(MatterTypeLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
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
        .onErrorResume(e -> apiClientErrorHandler
            .handleEbsApiError(e, RELATIONSHIP_TO_CASE_ERROR_MESSAGE));
  }

  /**
   * Retrieves the organisation to case relationships lookup values, optionally
   * filtered on code and description value.
   *
   * @param code - the relationship code.
   * @param description - the relationship description value.
   * @return A Mono containing the RelationshipToCaseLookupDetail or an error handler if an error
   *         occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getOrganisationToCaseRelationshipValues(
      final String code,
      final String description) {
    final String errorMessage = String.format(
        ORGANISATION_RELATIONSHIP_TO_CASE_ERROR_MESSAGE, code, description);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/organisation-to-case-relationships")
            .queryParamIfPresent("code",
                Optional.ofNullable(code))
            .queryParamIfPresent("description",
                Optional.ofNullable(description))
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(
      final Boolean copyAllowed) {
    final String errorMessage = String.format(CASE_STATUS_VALUES_ERROR_MESSAGE, copyAllowed);
    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/case-status")
                    .queryParamIfPresent("copy-allowed", Optional.ofNullable(copyAllowed))
                    .build())
            .retrieve()
            .bodyToMono(CaseStatusLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves amendment type lookup details based on the provided application type.
   *
   * @param applicationType The application type to retrieve amendment types for.
   * @return A Mono containing the AmendmentTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(
      final String applicationType) {
    final String errorMessage = String.format(AMENDMENT_TYPE_ERROR_MESSAGE, applicationType);
    return ebsApiWebClient
            .get()
            .uri(builder -> builder.path("/lookup/amendment-types")
                    .queryParamIfPresent("application-type",
                            Optional.ofNullable(applicationType))
                    .build())
            .retrieve()
            .bodyToMono(AmendmentTypeLookupDetail.class)
            .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
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
            .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, COUNTRY_ERROR_MESSAGE));
  }

  /**
   * Retrieves prior authority types by code and valueRequired flag.
   *
   * @param code - the prior authority type code
   * @param valueRequired - the value required flag
   * @return A Mono containing the PriorAuthorityTypeDetails or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes(
      final String code,
      final Boolean valueRequired) {
    final String errorMessage =
        String.format(PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE, code, valueRequired);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/prior-authority-types")
            .queryParamIfPresent("code",
                Optional.ofNullable(code))
            .queryParamIfPresent("value-required",
                Optional.ofNullable(valueRequired))
            .build())
        .retrieve()
        .bodyToMono(PriorAuthorityTypeDetails.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieve the list of users for a given Provider.
   *
   * @param providerId the provider id.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetails> getUsers(final Integer providerId) {
    final String errorMessage = String.format(USERS_ERROR_MESSAGE, providerId);
    return  ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/users")
            .queryParamIfPresent("provider-id",
                Optional.ofNullable(providerId))
            .build())
        .retrieve()
        .bodyToMono(UserDetails.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));

  }

  /**
   * Retrieves proceeding detail for the supplied proceeding code.
   *
   * @param proceedingCode - the proceeding code.
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<ProceedingDetail> getProceeding(final String proceedingCode) {
    final String errorMessage = String.format(PROCEEDING_ERROR_MESSAGE, proceedingCode);
    return ebsApiWebClient
        .get()
        .uri("/proceedings/{proceeding-code}", proceedingCode)
        .retrieve()
        .bodyToMono(ProceedingDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves proceeding details.
   *
   * @return A Mono containing the ProceedingDetails or an error handler if an error occurs.
   */
  public Mono<ProceedingDetails> getProceedings(
      final ProceedingDetail searchCriteria,
      final Boolean larScopeFlag,
      final String applicationType,
      final Boolean isLead) {
    final String errorMessage =
        String.format(PROCEEDINGS_ERROR_MESSAGE,
            searchCriteria.getCategoryOfLawCode(),
            searchCriteria.getMatterType(),
            searchCriteria.getAmendmentOnly(),
            larScopeFlag,
            applicationType,
            isLead);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/proceedings")
            .queryParamIfPresent("category-of-law",
                Optional.ofNullable(searchCriteria.getCategoryOfLawCode()))
            .queryParamIfPresent("matter-type",
                Optional.ofNullable(searchCriteria.getMatterType()))
            .queryParamIfPresent("amendment-only",
                Optional.ofNullable(searchCriteria.getAmendmentOnly()))
            .queryParamIfPresent("lar-scope-flag",
                Optional.ofNullable(larScopeFlag))
            .queryParamIfPresent("application-type",
                Optional.ofNullable(applicationType))
            .queryParamIfPresent("lead",
                Optional.ofNullable(isLead))
            .queryParam("size", 1000)
            .build())
        .retrieve()
        .bodyToMono(ProceedingDetails.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves Client involvement types.
   *
   * @return A Mono containing the ClientInvolvementTypeLookupDetail or an error handler if an error
   *         occurs.
   */
  public Mono<ClientInvolvementTypeLookupDetail> getClientInvolvementTypes(
      final String proceedingCode) {
    final String errorMessage = String.format(CLIENT_INVOLVEMENT_ERROR_MESSAGE, proceedingCode);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/proceeding-client-involvement-types")
            .queryParamIfPresent("proceeding-code", Optional.ofNullable(proceedingCode))
            .queryParam("size", 1000)
            .build())
        .retrieve()
        .bodyToMono(ClientInvolvementTypeLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves Proceeding level of service types.
   *
   * @return A Mono containing the LevelOfServiceLookupDetail or an error handler if an error
   *         occurs.
   */
  public Mono<LevelOfServiceLookupDetail> getLevelOfServiceTypes(
      final String proceedingCode,
      final String categoryOfLaw,
      final String matterType) {
    final String errorMessage = String.format(LEVEL_OF_SERVICE_ERROR_MESSAGE,
        proceedingCode, categoryOfLaw, matterType);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/level-of-service")
            .queryParamIfPresent("proceeding-code", Optional.ofNullable(proceedingCode))
            .queryParamIfPresent("category-of-law", Optional.ofNullable(categoryOfLaw))
            .queryParamIfPresent("matter-type", Optional.ofNullable(matterType))
            .queryParam("size", 1000)
            .build())
        .retrieve()
        .bodyToMono(LevelOfServiceLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves scope limitation details which match the provided example ScopeLimitationDetail.
   *
   * @param scopeLimitationDetail - the scope limitation search criteria.
   * @return A Mono containing the ScopeLimitationDetails or an error handler if an error occurs.
   */
  public Mono<ScopeLimitationDetails> getScopeLimitations(
      final ScopeLimitationDetail scopeLimitationDetail) {
    final String errorMessage = String.format(SCOPE_LIMITATIONS_ERROR_MESSAGE,
        scopeLimitationDetail.getScopeLimitations(),
        scopeLimitationDetail.getCategoryOfLaw(),
        scopeLimitationDetail.getMatterType(),
        scopeLimitationDetail.getProceedingCode(),
        scopeLimitationDetail.getLevelOfService(),
        scopeLimitationDetail.getDefaultWording(),
        scopeLimitationDetail.getStage(),
        scopeLimitationDetail.getCostLimitation(),
        scopeLimitationDetail.getEmergencyCostLimitation(),
        scopeLimitationDetail.getNonStandardWordingRequired(),
        scopeLimitationDetail.getEmergencyScopeDefault(),
        scopeLimitationDetail.getEmergency(),
        scopeLimitationDetail.getDefaultCode(),
        scopeLimitationDetail.getScopeDefault());
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
            .queryParamIfPresent("emergency",
                Optional.ofNullable(scopeLimitationDetail.getEmergency()))
            .queryParamIfPresent("default-code",
                Optional.ofNullable(scopeLimitationDetail.getDefaultCode()))
            .queryParamIfPresent("scope-default",
                Optional.ofNullable(scopeLimitationDetail.getScopeDefault()))
            .queryParam("size", 1000)
            .build())
        .retrieve()
        .bodyToMono(ScopeLimitationDetails.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves outcome result lookup detail based on the provided proceedingCode and
   * outcomeResult values.
   *
   * @param proceedingCode - the proceeding code.
   * @param outcomeResult - the outcome result value.
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<OutcomeResultLookupDetail> getOutcomeResults(
      final String proceedingCode,
      final String outcomeResult) {
    final String errorMessage =
        String.format(OUTCOME_RESULTS_ERROR_MESSAGE, proceedingCode, outcomeResult);
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
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves stage end lookup detail based on the provided proceedingCode and
   * stageEnd values.
   *
   * @param proceedingCode - the proceeding code.
   * @param stageEnd - the stage end value.
   * @return A Mono containing the StageEndLookupDetail or an error handler if an error occurs.
   */
  public Mono<StageEndLookupDetail> getStageEnds(
      final String proceedingCode,
      final String stageEnd) {
    final String errorMessage = String.format(STAGE_END_ERROR_MESSAGE, proceedingCode, stageEnd);
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
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves award type lookup detail based on the provided code and
   * award type values.
   *
   * @param code - the award type code.
   * @param awardType - the award type value.
   * @return A Mono containing the AwardTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes(
      final String code,
      final String awardType) {
    final String errorMessage = String.format(AWARD_TYPE_ERROR_MESSAGE, code, awardType);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/award-types")
            .queryParamIfPresent("code",
                Optional.ofNullable(code))
            .queryParamIfPresent("award-type",
                Optional.ofNullable(awardType))
            .build())
        .retrieve()
        .bodyToMono(AwardTypeLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves category of law lookup detail based on the provided code,
   * matter type description, and copy cost limit values.
   *
   * @param code - the category of law code.
   * @param matterTypeDescription - the matter type description value.
   * @param copyCostLimit - the copy cost limit flag.
   * @return A Mono containing the CategoryOfLawLookupDetail or an error handler if an error occurs.
   */
  public Mono<CategoryOfLawLookupDetail> getCategoriesOfLaw(
      final String code,
      final String matterTypeDescription,
      final Boolean copyCostLimit) {
    final String errorMessage = String.format(CATEGORIES_OF_LAW_ERROR_MESSAGE,
        code, matterTypeDescription, copyCostLimit);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/categories-of-law")
            .queryParamIfPresent("code",
                Optional.ofNullable(code))
            .queryParamIfPresent("matter-type-description",
                Optional.ofNullable(matterTypeDescription))
            .queryParamIfPresent("copy-cost-limit",
                Optional.ofNullable(copyCostLimit))
            .build())
        .retrieve()
        .bodyToMono(CategoryOfLawLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }

  /**
   * Retrieves person to case relationship lookup detail based on the provided code
   * and description values.
   *
   * @param code - the relationship code.
   * @param description - the relationship description value.
   * @return A Mono containing RelationshipToCaseLookupDetail or error handler if an error occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getPersonToCaseRelationships(
      final String code,
      final String description) {
    final String errorMessage = String.format(PERSON_RELATIONSHIP_TO_CASE_ERROR_MESSAGE,
        code, description);
    return ebsApiWebClient
        .get()
        .uri(builder -> builder.path("/lookup/person-to-case-relationships")
            .queryParamIfPresent("code",
                Optional.ofNullable(code))
            .queryParamIfPresent("description",
                Optional.ofNullable(description))
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(e -> apiClientErrorHandler.handleEbsApiError(e, errorMessage));
  }
}

