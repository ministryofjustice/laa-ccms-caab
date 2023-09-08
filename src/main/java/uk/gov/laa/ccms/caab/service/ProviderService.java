package uk.gov.laa.ccms.caab.service;

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
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

/**
 * Service class to handle Providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {
  private final SoaApiClient soaApiClient;

  private final EbsApiClient ebsApiClient;

  /**
   * Fetches the Contract Details for the provided provider and office.
   *
   * @param providerFirmId       The identifier for the provider firm.
   * @param officeId             The identifier for the office.
   * @param loginId              The login identifier for the user.
   * @param userType             Type of the user (e.g., admin, user).
   * @return A Contract Details containing all Contracts.
   */
  public Mono<ContractDetails> getContractDetails(
      Integer providerFirmId,
      Integer officeId,
      String loginId,
      String userType) {
    return soaApiClient.getContractDetails(
        providerFirmId,
        officeId,
        loginId,
        userType);
  }

  /**
   * Fetches the list of Category of Law codes based on specified criteria.
   *
   * @param providerFirmId       The identifier for the provider firm.
   * @param officeId             The identifier for the office.
   * @param loginId              The login identifier for the user.
   * @param userType             Type of the user (e.g., admin, user).
   * @param initialApplication   Whether it's an initial application or not.
   * @return A list of Category of Law codes.
   */
  public List<String> getCategoryOfLawCodes(
      Integer providerFirmId,
      Integer officeId,
      String loginId,
      String userType,
      Boolean initialApplication) {
    ContractDetails contractDetails = this.getContractDetails(
        providerFirmId,
        officeId,
        loginId,
        userType).block();

    // Process and filter the response
    return Optional.ofNullable(contractDetails)
        .map(cd -> filterCategoriesOfLaw(cd.getContracts(), initialApplication))
        .orElse(Collections.emptyList());
  }

  /**
   * Build a filtered list of Category Of Law.
   * Include the Category code only if
   * - CreateNewMatters is true
   * or
   * - This is not an initial Application and RemainderAuthorisation is true
   *
   * @param contractDetails    The List of contract details to process
   * @param initialApplication if it is an initial application
   * @return List of Category Of Law Codes
   */
  private List<String> filterCategoriesOfLaw(List<ContractDetail> contractDetails,
      final Boolean initialApplication) {
    return contractDetails.stream()
        .filter(c -> Boolean.TRUE.equals(c.isCreateNewMatters()) || (!initialApplication
            && Boolean.TRUE.equals(c.isRemainderAuthorisation())))
        .map(ContractDetail::getCategoryofLaw)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
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
}
