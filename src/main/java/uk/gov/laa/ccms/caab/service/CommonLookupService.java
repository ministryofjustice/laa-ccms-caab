package uk.gov.laa.ccms.caab.service;


import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Service class to handle Common Lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommonLookupService {
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_GENDER, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_CONTACT_TITLE, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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

  public Mono<RelationshipToCaseLookupDetail> getPersonToCaseRelationships() {
    return ebsApiClient.getPersonRelationshipsToCaseValues();
  }

  public Mono<RelationshipToCaseLookupDetail> getOrganisationToCaseRelationships() {
    return ebsApiClient.getOrganisationRelationshipsToCaseValues();
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
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
    return ebsApiClient.getCommonValues(COMMON_VALUE_DISABILITY, code)
        .mapNotNull(commonLookupDetail -> commonLookupDetail
            .getContent().stream()
            .findFirst()
            .orElse(null));
  }




}
