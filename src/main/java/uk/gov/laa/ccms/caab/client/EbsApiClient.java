package uk.gov.laa.ccms.caab.client;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseDetails;
import uk.gov.laa.ccms.data.model.CaseReferenceSummary;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.NotificationSummary;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.TransactionStatus;

/**
 * Client class responsible for interacting with the ebs-api microservice to retrieve various data
 * entities.
 */
@Service
@Slf4j
public class EbsApiClient extends BaseApiClient {

  private final EbsApiClientErrorHandler ebsApiClientErrorHandler;

  protected EbsApiClient(WebClient ebsApiWebClient,
      EbsApiClientErrorHandler ebsApiClientErrorHandler) {
    super(ebsApiWebClient);
    this.ebsApiClientErrorHandler = ebsApiClientErrorHandler;
  }

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(final String loginId) {
    return webClient
        .get()
        .uri("/users/{loginId}", loginId)
        .retrieve()
        .bodyToMono(UserDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "User", "login id", loginId));
  }

  /**
   * Retrieves a summary of notification counts for a user based on their login ID.
   *
   * @param loginId the login ID of the user whose notification summary is to be retrieved
   * @return a Mono emitting the NotificationSummary for the specified user
   */
  public Mono<NotificationSummary> getUserNotificationSummary(final String loginId) {
    return webClient
        .get()
        .uri("/users/{loginId}/notifications/summary", loginId)
        .retrieve()
        .bodyToMono(NotificationSummary.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "User", "login id", loginId));
  }

  /**
   * Retrieves details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the ProviderDetail or an error handler if an error occurs.
   */
  public Mono<ProviderDetail> getProvider(final Integer providerId) {

    return webClient
        .get()
        .uri("/providers/{providerId}", String.valueOf(providerId))
        .retrieve()
        .bodyToMono(ProviderDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Provider", "id", String.valueOf(providerId)));
  }

  /**
   * Retrieves common lookup values based on the type, code, description and sort criteria.
   *
   * @param type        The type of the common lookup values. Can be null.
   * @param code        The code of the common lookup values. Can be null.
   * @param description The description of the common lookup values. Can be null.
   * @param sort        The sort criteria for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(final String type, final String code,
      final String description, final String sort) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(type)
        .ifPresent(param -> queryParams.add("type", param));
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(description)
        .ifPresent(param -> queryParams.add("description", param));
    Optional.ofNullable(sort)
        .ifPresent(param -> queryParams.add("sort", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/common")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CommonLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Common values", queryParams));
  }

  /**
   * Retrieves common lookup values based on the type, code, and description.
   *
   * @param type        The type of the common lookup values. Can be null.
   * @param code        The code of the common lookup values. Can be null.
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
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(categoryOfLaw)
        .ifPresent(param -> queryParams.add("category-of-law", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/matter-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(MatterTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Matter types", queryParams));
  }


  /**
   * Retrieves the person to case relationships lookup values.
   *
   * @return A Mono containing the RelationshipToCaseLookupDetail or an error handler if an error
   *     occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getPersonRelationshipsToCaseValues() {
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/person-to-case-relationships")
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler
            .handleApiRetrieveError(e, "Person relationship to case", null));
  }

  /**
   * Retrieves the organisation to case relationships lookup values, optionally filtered on code and
   * description value.
   *
   * @param code        - the relationship code.
   * @param description - the relationship description value.
   * @return A Mono containing the RelationshipToCaseLookupDetail or an error handler if an error
   *     occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getOrganisationToCaseRelationshipValues(
      final String code,
      final String description) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(description)
        .ifPresent(param -> queryParams.add("description", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/organisation-to-case-relationships")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Organisation relationship to case", queryParams));
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(
      final Boolean copyAllowed) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(copyAllowed)
        .ifPresent(param -> queryParams.add("copy-allowed", String.valueOf(param)));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/case-status")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CaseStatusLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Case status", queryParams));
  }

  /**
   * Retrieves amendment type lookup details based on the provided application type.
   *
   * @param applicationType The application type to retrieve amendment types for.
   * @return A Mono containing the AmendmentTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(
      final String applicationType) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(applicationType)
        .ifPresent(param -> queryParams.add("application-type", param));
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/amendment-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(AmendmentTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Amendment types", queryParams));
  }

  /**
   * Retrieves country lookup details.
   *
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCountries() {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/countries")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CommonLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Countries", queryParams));
  }

  /**
   * Retrieves prior authority types by code and valueRequired flag.
   *
   * @param code          - the prior authority type code
   * @param valueRequired - the value required flag
   * @return A Mono containing the PriorAuthorityTypeDetails or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes(
      final String code,
      final Boolean valueRequired) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(valueRequired)
        .ifPresent(param -> queryParams.add("value-required", String.valueOf(valueRequired)));
    return webClient
        .get()
        .uri(builder -> builder.path("/prior-authority-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(PriorAuthorityTypeDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Prior authority types", queryParams));
  }

  /**
   * Retrieve the list of users for a given Provider.
   *
   * @param providerId the provider id.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetails> getUsers(final Integer providerId) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", String.valueOf(param)));
    return webClient
        .get()
        .uri(builder -> builder.path("/users")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(UserDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Users", queryParams));

  }

  /**
   * Retrieves proceeding detail for the supplied proceeding code.
   *
   * @param proceedingCode - the proceeding code.
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<ProceedingDetail> getProceeding(final String proceedingCode) {
    return webClient
        .get()
        .uri("/proceedings/{proceeding-code}", proceedingCode)
        .retrieve()
        .bodyToMono(ProceedingDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Proceedings", "proceeding code", proceedingCode));
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

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(searchCriteria.getCategoryOfLawCode())
        .ifPresent(code -> queryParams.add("category-of-law", code));
    Optional.ofNullable(searchCriteria.getMatterType())
        .ifPresent(type -> queryParams.add("matter-type", type));
    Optional.ofNullable(searchCriteria.getAmendmentOnly())
        .ifPresent(param -> queryParams.add("amendment-only", String.valueOf(param)));
    Optional.ofNullable(larScopeFlag)
        .ifPresent(param -> queryParams.add("lar-scope-flag", String.valueOf(param)));
    Optional.ofNullable(applicationType)
        .ifPresent(type -> queryParams.add("application-type", type));
    Optional.ofNullable(isLead)
        .ifPresent(param -> queryParams.add("lead", String.valueOf(param)));

    return webClient
        .get()
        .uri(builder -> builder.path("/proceedings")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(ProceedingDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Proceedings", queryParams));
  }

  /**
   * Retrieves Client involvement types.
   *
   * @return A Mono containing the ClientInvolvementTypeLookupDetail or an error handler if an error
   *     occurs.
   */
  public Mono<ClientInvolvementTypeLookupDetail> getClientInvolvementTypes(
      final String proceedingCode) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(proceedingCode)
        .ifPresent(code -> queryParams.add("proceeding-code", code));
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/proceeding-client-involvement-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(ClientInvolvementTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Client involvement types", queryParams));
  }

  /**
   * Retrieves Proceeding level of service types.
   *
   * @return A Mono containing the LevelOfServiceLookupDetail or an error handler if an error
   *     occurs.
   */
  public Mono<LevelOfServiceLookupDetail> getLevelOfServiceTypes(
      final String proceedingCode,
      final String categoryOfLaw,
      final String matterType) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(proceedingCode)
        .ifPresent(param -> queryParams.add("proceeding-code", param));
    Optional.ofNullable(categoryOfLaw)
        .ifPresent(param -> queryParams.add("category-of-law", param));
    Optional.ofNullable(matterType)
        .ifPresent(param -> queryParams.add("matter-type", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/level-of-service")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(LevelOfServiceLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Level of service", queryParams));
  }

  /**
   * Retrieves scope limitation details which match the provided example ScopeLimitationDetail.
   *
   * @param scopeLimitationDetail - the scope limitation search criteria.
   * @return A Mono containing the ScopeLimitationDetails or an error handler if an error occurs.
   */
  public Mono<ScopeLimitationDetails> getScopeLimitations(
      final ScopeLimitationDetail scopeLimitationDetail) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(scopeLimitationDetail.getScopeLimitations())
        .ifPresent(scopeLimitations -> queryParams.add("scope-limitations", scopeLimitations));
    Optional.ofNullable(scopeLimitationDetail.getCategoryOfLaw())
        .ifPresent(categoryOfLaw -> queryParams.add("category-of-law", categoryOfLaw));
    Optional.ofNullable(scopeLimitationDetail.getMatterType())
        .ifPresent(matterType -> queryParams.add("matter-type", matterType));
    Optional.ofNullable(scopeLimitationDetail.getProceedingCode())
        .ifPresent(proceedingCode -> queryParams.add("proceeding-code", proceedingCode));
    Optional.ofNullable(scopeLimitationDetail.getLevelOfService())
        .ifPresent(levelOfService -> queryParams.add("level-of-service", levelOfService));
    Optional.ofNullable(scopeLimitationDetail.getDefaultWording())
        .ifPresent(defaultWording -> queryParams.add("default-wording", defaultWording));
    Optional.ofNullable(scopeLimitationDetail.getStage())
        .ifPresent(stage -> queryParams.add("stage", String.valueOf(stage)));
    Optional.ofNullable(scopeLimitationDetail.getCostLimitation())
        .ifPresent(param -> queryParams.add("cost-limitation", String.valueOf(param)));
    Optional.ofNullable(scopeLimitationDetail.getEmergencyCostLimitation())
        .ifPresent(param -> queryParams.add(
            "emergency-cost-limitation", String.valueOf(param)));
    Optional.ofNullable(scopeLimitationDetail.getNonStandardWordingRequired())
        .ifPresent(param -> queryParams.add(
            "non-standard-wording", String.valueOf(param)));
    Optional.ofNullable(scopeLimitationDetail.getEmergencyScopeDefault())
        .ifPresent(param -> queryParams.add(
            "emergency-scope-default", String.valueOf(param)));
    Optional.ofNullable(scopeLimitationDetail.getEmergency())
        .ifPresent(param -> queryParams.add("emergency", String.valueOf(param)));
    Optional.ofNullable(scopeLimitationDetail.getDefaultCode())
        .ifPresent(defaultCode -> queryParams.add("default-code", String.valueOf(defaultCode)));
    Optional.ofNullable(scopeLimitationDetail.getScopeDefault())
        .ifPresent(scopeDefault -> queryParams.add("scope-default", String.valueOf(scopeDefault)));

    return webClient
        .get()
        .uri(builder -> builder.path("/scope-limitations")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(ScopeLimitationDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Scope limitations", queryParams));
  }

  /**
   * Retrieves outcome result lookup detail based on the provided proceedingCode and outcomeResult
   * values.
   *
   * @param proceedingCode - the proceeding code.
   * @param outcomeResult  - the outcome result value.
   * @return A Mono containing the ProceedingDetail or an error handler if an error occurs.
   */
  public Mono<OutcomeResultLookupDetail> getOutcomeResults(
      final String proceedingCode,
      final String outcomeResult) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(proceedingCode)
        .ifPresent(param -> queryParams.add("proceeding-code", param));
    Optional.ofNullable(outcomeResult)
        .ifPresent(param -> queryParams.add("outcome-result", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/outcome-results")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(OutcomeResultLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Outcome results", queryParams));
  }

  /**
   * Retrieves stage end lookup detail based on the provided proceedingCode and stageEnd values.
   *
   * @param proceedingCode - the proceeding code.
   * @param stageEnd       - the stage end value.
   * @return A Mono containing the StageEndLookupDetail or an error handler if an error occurs.
   */
  public Mono<StageEndLookupDetail> getStageEnds(
      final String proceedingCode,
      final String stageEnd) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(proceedingCode)
        .ifPresent(param -> queryParams.add("proceeding-code", param));
    Optional.ofNullable(stageEnd)
        .ifPresent(param -> queryParams.add("stage-end", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/stage-ends")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(StageEndLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Stage ends", queryParams));
  }

  /**
   * Retrieves award type lookup detail based on the provided code and award type values.
   *
   * @param code      - the award type code.
   * @param awardType - the award type value.
   * @return A Mono containing the AwardTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes(
      final String code,
      final String awardType) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(awardType)
        .ifPresent(param -> queryParams.add("award-type", param));
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/award-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(AwardTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Award types", queryParams));
  }

  /**
   * Retrieves category of law lookup detail based on the provided code, matter type description,
   * and copy cost limit values.
   *
   * @param code                  - the category of law code.
   * @param matterTypeDescription - the matter type description value.
   * @param copyCostLimit         - the copy cost limit flag.
   * @return A Mono containing the CategoryOfLawLookupDetail or an error handler if an error occurs.
   */
  public Mono<CategoryOfLawLookupDetail> getCategoriesOfLaw(
      final String code,
      final String matterTypeDescription,
      final Boolean copyCostLimit) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(matterTypeDescription)
        .ifPresent(param -> queryParams.add("matter-type-description", param));
    Optional.ofNullable(copyCostLimit)
        .ifPresent(param -> queryParams.add("copy-cost-limit", String.valueOf(param)));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/categories-of-law")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CategoryOfLawLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(e,
            "Categories of law", queryParams));
  }

  /**
   * Retrieves person to case relationship lookup detail based on the provided code and description
   * values.
   *
   * @param code        - the relationship code.
   * @param description - the relationship description value.
   * @return A Mono containing RelationshipToCaseLookupDetail or error handler if an error occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getPersonToCaseRelationships(
      final String code,
      final String description) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));
    Optional.ofNullable(description)
        .ifPresent(param -> queryParams.add("description", param));
    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/person-to-case-relationships")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(RelationshipToCaseLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Person to case relationships", queryParams));
  }

  /**
   * Retrieves evidence document type lookup detail based on the provided type and code values.
   *
   * @param type - the subset of evidence document types to retrieve.
   * @param code - the evidence document type code.
   * @return A Mono containing EvidenceDocumentTypeLookupDetail or error handler if an error occurs.
   */
  public Mono<EvidenceDocumentTypeLookupDetail> getEvidenceDocumentTypes(
      final String type,
      final String code) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(type)
        .ifPresent(param -> queryParams.add("type", param));
    Optional.ofNullable(code)
        .ifPresent(param -> queryParams.add("code", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/evidence-document-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(EvidenceDocumentTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Evidence document types", queryParams));
  }

  /**
   * Retrieves assessment summary attributes based on the provided summary type.
   *
   * @param summaryType the type of the summary to retrieve attributes for
   * @return a Mono emitting the assessment summary attributes
   */
  public Mono<AssessmentSummaryEntityLookupDetail> getAssessmentSummaryAttributes(
      final String summaryType) {

    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(summaryType)
        .ifPresent(param -> queryParams.add("summary-type", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/assessment-summary-attributes")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(AssessmentSummaryEntityLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Assessment summary attributes", queryParams));
  }


  /**
   * Retrieves declaration details based on the provided type and bill type. Constructs a query with
   * the given parameters and sends a GET request to the API.
   *
   * @param type     the type of declaration to retrieve
   * @param billType the bill type to filter the declarations, may be null
   * @return a Mono emitting the {@link DeclarationLookupDetail} or handling errors
   */
  public Mono<DeclarationLookupDetail> getDeclarations(
      final String type,
      final String billType) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();
    Optional.ofNullable(type)
        .ifPresent(param -> queryParams.add("type", param));
    Optional.ofNullable(billType)
        .ifPresent(param -> queryParams.add("billType", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/declarations")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(DeclarationLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Declarations", queryParams));
  }

  /**
   * Retrieves declaration details based on the provided type and bill type. Constructs a query with
   * the given parameters and sends a GET request to the API.
   *
   * @param type          the type of provider requests to retrieve
   * @param isCaseRelated the case related filter, may be null to retrieve all
   * @return a Mono emitting the {@link ProviderRequestTypeLookupDetail} or handling errors
   */
  public Mono<ProviderRequestTypeLookupDetail> getProviderRequestTypes(
      final Boolean isCaseRelated,
      final String type) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();

    Optional.ofNullable(isCaseRelated)
        .ifPresent(param -> queryParams.add("is-case-related", String.valueOf(param)));
    Optional.ofNullable(type)
        .ifPresent(param -> queryParams.add("type", param));

    return webClient
        .get()
        .uri(builder -> builder.path("/lookup/provider-request-types")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(ProviderRequestTypeLookupDetail.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Provider request types", queryParams));
  }

  /**
   * Allocates the next available case reference by sending a POST request to the external case
   * reference service.
   *
   * @return a {@link Mono} emitting the {@link CaseReferenceSummary} containing the details of the
   *     next allocated case reference
   */
  public Mono<CaseReferenceSummary> postAllocateNextCaseReference() {
    return webClient
        .post()
        .uri("/case-reference")
        .retrieve()
        .bodyToMono(CaseReferenceSummary.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "case reference", null));
  }

  /**
   * Retrieves a list of notifications based on the specified search criteria, pagination, and
   * sorting options.
   *
   * @param criteria the criteria used to filter the notifications, such as case reference, client
   *                 surname, assigned user ID, or other relevant attributes
   * @param page     the page number to retrieve in the paginated result set
   * @param pageSize the number of notifications to retrieve per page
   * @return a {@code Mono<Notifications>} containing the retrieved notifications that match the
   *     search criteria
   */
  public Mono<Notifications> getNotifications(
      final NotificationSearchCriteria criteria,
      final Integer page,
      final Integer pageSize) {
    final MultiValueMap<String, String> queryParams = buildQueryParams(
        criteria, page, pageSize);

    return webClient
        .get()
        .uri(builder -> builder.path("/notifications")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(Notifications.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Notifications", queryParams));
  }

  /**
   * Retrieves case details based on the specified search criteria, provider ID,
   * pagination parameters, and returns the results as a Mono of CaseDetails.
   *
   * @param caseSearchCriteria the criteria used to filter the cases
   * @param providerId the ID of the provider to retrieve cases for
   * @param page the page number to retrieve
   * @param size the number of items per page; can be null to use default
   * @return a Mono containing the retrieved case details
   */
  public Mono<CaseDetails> getCases(final CaseSearchCriteria caseSearchCriteria,
      final int providerId, final int page, final Integer size) {
    final MultiValueMap<String, String> queryParams =
        buildQueryParams(caseSearchCriteria, providerId, page, size);

    return webClient
        .get()
        .uri(builder -> builder.path("/cases")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(CaseDetails.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "Cases", queryParams));
  }

  /**
   * Fetches the transaction status for a client transaction.
   *
   * @param transactionId         The transaction id for the client transaction in soa.
   * @return A Mono wrapping the TransactionStatus.
   */
  public Mono<TransactionStatus> getClientStatus(
      final String transactionId) {
    return webClient
        .get()
        .uri("/clients/status/{transactionId}", transactionId)
        .retrieve()
        .bodyToMono(TransactionStatus.class)
        .onErrorResume(e -> ebsApiClientErrorHandler.handleApiRetrieveError(
            e, "client transaction status", "transaction id", transactionId));

  }

  private static MultiValueMap<String, String> buildQueryParams(
      final NotificationSearchCriteria criteria, final Integer page, final Integer pageSize) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();

    addQueryParam(queryParams, "case-reference-number", criteria.getCaseReference());
    addQueryParam(queryParams, "provider-case-reference", criteria.getProviderCaseReference());
    addQueryParam(queryParams, "assigned-to-user-id", criteria.getAssignedToUserId());
    addQueryParam(queryParams, "client-surname", criteria.getClientSurname());
    addQueryParam(queryParams, "fee-earner-id", criteria.getFeeEarnerId());
    addQueryParam(queryParams, "include-closed", criteria.isIncludeClosed());
    addQueryParam(queryParams, "notification-type", criteria.getNotificationType());
    addQueryParam(queryParams, "date-from", criteria.getNotificationFromDate());
    addQueryParam(queryParams, "date-to", criteria.getNotificationToDate());
    addQueryParam(queryParams, "page", page);
    addQueryParam(queryParams, "size", pageSize);
    addQueryParam(queryParams, "sort", criteria.getSort());
    return queryParams;
  }

  private static MultiValueMap<String, String> buildQueryParams(
      final CaseSearchCriteria criteria, final int providerId, final Integer page,
      final Integer pageSize) {
    final MultiValueMap<String, String> queryParams = createDefaultQueryParams();

    addQueryParam(queryParams, "provider-id", providerId);
    addQueryParam(queryParams, "case-reference-number", criteria.getCaseReference());
    addQueryParam(queryParams, "provider-case-reference", criteria.getProviderCaseReference());
    addQueryParam(queryParams, "case-status", criteria.getStatus());
    addQueryParam(queryParams, "fee-earner-id", criteria.getFeeEarnerId());
    addQueryParam(queryParams, "office-id", criteria.getOfficeId());
    addQueryParam(queryParams, "client-surname", criteria.getClientSurname());
    addQueryParam(queryParams, "page", page);
    addQueryParam(queryParams, "size", pageSize);
    return queryParams;
  }


}

