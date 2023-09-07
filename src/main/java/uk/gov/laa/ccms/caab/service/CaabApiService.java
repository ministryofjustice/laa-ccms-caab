package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

/**
 * Service responsible for interactions with the CAAB API.
 */
@Service
@RequiredArgsConstructor
public class CaabApiService {

  private final CaabApiClient caabApiClient;

  /**
   * Creates an application using the CAAB API.
   *
   * @param loginId the ID associated with the user login
   * @param application the details of the application to be created
   * @return a Mono signaling the completion of the application creation
   */
  public Mono<Void> createApplication(String loginId, ApplicationDetail application) {

    return caabApiClient.createApplication(loginId, application);
  }
}
