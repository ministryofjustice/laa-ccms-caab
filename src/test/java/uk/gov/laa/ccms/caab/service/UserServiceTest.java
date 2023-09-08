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
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

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
}
