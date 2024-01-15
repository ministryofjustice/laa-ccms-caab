package uk.gov.laa.ccms.caab.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.TransactionStatus;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private ClientDetailMapper clientDetailMapper;

  @InjectMocks
  private ClientService clientService;

  @Test
  void getClient_returnsClientDetails_Successful() {
    String clientReferenceNumber = "CLIENT123";
    String loginId = "user1";
    String userType = "userType";

    ClientDetail mockClientDetail = new ClientDetail();

    when(soaApiClient.getClient(clientReferenceNumber, loginId, userType))
        .thenReturn(Mono.just(mockClientDetail));

    Mono<ClientDetail> clientDetailMono =
        clientService.getClient(clientReferenceNumber, loginId, userType);

    StepVerifier.create(clientDetailMono)
        .expectNextMatches(clientDetail -> clientDetail == mockClientDetail)
        .verifyComplete();
  }

  @Test
  void getClients_ReturnsClientDetails_Successful() {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    String loginId = "user1";
    String userType = "userType";
    String firstName = "John";
    String lastName = "Doe";

    int page = 0;
    int size = 10;

    clientSearchCriteria.setForename(firstName);
    clientSearchCriteria.setSurname(lastName);

    ClientDetails mockClientDetails = new ClientDetails();

    when(soaApiClient.getClients(clientSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockClientDetails));

    Mono<ClientDetails> clientDetailsMono =
        clientService.getClients(clientSearchCriteria, loginId, userType, page, size);

    StepVerifier.create(clientDetailsMono)
        .expectNextMatches(clientDetails -> clientDetails == mockClientDetails)
        .verifyComplete();
  }

  @Test
  void getClientStatus_ReturnsClientStatus_Successful() {
    String transactionId = "TRANS123";
    String loginId = "user1";
    String userType = "userType";

    TransactionStatus mockClientStatus = new TransactionStatus();

    when(soaApiClient.getClientStatus(transactionId, loginId, userType))
        .thenReturn(Mono.just(mockClientStatus));

    Mono<TransactionStatus> clientStatusMono =
        clientService.getClientStatus(transactionId, loginId, userType);

    StepVerifier.create(clientStatusMono)
        .expectNextMatches(clientStatus -> clientStatus == mockClientStatus)
        .verifyComplete();
  }

  @Test
  void postClient_ReturnsTransactionId_Successful() {
    ClientDetail clientDetail = new ClientDetail();
    ClientDetailDetails clientDetailDetails = new ClientDetailDetails();
    clientDetail.setDetails(clientDetailDetails);

    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    ClientFlowFormData clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    clientFlowFormData.setBasicDetails(basicDetails);
    clientFlowFormData.setContactDetails(contactDetails);
    clientFlowFormData.setAddressDetails(addressDetails);
    clientFlowFormData.setMonitoringDetails(monitoringDetails);

    String loginId = "user1";
    String userType = "userType";

    ClientTransactionResponse mockClientCreated = new ClientTransactionResponse();
    UserDetail userDetail = new UserDetail();
    userDetail.setLoginId(loginId);
    userDetail.setUserType(userType);

    when(clientDetailMapper.toClientDetail(any()))
        .thenReturn(clientDetail);

    when(soaApiClient.postClient(clientDetailDetails, loginId, userType))
        .thenReturn(Mono.just(mockClientCreated));

    Mono<ClientTransactionResponse> clientCreatedMono =
        clientService.createClient(clientFlowFormData, userDetail);

    StepVerifier.create(clientCreatedMono)
        .expectNextMatches(clientCreated -> clientCreated == mockClientCreated)
        .verifyComplete();
  }

  @Test
  void updateClient_ReturnsTransactionId_Successful() {
    ClientDetail clientDetail = new ClientDetail();
    ClientDetailDetails clientDetailDetails = new ClientDetailDetails();
    clientDetail.setDetails(clientDetailDetails);

    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    ClientFlowFormData clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    clientFlowFormData.setBasicDetails(basicDetails);
    clientFlowFormData.setContactDetails(contactDetails);
    clientFlowFormData.setAddressDetails(addressDetails);
    clientFlowFormData.setMonitoringDetails(monitoringDetails);

    String loginId = "user1";
    String userType = "userType";
    String clientReferenceNumber = "1234";

    ClientTransactionResponse mockClientUpdated = new ClientTransactionResponse();
    UserDetail userDetail = new UserDetail();
    userDetail.setLoginId(loginId);
    userDetail.setUserType(userType);

    when(clientDetailMapper.toClientDetail(any()))
        .thenReturn(clientDetail);

    when(soaApiClient.putClient(clientReferenceNumber, clientDetailDetails, loginId, userType))
        .thenReturn(Mono.just(mockClientUpdated));

    Mono<ClientTransactionResponse> clientUpdatedMono =
        clientService.updateClient(clientReferenceNumber, clientFlowFormData, userDetail);

    StepVerifier.create(clientUpdatedMono)
        .expectNextMatches(clientUpdated -> clientUpdated == mockClientUpdated)
        .verifyComplete();
  }
}
