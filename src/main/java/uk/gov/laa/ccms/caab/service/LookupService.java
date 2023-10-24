package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_STATUS;
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
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
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
  public Mono<CommonLookupValueDetail> getApplicationType(String code) {
    return getCommonValue(COMMON_VALUE_APPLICATION_TYPE, code);
  }

  /**
   * Get a list of Category Of Law Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCategoriesOfLaw() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW);
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
  public Mono<CommonLookupValueDetail> getGender(String code) {
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
   * @param code The code of the common lookup values
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupValueDetail> getContactTitle(String code) {
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
   * @param code The code of the common lookup values
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupValueDetail> getMaritalStatus(String code) {
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
   * @param code The code of the common lookup values
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCountry(String code) {
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
   * Get a Correspondence Method Common Values.
   *
   * @param code The code of the common lookup values
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCorrespondenceMethod(String code) {
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
   * Get a Correspondence Language Common Values.
   *
   * @param code The code of the common lookup values
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getCorrespondenceLanguage(String code) {
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
   * @param code The code of the common lookup values
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getEthnicOrigin(String code) {
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
   * @param code The code of the common lookup values
   * @return CommonLookupValueDetail containing the common lookup value.
   */
  public Mono<CommonLookupValueDetail> getDisability(String code) {
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
   * Get a list of Matter Type Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getMatterTypes() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_MATTER_TYPES);
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
   * Get a list of Scope Limitation Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getScopeLimitations() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_SCOPE_LIMITATIONS);
  }

  /**
   * Get a list of Scope Limitation Detail.
   *
   * @return ScopeLimitationDetails containing the scope limitation details.
   */
  public Mono<ScopeLimitationDetails> getScopeLimitationDetails(
      ScopeLimitationDetail searchCriteria) {
    return ebsApiClient.getScopeLimitations(searchCriteria);
  }

  /**
   * Get a list of Proceeding Status Lookup Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getProceedingStatuses() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_PROCEEDING_STATUS);
  }

  /**
   * Get a single Proceeding Status Lookup Value by code.
   *
   * @return CommonLookupValueDetail containing the status value.
   */
  public Mono<CommonLookupValueDetail> getProceedingStatus(String code) {
    return getCommonValue(COMMON_VALUE_PROCEEDING_STATUS, code);
  }

  /**
   * Retrieves court details.
   * A wildcard match is performed for both courtCode and description to return all Courts
   * which contain the provided values.
   *
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCourts(String courtCode, String description) {
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
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCourts(String courtCode) {
    return this.getCourts(courtCode, null);
  }

  /**
   * Retrieves outcome results data based on the supplied proceedingCode and outcomeResult value.
   *
   * @return A Mono containing the OutcomeResultLookupDetail or an error handler if an error occurs.
   */
  public Mono<OutcomeResultLookupDetail> getOutcomeResults(String proceedingCode,
      String outcomeResult) {
    return ebsApiClient.getOutcomeResults(proceedingCode, outcomeResult);
  }

  /**
   * Retrieves stage end data based on the supplied proceedingCode and stageEnd value.
   *
   * @return A Mono containing the StageEndLookupDetail or an error handler if an error occurs.
   */
  public Mono<StageEndLookupDetail> getStageEnds(String proceedingCode,
      String stageEnd) {
    return ebsApiClient.getStageEnds(proceedingCode, stageEnd);
  }

  /**
   * Retrieves all prior authority types.
   *
   * @return A Mono containing the PriorAuthorityLookupDetail
   *   or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes() {
    return this.getPriorAuthorityTypes(null, null);
  }

  /**
   * Retrieves prior authority types matching the specified code and valueRequired flag.
   *
   * @return A Mono containing the PriorAuthorityLookupDetail
   *   or an error handler if an error occurs.
   */
  public Mono<PriorAuthorityTypeDetails> getPriorAuthorityTypes(
      final String code, final Boolean valueRequired) {
    return ebsApiClient.getPriorAuthorityTypes(code, valueRequired);
  }

  /**
   * Retrieves all award types.
   *
   * @return A Mono containing the AwardTypeLookupDetail
   *   or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes() {
    return this.getAwardTypes(null, null);
  }

  /**
   * Retrieves award types matching the specified code and awardType values.
   *
   * @return A Mono containing the AwardTypeLookupDetail
   *   or an error handler if an error occurs.
   */
  public Mono<AwardTypeLookupDetail> getAwardTypes(
      final String code, final String awardType) {
    return ebsApiClient.getAwardTypes(code, awardType);
  }

  public Mono<CommonLookupValueDetail> getCommonValue(String type, String code) {
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




}
