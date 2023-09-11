package uk.gov.laa.ccms.caab.service;

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
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {
  @Mock
  private SoaApiClient soaApiClient;

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

}
