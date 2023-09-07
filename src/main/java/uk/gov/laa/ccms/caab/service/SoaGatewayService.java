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
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

/**
 * Service class responsible for interactions with the Service-Oriented Architecture (SOA) Gateway.
 * Provides methods to retrieve various data like notifications, contract details, client details,
 * etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoaGatewayService {

  private final SoaApiClient soaApiClient;

  /**
   * Retrieve the summary of notifications for a given user.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
   * @return A Mono wrapping the NotificationSummary for the specified user.
   */
  public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType) {
    return soaApiClient.getNotificationsSummary(loginId, userType);
  }

  /**
   * Fetches the complete Contract Details.
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
   * Searches and retrieves client details based on provided search criteria.
   *
   * @param clientSearchCriteria  The search criteria to use when fetching clients.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @param page                  The page number for pagination.
   * @param size                  The size or number of records per page.
   * @return A Mono wrapping the ClientDetails.
   */
  public Mono<ClientDetails> getClients(
          ClientSearchCriteria clientSearchCriteria,
          String loginId,
          String userType,
          Integer page,
          Integer size) {
    return soaApiClient.getClients(clientSearchCriteria, loginId, userType, page, size);
  }

  /**
   * Fetches detailed client information based on a given client reference number.
   *
   * @param clientReferenceNumber The client's reference number.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the ClientDetail.
   */
  public Mono<ClientDetail> getClient(String clientReferenceNumber, String loginId,
                                      String userType) {
    return soaApiClient.getClient(clientReferenceNumber, loginId, userType);
  }

  /**
   * Searches and retrieves case details based on provided search criteria.
   *
   * @param copyCaseSearchCriteria The search criteria to use when fetching cases.
   * @param loginId                The login identifier for the user.
   * @param userType               Type of the user (e.g., admin, user).
   * @param page                   The page number for pagination.
   * @param size                   The size or number of records per page.
   * @return A Mono wrapping the CaseDetails.
   */
  public Mono<CaseDetails> getCases(CopyCaseSearchCriteria copyCaseSearchCriteria, String loginId,
                                    String userType, Integer page, Integer size) {
    return soaApiClient.getCases(copyCaseSearchCriteria, loginId, userType, page, size);
  }

  /**
   * Fetches a unique case reference.
   *
   * @param loginId   The login identifier for the user.
   * @param userType  Type of the user (e.g., admin, user).
   * @return A Mono wrapping the CaseReferenceSummary.
   */
  public Mono<CaseReferenceSummary> getCaseReference(String loginId,
                                                     String userType) {
    return soaApiClient.getCaseReference(loginId, userType);
  }

}