package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProviderRequestMappingContext;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderRequestResponse;

/**
 * Service class to handle Provider requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderRequestService {

  private final SoaApiClient soaApiClient;

  private final ProviderRequestsMapper mapper;


  /**
   * Submits a provider request based on the given form data and user details.
   *
   * @param providerRequestTypeFormData the form data containing the type of the provider request
   * @param providerRequestDetailsFormData the form data containing details of the provider request
   * @param user the user submitting the provider request
   * @return the notification ID of the submitted provider request
   * @throws CaabApplicationException if the provider request submission fails
   */
  public String submitProviderRequest(
      final ProviderRequestTypeFormData providerRequestTypeFormData,
      final ProviderRequestDetailsFormData providerRequestDetailsFormData,
      final UserDetail user) {
    log.info("POST /provider-requests");

    final ProviderRequestMappingContext mappingContext = ProviderRequestMappingContext.builder()
        .user(user)
        .typeData(providerRequestTypeFormData)
        .detailsData(providerRequestDetailsFormData)
        .build();

    final ProviderRequestDetail providerRequestDetail =
        mapper.toProviderRequestDetail(mappingContext);

    try {
      return soaApiClient.submitProviderRequest(
          providerRequestDetail,
          user.getLoginId(),
          user.getUserType()).block().getNotificationId();

    } catch (final Exception e) {
      log.error("ProviderRequestService caught exception", e);
      throw new CaabApplicationException("Failed to submit provider request", e);
    }
  }


}
