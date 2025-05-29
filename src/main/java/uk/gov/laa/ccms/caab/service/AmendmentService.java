package uk.gov.laa.ccms.caab.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.CaseDetail;

@Service
@RequiredArgsConstructor
public final class AmendmentService {

  private final EbsApiClient ebsApiClient;
  private final CaabApiClient caabApiClient;

  public ApplicationDetail createAmendment(ApplicationDetail initialAmendmentDetails,
      String caseReferenceNumber,
      Long providerId,
      String userId) {

    // Original case
    Mono<CaseDetail> originalCase = ebsApiClient.getCase(caseReferenceNumber, providerId, userId);

    // TODO: Remove abandoned opponents. Check CcmsPrepare.java and PrepareAmendment:73.

    // Check if application already exists
    Optional<ApplicationDetail> existingAmendment =
        caabApiClient.getApplication(caseReferenceNumber).blockOptional();
    if(existingAmendment.isEmpty()) {
      ApplicationDetail newAmendmentDetails = new ApplicationDetail();
      // Copy data from case into application

      return newAmendmentDetails;
    }


    return existingAmendment.get();
  }

}
