package uk.gov.laa.ccms.caab.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
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
@RequiredArgsConstructor
@Slf4j
public class DataService {

  private final EbsApiClient ebsApiClient;

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(String loginId) {
    return ebsApiClient.getUser(loginId);
  }

  /**
   * Retrieves common lookup values based on the type.
   * Convenience method to avoid passing null for the code and sort parameters.
   *
   * @param type The type of the common lookup values.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<List<CommonLookupValueDetail>> getCommonValues(String type) {
    return this.getCommonValues(type, null, null);
  }

  /**
   * Retrieves common lookup values based on the type, code, and sort criteria.
   *
   * @param type The type of the common lookup values. Can be null.
   * @param code The code of the common lookup values. Can be null.
   * @param sort The sort criteria for the common lookup values. Can be null.
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<List<CommonLookupValueDetail>> getCommonValues(
      String type, String code, String sort) {
    return ebsApiClient.getCommonValues(type, code, sort)
        .map(commonLookupDetail -> Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));
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
    return ebsApiClient.getCaseStatusValues(copyAllowed);
  }

  /**
   * Retrieves fee earner details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the FeeEarnerDetail or an error handler if an error occurs.
   */
  public Mono<FeeEarnerDetail> getFeeEarners(Integer providerId) {
    return ebsApiClient.getFeeEarners(providerId);
  }

  /**
   * Retrieves amendment type lookup details based on the provided application type.
   *
   * @param applicationType The application type to retrieve amendment types for.
   * @return A Mono containing the AmendmentTypeLookupDetail or an error handler if an error occurs.
   */
  public Mono<AmendmentTypeLookupDetail> getAmendmentTypes(String applicationType) {
    return ebsApiClient.getAmendmentTypes(applicationType);
  }

  /**
   * Retrieves country lookup details.
   *
   * @return A Mono containing the CommonLookupDetail or an error handler if an error occurs.
   */
  public Mono<List<CommonLookupValueDetail>> getCountries() {
    return ebsApiClient.getCountries()
        .map(commonLookupDetail -> Optional.ofNullable(commonLookupDetail)
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));
  }

}

