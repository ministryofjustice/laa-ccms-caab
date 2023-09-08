package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

/**
 * Service class to handle Clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
  private final SoaApiClient soaApiClient;

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
}
