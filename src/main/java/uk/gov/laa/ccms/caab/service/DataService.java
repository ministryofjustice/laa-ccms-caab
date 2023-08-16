package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Service class responsible for interacting with the data service to retrieve various data
 * entities.
 */
@Service
@Slf4j
public class DataService {
  private final WebClient dataWebClient;

  private final DataServiceErrorHandler dataServiceErrorHandler;

  /**
   * Constructs the DataService instance with the necessary dependencies.
   *
   * @param dataWebClient The WebClient instance for making HTTP requests to the data service.
   * @param dataServiceErrorHandler The error handler for handling errors during data service calls.
   */
  public DataService(@Qualifier("dataWebClient") WebClient dataWebClient,
                     DataServiceErrorHandler dataServiceErrorHandler) {
    this.dataWebClient = dataWebClient;
    this.dataServiceErrorHandler = dataServiceErrorHandler;
  }

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(String loginId) {

    return dataWebClient
            .get()
            .uri("/users/{loginId}", loginId)
            .retrieve()
            .bodyToMono(UserDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler.handleUserError(loginId, e));
  }

  /**
   * Retrieves common lookup values based on the type, code, and sort criteria.
   *
   * @param type The type of the common lookup values.
   * @param code The code of the common lookup values.
   * @param sort The sort criteria for the common lookup values.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCommonValues(String type, String code, String sort) {

    return dataWebClient
            .get()
            .uri(builder -> builder.path("/lookup/common")
                    .queryParamIfPresent("type", Optional.ofNullable(type))
                    .queryParamIfPresent("code", Optional.ofNullable(code))
                    .queryParamIfPresent("sort", Optional.ofNullable(sort))
                    .build())
            .retrieve()
            .bodyToMono(CommonLookupDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler.handleCommonValuesError(
                    type, code, sort, e));
  }


  /**
   * Retrieves the case status lookup value that is eligible for copying.
   *
   * @return The CaseStatusLookupValueDetail representing the eligible case status for copying.
   */
  public CaseStatusLookupValueDetail getCopyCaseStatus() {
    CaseStatusLookupDetail caseStatusLookupDetail = this.getCaseStatusValues(Boolean.TRUE).block();

    return Optional.ofNullable(caseStatusLookupDetail)
            .map(CaseStatusLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream().findFirst().orElse(null);
  }

  /**
   * Retrieves the case status lookup details based on the provided copyAllowed flag.
   *
   * @param copyAllowed A boolean flag indicating whether copying is allowed.
   * @return A Mono containing the CaseStatusLookupDetail or an error handler if an error occurs.
   */
  public Mono<CaseStatusLookupDetail> getCaseStatusValues(Boolean copyAllowed) {

    return dataWebClient
            .get()
            .uri(builder -> builder.path("/lookup/case-status")
                    .queryParamIfPresent("copy-allowed", Optional.ofNullable(copyAllowed))
                    .build())
            .retrieve()
            .bodyToMono(CaseStatusLookupDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler
                    .handleCaseStatusValuesError(copyAllowed, e));
  }

  /**
   * Retrieves a list of common lookup values representing application types.
   *
   * @return A list of CommonLookupValueDetail representing application types.
   */
  public List<CommonLookupValueDetail> getApplicationTypes() {
    CommonLookupDetail commonLookupDetail = getCommonValues(
            COMMON_VALUE_APPLICATION_TYPE,
            null,
            null).block();

    return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .filter(applicationType -> {
              String code = applicationType.getCode().toUpperCase();
              return !EXCLUDED_APPLICATION_TYPE_CODES.contains(code);
            })
            .collect(Collectors.toList());
  }

  /**
   * Retrieves a list of common lookup values representing genders.
   *
   * @return A list of CommonLookupValueDetail representing genders.
   */
  public List<CommonLookupValueDetail> getGenders() {
    CommonLookupDetail commonLookupValues = getCommonValues(
            COMMON_VALUE_GENDER,
            null,
            null).block();
    return Optional.ofNullable(commonLookupValues)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList());
  }

  /**
   * Retrieves a list of common lookup values representing unique identifier types.
   *
   * @return A list of CommonLookupValueDetail representing unique identifier types.
   */
  public List<CommonLookupValueDetail> getUniqueIdentifierTypes() {
    CommonLookupDetail commonLookupValues = getCommonValues(
            COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE,
            null,
            null).block();
    return Optional.ofNullable(commonLookupValues)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList());
  }

  /**
   * Retrieves a list of common lookup values representing categories of law based on codes.
   *
   * @param codes The list of category codes to filter by.
   * @return A list of CommonLookupValueDetail representing categories of law.
   */
  public List<CommonLookupValueDetail> getCategoriesOfLaw(List<String> codes) {
    CommonLookupDetail commonLookupDetail = getCommonValues(
            COMMON_VALUE_CATEGORY_OF_LAW,
            null,
            null).block();

    return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .filter(category -> codes.contains(category.getCode()))
            .collect(Collectors.toList());
  }

  /**
   * Retrieves a list of all common lookup values representing categories of law.
   *
   * @return A list of CommonLookupValueDetail representing all categories of law.
   */
  public List<CommonLookupValueDetail> getAllCategoriesOfLaw() {
    CommonLookupDetail commonLookupDetail = getCommonValues(
            COMMON_VALUE_CATEGORY_OF_LAW,
            null,
            null).block();

    return Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList());
  }

  /**
   * Retrieves fee earner details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the FeeEarnerDetail or an error handler if an error occurs.
   */
  public Mono<FeeEarnerDetail> getFeeEarners(Integer providerId) {
    return dataWebClient
            .get()
            .uri(builder -> builder.path("/fee-earners")
                    .queryParam("provider-id", providerId)
                    .build())
            .retrieve()
            .bodyToMono(FeeEarnerDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler.handleFeeEarnersError(providerId, e));
  }

  /**
   * Retrieves amendment type lookup details based on the provided application type.
   *
   * @param applicationType The application type to retrieve amendment types for.
   * @return A Mono containing the AmendmentTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(String applicationType) {
    return dataWebClient
            .get()
            .uri(builder -> builder.path("/lookup/amendment-types")
                    .queryParamIfPresent("application-type",
                            Optional.ofNullable(applicationType))
                    .build())
            .retrieve()
            .bodyToMono(AmendmentTypeLookupDetail.class)
            .onErrorResume(e -> dataServiceErrorHandler
                    .handleAmendmentTypeLookupError(applicationType, e));
  }

  /**
   * Retrieves country lookup details.
   *
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<CommonLookupDetail> getCountries() {
    return dataWebClient
            .get()
            .uri(builder -> builder.path("/lookup/countries")
                    .queryParam("size", 1000)
                    .build())
            .retrieve()
            .bodyToMono(CommonLookupDetail.class)
            .onErrorResume(dataServiceErrorHandler::handleCountryLookupError);
  }

}

