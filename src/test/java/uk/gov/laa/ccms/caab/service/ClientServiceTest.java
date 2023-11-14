package uk.gov.laa.ccms.caab.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;

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

    ClientStatus mockClientStatus = new ClientStatus();

    when(soaApiClient.getClientStatus(transactionId, loginId, userType))
        .thenReturn(Mono.just(mockClientStatus));

    Mono<ClientStatus> clientStatusMono =
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

    uk.gov.laa.ccms.caab.bean.ClientDetails clientDetails = new uk.gov.laa.ccms.caab.bean.ClientDetails();
    String loginId = "user1";
    String userType = "userType";

    ClientCreated mockClientCreated = new ClientCreated();
    UserDetail userDetail = new UserDetail();
    userDetail.setLoginId(loginId);
    userDetail.setUserType(userType);

    when(clientDetailMapper.toSoaClientDetail(any()))
        .thenReturn(clientDetail);

    when(soaApiClient.postClient(clientDetailDetails, loginId, userType))
        .thenReturn(Mono.just(mockClientCreated));

    Mono<ClientCreated> clientCreatedMono =
        clientService.createClient(clientDetails, userDetail);

    StepVerifier.create(clientCreatedMono)
        .expectNextMatches(clientCreated -> clientCreated == mockClientCreated)
        .verifyComplete();
  }
}
