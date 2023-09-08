package uk.gov.laa.ccms.caab.service;


import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

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
   * Get a list of Marital Status Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getMaritalStatuses() {
    return ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS);
  }

  /**
   * Get a list of Country Common Values.
   *
   * @return CommonLookupDetail containing the common lookup values.
   */
  public Mono<CommonLookupDetail> getCountries() {
    return ebsApiClient.getCountries();
  }


}
