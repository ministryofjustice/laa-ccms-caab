package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

/**
 * Service class to handle Users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final EbsApiClient ebsApiClient;

  /**
   * Retrieves user details based on the login ID.
   *
   * @param loginId The login ID of the user.
   * @return A Mono containing the UserDetail or an error handler if an error occurs.
   */
  public Mono<UserDetail> getUser(String loginId) {
    return ebsApiClient.getUser(loginId);
  }


  /**
   * Retrieves the list of users for the given provider ID.
   *
   * @param providerId the ID of the provider.
   * @return A Mono containing the list of users or an error handler if an error occurs.
   */
  public Mono<UserDetails> getUsers(final Integer providerId) {
    return ebsApiClient.getUsers(providerId);
  }
}
