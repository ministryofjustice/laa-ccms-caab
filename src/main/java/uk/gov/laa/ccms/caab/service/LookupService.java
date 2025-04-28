package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_COURTS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;

/**
 * Service class to handle Common Lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupService {
  private final EbsApiClient ebsApiClient;

  /**
   * Get a list of Country Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values. Remove all null objects due
   *         to data returned from ebs.
   */
  public Mono<CommonLookupDetail> getCountries() {
    return ebsApiClient.getCountries()
        .flatMap(countries -> {
          if (countries != null) {
            final List<CommonLookupValueDetail> filteredContent = countries
                .getContent()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            return Mono.just(new CommonLookupDetail().content(filteredContent));
          } else {
            return Mono.just(new CommonLookupDetail().content(Collections.emptyList()));
          }
        });
  }

  /**
   * Get a Country Common Value.
   *
   * @param code The country code to look up
   * @return Optional CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<Optional<CommonLookupValueDetail>> getCountry(final String code) {
    return getCountries()
        .map(commonLookupDetail -> commonLookupDetail
              .getContent()
              .stream()
              .filter(Objects::nonNull)
              .filter(countryDetail -> code.equals(countryDetail.getCode()))
              .findFirst());
  }

  /**
   * Get a list of matter types for proceedings.
   *
   * @return MatterTypeLookupDetail containing the matterType values.
   */
  public Mono<MatterTypeLookupDetail> getMatterTypes(final String categoryOfLaw) {
    return ebsApiClient.getMatterTypes(categoryOfLaw);
  }

  /**
   * Get a list of proceeding types for proceedings.
   *
   * @param searchCriteria The criteria to search for proceedings.
   * @param larScopeFlag The flag to indicate if the scope is for Legal Aid Representation.
   * @param applicationType The type of the application.
   * @param isLead A flag to indicate if the proceeding is lead.
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<ProceedingDetails> getProceedings(
      final ProceedingDetail searchCriteria,
      final Boolean larScopeFlag,
      final String applicationType,
      final Boolean isLead) {

    return ebsApiClient.getProceedings(
        searchCriteria,
        larScopeFlag,
        applicationType,
        isLead);
  }

  /**
   * Retrieves Client involvement types with detailed parameters.
   *
   * @param proceedingCode The proceeding code.
   * @return A Mono containing the ClientInvolvementTypeLookupDetail or an error handler if an
   *         error occurs.
   */
  public Mono<ClientInvolvementTypeLookupDetail> getProceedingClientInvolvementTypes(
      final String proceedingCode) {
    return ebsApiClient.getClientInvolvementTypes(proceedingCode);
  }

  /**
   * Retrieves the level of service types for a given proceeding.
   * The level of service types are fetched based on the proceeding code, category of law, and
   * matter type.
   *
   * @param categoryOfLaw The category of law.
   * @param proceedingCode The code of the proceeding.
   * @param matterType The type of the matter.
   * @return A Mono of LevelOfServiceLookupDetail containing the level of service types.
   */
  public Mono<LevelOfServiceLookupDetail> getProceedingLevelOfServiceTypes(
      final String categoryOfLaw,
      final String proceedingCode,
      final String matterType) {

    return ebsApiClient.getLevelOfServiceTypes(proceedingCode, categoryOfLaw, matterType);
  }

  /**
   * Get the order type description, filtered by code.
   *
   * @return String containing the description of an order type.
   */
  public Mono<String> getOrderTypeDescription(final String code) {
    return getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE)
        .map(commonLookupDetail -> commonLookupDetail.getContent().stream()
            .filter(commonLookupValueDetail -> commonLookupValueDetail.getCode().equals(code))
            .findFirst()
            .map(CommonLookupValueDetail::getDescription)
            .orElse(code));
  }

  /**
   * Get a list of Scope Limitation Detail based on the search criteria.
   *
   * @param searchCriteria - the criteria to search for Scope Limitation Details.
   * @return ScopeLimitationDetails containing the scope limitation details.
   */
  public Mono<ScopeLimitationDetails> getScopeLimitationDetails(
      final ScopeLimitationDetail searchCriteria) {
    return ebsApiClient.getScopeLimitations(searchCriteria);
  }

  /**
   * Retrieves court details.
   * A wildcard match is performed for both courtCode and description to return all Courts
   * which contain the provided values.
   *
   * @param courtCode - the court code value.
   * @param description - the court description.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCourts(
      final String courtCode, final String description) {
    return ebsApiClient.getCommonValues(
        COMMON_VALUE_COURTS,
        StringUtils.hasText(courtCode) ? "*%s*".formatted(courtCode) : null,
        StringUtils.hasText(description) ? "*%s*".formatted(description.toUpperCase()) : null);
  }

  /**
   * Retrieves court details.
   * A wildcard match is performed for courtCode to return all Courts
   * which contain the provided value.
   *
   * @param courtCode - the court code.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCourts(
      final String courtCode) {
    return this.getCourts(courtCode, null);
  }

  /**
   * Retrieves outcome results data based on the supplied proceedingCode and outcomeResult value.
   *
   * @param proceedingCode - the proceeding code.
   * @param outcomeResult - the outcome result.
   * @return A Mono containing the OutcomeResultLookupDetail or an error handler if an error occurs.
   */
  public Mono<OutcomeResultLookupDetail> getOutcomeResults(
      final String proceedingCode,
      final String outcomeResult) {
    return ebsApiClient.getOutcomeResults(proceedingCode, outcomeResult);
  }

  /**
   * Retrieves stage end data based on the supplied proceedingCode and stageEnd value.
   *
   * @param proceedingCode - the proceeding code.
   * @param stageEnd - the stage end value.
   * @return A Mono containing the StageEndLookupDetail or an error handler if an error occurs.
   */
  public Mono<StageEndLookupDetail> getStageEnds(
      final String proceedingCode,
      final String stageEnd) {
    return ebsApiClient.getStageEnds(proceedingCode, stageEnd);
  }

  /**
   * Retrieves all prior authority types.
   *
   * @return A Mono containing the PriorAuthorityLookupDetail
   *     or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes() {
    return this.getPriorAuthorityTypes(null, null);
  }

  /**
   * Retrieves prior authority types matching the specified code and valueRequired flag.
   *
   * @param code - the prior authority code.
   * @param valueRequired - the value required flag.
   * @return A Mono containing the PriorAuthorityLookupDetail
   *     or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes(
      final String code, final Boolean valueRequired) {
    return ebsApiClient.getPriorAuthorityTypes(code, valueRequired);
  }

  /**
   * Retrieve a single prior authority type matching the specified code.
   *
   * @param code - the prior authority code.
   * @return A Mono containing an Optional PriorAuthorityTypeDetail
   *     or an error handler if an error occurs.
   */
  public Mono<Optional<PriorAuthorityTypeDetail>> getPriorAuthorityType(
      final String code) {
    return this.getPriorAuthorityTypes(code, null)
        .mapNotNull(priorAuthorityTypeDetails -> priorAuthorityTypeDetails.getContent()
            .stream()
            .findFirst());
  }

  /**
   * Retrieves all award types.
   *
   * @return A Mono containing the AwardTypeLookupDetail
   *     or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes() {
    return this.getAwardTypes(null, null);
  }

  /**
   * Retrieves award types matching the specified code and awardType values.
   *
   * @param code - the award type code.
   * @param awardType - the award type value.
   * @return A Mono containing the AwardTypeLookupDetail
   *     or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes(
      final String code, final String awardType) {
    return ebsApiClient.getAwardTypes(code, awardType);
  }

  /**
   * Get a list of all Category Of Law Lookup Values.
   *
   * @return CategoryOfLawLookupDetail containing the category of law values.
   */
  public Mono<CategoryOfLawLookupDetail> getCategoriesOfLaw() {
    return ebsApiClient.getCategoriesOfLaw(null, null, null);
  }

  /**
   * Get the Category Of Law Lookup Value with the specified code.
   *
   * @param code - the category of law code.
   * @return Mono containing an Optional category of law.
   */
  public Mono<Optional<CategoryOfLawLookupValueDetail>> getCategoryOfLaw(final String code) {
    return ebsApiClient.getCategoriesOfLaw(code, null, null)
        .mapNotNull(categoryOfLawLookupDetail -> categoryOfLawLookupDetail.getContent().stream()
            .findFirst());
  }

  /**
   * Get a list of all Person Relationship To Case Lookup Values.
   *
   * @return Mono containing all relationship lookup values or null if an error occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getPersonToCaseRelationships() {
    return ebsApiClient.getPersonToCaseRelationships(null, null);
  }

  /**
   * Get a single Person Relationship To Case Lookup Value.
   *
   * @return Mono containing an Optional relationship lookup value.
   */
  public Mono<Optional<RelationshipToCaseLookupValueDetail>> getPersonToCaseRelationship(
      final String code) {
    return ebsApiClient.getPersonToCaseRelationships(code, null)
        .mapNotNull(relationshipToCaseLookupDetail -> relationshipToCaseLookupDetail.getContent()
            .stream().findFirst());
  }

  /**
   * Get a list of all Organisation Relationship To Case Lookup Values.
   *
   * @return Mono containing all relationship lookup values or null if an error occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getOrganisationToCaseRelationships() {
    return ebsApiClient.getOrganisationToCaseRelationshipValues(null, null);
  }

  /**
   * Get a single Organisation Relationship To Case Lookup Value.
   *
   * @return Mono containing the Optional relationship lookup value.
   */
  public Mono<Optional<RelationshipToCaseLookupValueDetail>> getOrganisationToCaseRelationship(
      final String code) {
    return ebsApiClient.getOrganisationToCaseRelationshipValues(code, null)
        .mapNotNull(relationshipToCaseLookupDetail -> relationshipToCaseLookupDetail.getContent()
            .stream().findFirst());
  }

  /**
   * Retrieves all case status values.
   *
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues() {
    return getCaseStatusValues(null);
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(final Boolean copyAllowed) {
    return ebsApiClient.getCaseStatusValues(copyAllowed);
  }

  public Mono<AssessmentSummaryEntityLookupDetail> getAssessmentSummaryAttributes(
      final String summaryType) {
    return ebsApiClient.getAssessmentSummaryAttributes(summaryType);
  }

  /**
   * Get a single common value based on type and code.
   *
   * @param type - the value type.
   * @param code - the value code.
   * @return a Mono containing the Optional CommonLookupValueDetail
   *     or an error handler if an error occurs.
   */
  public Mono<Optional<CommonLookupValueDetail>> getCommonValue(
      final String type, final String code) {
    return ebsApiClient.getCommonValues(type, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst());
  }

  /**
   * Get a common lookup detail based on a type.
   *
   * @param type - the value type.
   * @return a Mono containing the CommonLookupDetail
   */
  public Mono<CommonLookupDetail> getCommonValues(final String type) {
    return ebsApiClient.getCommonValues(type);
  }

  /**
   * Retrieves client lookups based on client flow form data.
   *
   * @param clientFlowFormData The client flow form data.
   * @return A list of pairs containing lookup keys and Mono optional values.
   */
  public List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> getClientLookups(
      final ClientFlowFormData clientFlowFormData) {

    return List.of(
        Pair.of("contactTitle",
            getCommonValue(
                COMMON_VALUE_CONTACT_TITLE,
                clientFlowFormData.getBasicDetails().getTitle())),
        Pair.of("countryOfOrigin",
            getCountry(
                clientFlowFormData.getBasicDetails().getCountryOfOrigin())),
        Pair.of("maritalStatus",
            getCommonValue(
                COMMON_VALUE_MARITAL_STATUS,
                clientFlowFormData.getBasicDetails().getMaritalStatus())),
        Pair.of("gender",
            getCommonValue(
                COMMON_VALUE_GENDER,
                clientFlowFormData.getBasicDetails().getGender())),
        Pair.of("correspondenceMethod",
            getCommonValue(
                COMMON_VALUE_CORRESPONDENCE_METHOD,
                clientFlowFormData.getContactDetails().getCorrespondenceMethod())),
        Pair.of("ethnicity",
            getCommonValue(
                COMMON_VALUE_ETHNIC_ORIGIN,
                clientFlowFormData.getMonitoringDetails().getEthnicOrigin())),
        Pair.of("disability",
            getCommonValue(
                COMMON_VALUE_DISABILITY,
                clientFlowFormData.getMonitoringDetails().getDisability())),

        //Processed differently due to optionality
        Pair.of("country",
            StringUtils.hasText(clientFlowFormData.getAddressDetails().getCountry())
                ? getCountry(
                clientFlowFormData.getAddressDetails().getCountry())
                : Mono.just(Optional.of(new CommonLookupValueDetail()))),
        Pair.of("correspondenceLanguage",
            StringUtils.hasText(clientFlowFormData.getContactDetails().getCorrespondenceLanguage())
                ? getCommonValue(COMMON_VALUE_CORRESPONDENCE_LANGUAGE,
                clientFlowFormData.getContactDetails().getCorrespondenceLanguage())
                : Mono.just(Optional.of(new CommonLookupValueDetail()))));
  }

  /**
   * Adds common lookups to the model and retrieves a list of lookup details.
   *
   * @param lookups The list of pairs containing lookup keys and Mono optional values.
   * @param model The model to which lookups will be added.
   * @return A Mono containing a list of lookup details.
   */
  public Mono<Void> addCommonLookupsToModel(
      final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups,
      final Model model) {
    return Flux.fromIterable(lookups)
        .flatMap(pair -> pair.getRight()
            .map(optionalValue -> optionalValue.orElseGet(CommonLookupValueDetail::new))
            .map(value -> Pair.of(pair.getLeft(), value)))
        .collectList()
        .doOnNext(list -> list.forEach(pair -> model.addAttribute(pair.getLeft(), pair.getRight())))
        .then();
  }



  /**
   * Retrieves a map of common lookups.
   *
   * @param lookups The list of pairs containing lookup keys and Mono optional values.
   * @return A Mono containing a map of lookup details.
   */
  public Mono<HashMap<String, CommonLookupValueDetail>> getCommonLookupsMap(
      final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups) {

    return Flux.fromIterable(lookups)
        .flatMap(pair -> pair.getRight()
            .map(optionalValue -> optionalValue.orElseGet(CommonLookupValueDetail::new))
            .map(value -> Pair.of(pair.getLeft(), value)))
        .collectList()
        .map(list -> {
          final HashMap<String, CommonLookupValueDetail> clientLookupsMap = new HashMap<>();
          list.forEach(pair -> clientLookupsMap.put(pair.getLeft(), pair.getRight()));
          return clientLookupsMap;
        });
  }



  /**
   * Retrieves client summary list lookups.
   *
   * @param clientFlowFormData The client flow form data.
   * @return A Mono containing a map of lookup details.
   */
  public Mono<HashMap<String, CommonLookupValueDetail>> getClientSummaryListLookups(
      final ClientFlowFormData clientFlowFormData) {

    // Create a list of Mono calls and their respective attribute keys
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        getClientLookups(clientFlowFormData);

    // Fetch all Mono's asynchronously
    return getCommonLookupsMap(lookups);
  }

  /**
   * Retrieves the general details submission mapping context.
   * <p>
   * This method combines multiple asynchronous calls to fetch various lookup details required
   * for mapping general details submission summaries. The following components are included:
   * </p>
   * <ul>
   *   <li>preferredAddress - Common lookup detail for the preferred address option</li>
   *   <li>country - Common lookup detail for countries</li>
   * </ul>
   *
   * @return a Mono emitting the GeneralDetailsSubmissionSummaryMappingContext
   */
  public Mono<GeneralDetailsSubmissionSummaryMappingContext>
      getGeneralDetailsSubmissionMappingContext() {
    final Mono<CommonLookupDetail> preferredAddressMono =
        getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION);
    final Mono<CommonLookupDetail> countriesMono =
        getCountries();

    return Mono.zip(
            preferredAddressMono,
            countriesMono)
        .map(tuple -> GeneralDetailsSubmissionSummaryMappingContext.builder()
            .preferredAddress(tuple.getT1())
            .country(tuple.getT2())
            .build());
  }

  /**
   * Retrieves the proceeding submission mapping context.
   * <p>
   * This method fetches the lookup detail for the type of order required for
   * mapping proceeding submission summaries. The following component is included:
   * </p>
   * <ul>
   *   <li>typeOfOrder - Common lookup detail for the type of order</li>
   * </ul>
   *
   * @return a Mono emitting the ProceedingSubmissionSummaryMappingContext
   */
  public Mono<ProceedingSubmissionSummaryMappingContext> getProceedingSubmissionMappingContext() {
    final Mono<CommonLookupDetail> typeOfOrderMono =
        getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE);

    return typeOfOrderMono.map(typeOfOrder ->
        ProceedingSubmissionSummaryMappingContext.builder()
            .typeOfOrder(typeOfOrder)
            .build()
    );
  }

  /**
   * Retrieves the opponent submission mapping context.
   * <p>
   * This method combines multiple asynchronous calls to fetch various lookup details required
   * for mapping opponent submission summaries. The following components are included:
   * </p>
   * <ul>
   *   <li>contactTitle - Common lookup detail for contact titles</li>
   *   <li>organisationRelationshipsToCase - Relationship details for organisations to cases</li>
   *   <li>individualRelationshipsToCase - Relationship details for individuals to cases</li>
   *   <li>relationshipToClient - Common lookup detail for relationships to clients</li>
   * </ul>
   *
   * @return a Mono emitting the OpponentSubmissionSummaryMappingContext
   */
  public Mono<OpponentSubmissionSummaryMappingContext> getOpponentSubmissionMappingContext() {
    final Mono<CommonLookupDetail> contactTitleMono =
        getCommonValues(COMMON_VALUE_CONTACT_TITLE);
    final Mono<RelationshipToCaseLookupDetail> organisationRelationshipsToCaseMono =
        getOrganisationToCaseRelationships();
    final Mono<RelationshipToCaseLookupDetail> individualRelationshipsToCaseMono =
        getPersonToCaseRelationships();
    final Mono<CommonLookupDetail> relationshipToClientMono =
        getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT);

    return Mono.zip(
            contactTitleMono,
            organisationRelationshipsToCaseMono,
            individualRelationshipsToCaseMono,
            relationshipToClientMono)
        .map(tuple -> OpponentSubmissionSummaryMappingContext.builder()
            .contactTitle(tuple.getT1())
            .organisationRelationshipsToCase(tuple.getT2())
            .individualRelationshipsToCase(tuple.getT3())
            .relationshipToClient(tuple.getT4())
            .build());
  }

  /**
   * Retrieves declaration details based only on the submission type.
   *
   * @param submissionType the type of submission for the declaration
   * @return a Mono emitting the declaration lookup details
   */
  public Mono<DeclarationLookupDetail> getDeclarations(
      final String submissionType) {
    return ebsApiClient.getDeclarations(submissionType, null);
  }


  /**
   * Retrieves provider request types based on parameters.
   *
   * @param type the type provider request
   * @param isCaseRelated whether the request is case related
   * @return a Mono emitting the declaration lookup details
   */
  public Mono<ProviderRequestTypeLookupDetail> getProviderRequestTypes(
      final Boolean isCaseRelated,
      final String type) {
    return ebsApiClient.getProviderRequestTypes(isCaseRelated, type);
  }


}
