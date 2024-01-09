package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.util.ReflectionUtils;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.TransactionStatus;

/**
 * Service class to handle Clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
  private final SoaApiClient soaApiClient;

  private final ClientDetailMapper clientDetailsMapper;

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
      final ClientSearchCriteria clientSearchCriteria,
      final String loginId,
      final String userType,
      final Integer page,
      final Integer size) {
    log.debug("SOA Clients to get using criteria: {}", clientSearchCriteria);
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
  public Mono<ClientDetail> getClient(
      final String clientReferenceNumber,
      final String loginId,
      final String userType) {
    log.debug("SOA Client to get using reference: {}", clientReferenceNumber);
    return soaApiClient.getClient(clientReferenceNumber, loginId, userType);
  }

  /**
   * Fetches the transaction status for a client create transaction.
   *
   * @param transactionId         The transaction id for the client create transaction in soa.
   * @param loginId               The login identifier for the user.
   * @param userType              Type of the user (e.g., admin, user).
   * @return A Mono wrapping the TransactionStatus.
   */
  public Mono<TransactionStatus> getClientStatus(
      final String transactionId,
      final String loginId,
      final String userType) {
    log.debug("SOA Client Status to get using transaction Id: {}", transactionId);
    return soaApiClient.getClientStatus(transactionId, loginId, userType);
  }

  /**
   * Creates a client based on a given client details.
   *
   * @param clientFlowFormData    The client's details.
   * @param user                  The user.
   * @return A Mono wrapping the ClientCreated transaction id.
   */
  public Mono<ClientTransactionResponse> createClient(
      final ClientFlowFormData clientFlowFormData,
      final UserDetail user) {

    ReflectionUtils.nullifyStrings(clientFlowFormData.getBasicDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getContactDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getAddressDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getMonitoringDetails());
    final ClientDetail clientDetail = clientDetailsMapper.toClientDetail(clientFlowFormData);

    return soaApiClient.postClient(
        clientDetail.getDetails(),
        user.getLoginId(),
        user.getUserType());
  }

  /**
   * Updates a client based on a given client details.
   *
   * @param clientFlowFormData    The client's details
   * @param user                  The user.
   * @return A Mono wrapping the ClientCreated transaction id.
   */
  public Mono<ClientTransactionResponse> updateClient(
      final String clientReferenceNumber,
      final ClientFlowFormData clientFlowFormData,
      final UserDetail user) {

    ReflectionUtils.nullifyStrings(clientFlowFormData.getBasicDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getContactDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getAddressDetails());
    ReflectionUtils.nullifyStrings(clientFlowFormData.getMonitoringDetails());
    final ClientDetail clientDetail = clientDetailsMapper.toClientDetail(clientFlowFormData);

    return soaApiClient.putClient(
        clientReferenceNumber,
        clientDetail.getDetails(),
        user.getLoginId(),
        user.getUserType());
  }
}
