package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.user.UserViewModel;

/**
 * Client class responsible for interacting with the User Details API.
 *
 * <p>External users reach this application through SiLAS, which identifies them by an opaque
 * identifier rather than by a CCMS username. This API is what turns one into the other. The legacy
 * PUI calls the same endpoint for the same reason.
 */
@Service
@Slf4j
public class UserDetailsApiClient extends BaseApiClient {

  private static final String SILAS_USER_URI = "/api/v1/user-details/silas/{silasId}";

  private final UserDetailsApiClientErrorHandler userDetailsApiClientErrorHandler;

  protected UserDetailsApiClient(
      @Qualifier("userDetailsApiWebClient") WebClient userDetailsApiWebClient,
      UserDetailsApiClientErrorHandler userDetailsApiClientErrorHandler) {
    super(userDetailsApiWebClient);
    this.userDetailsApiClientErrorHandler = userDetailsApiClientErrorHandler;
  }

  /**
   * Retrieves the CCMS user held against a SiLAS identity.
   *
   * @param silasId the SiLAS identifier taken from the authenticated user's token.
   * @return a Mono containing the UserViewModel, empty if the identity is not known to the API, or
   *     an error handler if the call fails.
   */
  public Mono<UserViewModel> getUserBySilasId(final String silasId) {
    return webClient
        .get()
        .uri(SILAS_USER_URI, silasId)
        .retrieve()
        // An unknown identity is an expected outcome, not a call failure.
        .onStatus(
            status -> status.value() == HttpStatus.NOT_FOUND.value(),
            clientResponse -> Mono.empty())
        .bodyToMono(UserViewModel.class)
        .onErrorResume(
            e ->
                userDetailsApiClientErrorHandler.handleApiRetrieveError(
                    e, "User", "SiLAS id", silasId));
  }
}
