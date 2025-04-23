package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;
import uk.gov.laa.ccms.soa.gateway.model.UserOptions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @Mock
  private SoaApiClient soaApiClient;

  @InjectMocks
  private UserService userService;

  @Test
  void getUser_returnData() {

    String loginId = "user1";

    UserDetail mockUser = new UserDetail();
    mockUser.setLoginId(loginId);

    when(ebsApiClient.getUser(loginId)).thenReturn(Mono.just(mockUser));

    Mono<UserDetail> userDetailsMono = userService.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .expectNextMatches(user -> user.getLoginId().equals(loginId))
        .verifyComplete();
  }

  @Test
  void getUsers_returnsData() {
    Integer providerId = 1234;
    UserDetails userDetails = new UserDetails()
        .addContentItem(new BaseUser()
            .userId(123)
            .userType("type1")
            .loginId("login1"));
    when(ebsApiClient.getUsers(providerId)).thenReturn(Mono.just(userDetails));
    Mono<UserDetails> userDetailsMono = userService.getUsers(providerId);
    StepVerifier.create(userDetailsMono)
        .expectNextMatches(userList -> "login1".equals(userList.getContent().getFirst().getLoginId()))
        .verifyComplete();
  }

  @Test
  void updateUserOptions_updatesUser() {
    String loginId = "loginId";
    String userType = "userType";
    UserOptions userOptions = new UserOptions()
        .userLoginId(loginId)
        .providerFirmId("12345");

    ClientTransactionResponse userUpdatedResponse = new ClientTransactionResponse();

    when(soaApiClient.updateUserOptions(userOptions, loginId, userType)).thenReturn(Mono.just(userUpdatedResponse));

    Mono<ClientTransactionResponse> responseMono = userService.updateUserOptions(12345, loginId,
        userType);
    StepVerifier.create(responseMono)
        .expectNextMatches(userUpdated -> userUpdated == userUpdatedResponse)
        .verifyComplete();

  }
}
