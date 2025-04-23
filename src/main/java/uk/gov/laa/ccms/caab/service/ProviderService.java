package uk.gov.laa.ccms.caab.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
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
    return Optional.ofNullable(contractDetails)// Wrap the list in an Optional
        .map(list -> list.stream()
            .filter(c -> Boolean.TRUE.equals(c.isCreateNewMatters()) || (!initialApplication
                && Boolean.TRUE.equals(c.isRemainderAuthorisation())))
            .map(ContractDetail::getCategoryofLaw)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  /**
   * Retrieves provider details for a specific provider.
   *
   * @param providerId The ID of the provider.
   * @return A Mono containing the ProviderDetail or an error handler if an error occurs.
   */
  public Mono<ProviderDetail> getProvider(Integer providerId) {
    return ebsApiClient.getProvider(providerId);
  }

  /**
   * Fetches a single list of fee earners across all offices of a provider.
   *
   * @param provider The Provider details.
   * @return List of contact details representing fee earners.
   */
  public List<ContactDetail> getAllFeeEarners(ProviderDetail provider) {
    return provider.getOffices() != null ? provider.getOffices().stream()
        .map(OfficeDetail::getFeeEarners)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet())
        .stream()
        .filter(contactDetail -> contactDetail.getName() != null)
        .sorted(Comparator.comparing(ContactDetail::getName))
        .collect(Collectors.toList()) : Collections.emptyList();
  }

  /**
   * Fetches a list of fee earners for a specific office of a provider.
   *
   * @param provider The Provider details.
   * @param officeId The ID of the office to get fee earners from.
   * @return List of contact details representing fee earners for the specified office.
   */
  public List<ContactDetail> getFeeEarnersByOffice(ProviderDetail provider, Integer officeId) {
    return provider.getOffices() != null ? provider.getOffices().stream()
        .filter(officeDetail -> officeDetail.getId().equals(officeId))
        .flatMap(officeDetail -> officeDetail.getFeeEarners() != null
            ? officeDetail.getFeeEarners().stream() : Stream.empty())
        .filter(contactDetail -> contactDetail.getName() != null)
        .sorted(Comparator.comparing(ContactDetail::getName))
        .collect(Collectors.toList()) : Collections.emptyList();
  }

  /**
   * Fetches a list of fee earners for a specific office of a provider.
   *
   * @param provider The Provider details.
   * @param officeId The ID of the office to get the fee earners from.
   * @param feeEarnerId The ID of the fee earner to get the fee earner from.
   * @return List of contact details representing fee earners for the specified office.
   */
  public ContactDetail getFeeEarnerByOfficeAndId(
      final ProviderDetail provider,
      final Integer officeId,
      final Integer feeEarnerId) {

    if (feeEarnerId == null) {
      return null;
    }

    return getFeeEarnersByOffice(provider, officeId)
        .stream()
        .filter(fe -> feeEarnerId.equals(fe.getId()))
        .findFirst()
        .orElse(null);
  }
}
