package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_LINK_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_COURTS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MATTER_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
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
   * Get a list of Application Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getApplicationTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_APPLICATION_TYPE);
  }

  /**
   * Get a single Application Type Common Value.
   *
   * @param code - the code for the application type
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupValueDetail> getApplicationType(final String code) {
    return getCommonValue(COMMON_VALUE_APPLICATION_TYPE, code);
  }

  /**
   * Get a list of Gender Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getGenders() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_GENDER);
  }

  /**
   * Get a Gender Common Values.
   *
   * @param code The code of the common lookup values.
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getGender(final String code) {
    return getCommonValue(COMMON_VALUE_GENDER, code);
  }

  /**
   * Get a list of Unique Identifier Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getUniqueIdentifierTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE);
  }

  /**
   * Get a list of Contact Title Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getContactTitles() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CONTACT_TITLE);
  }

  /**
   * Get a Contact Title Common Value.
   *
   * @param code The contact title code
   * @return CommonLookupDetail containing the contact title lookup
   */
  public Mono<CommonLookupValueDetail> getContactTitle(final String code) {
    return getCommonValue(COMMON_VALUE_CONTACT_TITLE, code);
  }

  /**
   * Get a list of Marital Status Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getMaritalStatuses() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS);
  }

  /**
   * Get a Marital Status Common Value.
   *
   * @param code The marital status code.
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupValueDetail> getMaritalStatus(final String code) {
    return getCommonValue(COMMON_VALUE_MARITAL_STATUS, code);
  }

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
            List<CommonLookupValueDetail> filteredContent = countries
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
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCountry(final String code) {
    return getCountries()
        .flatMap(commonLookupDetail -> {
          Optional<CommonLookupValueDetail> country = commonLookupDetail
              .getContent()
              .stream()
              .filter(Objects::nonNull)
              .filter(countryDetail -> code.equals(countryDetail.getCode()))
              .findFirst();

          return country.map(Mono::just).orElse(Mono.empty());
        });
  }

  /**
   * Get a list of Correspondence Method Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCorrespondenceMethods() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD);
  }

  /**
   * Get a Correspondence Method Common Value.
   *
   * @param code The correspondende method code
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCorrespondenceMethod(final String code) {
    return getCommonValue(COMMON_VALUE_CORRESPONDENCE_METHOD, code);
  }

  /**
   * Get a list of Correspondence Language Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCorrespondenceLanguages() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE);
  }

  /**
   * Get a list of Case address option Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCaseAddressOptions() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION);
  }

  /**
   * Get a list of Case link types Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCaseLinkTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE);
  }

  /**
   * Get a list of Case/application statuses Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getApplicationStatuses() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_APPLICATION_STATUS);
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
   * Get a list of Matter Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getMatterTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_MATTER_TYPES);
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
  public Mono<ClientInvolvementTypeLookupDetail> getClientInvolvementTypes(
      final String proceedingCode) {
    return ebsApiClient.getClientInvolvementTypes(proceedingCode);
  }

  /**
   * Get a list of Client Involvement Types Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getClientInvolvementTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES);
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
   * Get a list of Order Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getOrderTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE);
  }

  /**
   * Get the order type description, filtered by code.
   *
   * @return String containing the description of an order type.
   */
  public Mono<String> getOrderTypeDescription(final String code) {
    return ebsApiClient.getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE)
        .map(commonLookupDetail -> commonLookupDetail.getContent().stream()
            .filter(commonLookupValueDetail -> commonLookupValueDetail.getCode().equals(code))
            .findFirst()
            .map(CommonLookupValueDetail::getDescription)
            .orElse(code));
  }

  /**
   * Get a Correspondence Language Common Values.
   *
   * @param code The correspondence language code.
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCorrespondenceLanguage(
      final String code) {
    return getCommonValue(COMMON_VALUE_CORRESPONDENCE_LANGUAGE, code);
  }

  /**
   * Get a list of Ethnic Origin Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getEthnicOrigins() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN);
  }

  /**
   * Get an Ethnic Origin Common Values.
   *
   * @param code The ethnic origin code.
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getEthnicOrigin(final String code) {
    return getCommonValue(COMMON_VALUE_ETHNIC_ORIGIN, code);
  }

  /**
   * Get a list of Disability Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getDisabilities() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_DISABILITY);
  }

  /**
   * Get a Disability Common Values.
   *
   * @param code The disability code.
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getDisability(final String code) {
    return getCommonValue(COMMON_VALUE_DISABILITY, code);
  }

  /**
   * Get a list of Levels Of Service Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getLevelsOfService() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_LEVEL_OF_SERVICE);
  }

  /**
   * Get a list of Scope Limitation Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getScopeLimitations() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_SCOPE_LIMITATIONS);
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
   * Get a single Proceeding Status Lookup Value by code.
   *
   * @param code - the proceeding status code.
   * @return CommonLookupValueDetail containing the status value.
   */
  public Mono<CommonLookupValueDetail> getProceedingStatus(
      final String code) {
    return getCommonValue(COMMON_VALUE_PROCEEDING_STATUS, code);
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
        StringUtils.hasText(courtCode) ? String.format("*%s*", courtCode) : null,
        StringUtils.hasText(description) ? String.format("*%s*", description.toUpperCase()) : null);
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
   * @return Mono containing the category of law or null if not found.
   */
  public Mono<CategoryOfLawLookupValueDetail> getCategoryOfLaw(final String code) {
    return ebsApiClient.getCategoriesOfLaw(code, null, null)
        .mapNotNull(categoryOfLawLookupDetail -> categoryOfLawLookupDetail.getContent().stream()
            .findFirst()
            .orElse(null));
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
   * Get a list of all Organisation Relationship To Case Lookup Values.
   *
   * @return Mono containing all relationship lookup values or null if an error occurs.
   */
  public Mono<RelationshipToCaseLookupDetail> getOrganisationToCaseRelationships() {
    return ebsApiClient.getOrganisationRelationshipsToCaseValues(null, null);
  }

  /**
   * Get a list of all Relationships To Client Lookup Values.
   *
   * @return Mono containing all relationship lookup values or null if an error occurs.
   */
  public Mono<CommonLookupDetail> getRelationshipsToClient() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT);
  }

  /**
   * Retrieves all case status values.
   *
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues() {
    return this.getCaseStatusValues(null);
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

  /**
   * Get a single common value based on type and code.
   *
   * @param type - the value type.
   * @param code - the value code.
   * @return a Mono containing the CommonLookupValueDetail
   *     or an error handler if an error occurs.
   */
  public Mono<CommonLookupValueDetail> getCommonValue(
      final String type, final String code) {
    return ebsApiClient.getCommonValues(type, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
  }

  /**
   * Get a list of Notification Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getNotificationTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE);
  }

  /**
   * Get a list of Organisation Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getOrganisationTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES);
  }

  /**
   * Get a single Organisation Type Common Value by its code.
   *
   * @return CommonLookupValueDetail containing the common lookup values.
   */
  public Mono<CommonLookupValueDetail> getOrganisationType(final String code) {
    return this.getCommonValue(COMMON_VALUE_ORGANISATION_TYPES, code);
  }

}
