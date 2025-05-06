package uk.gov.laa.ccms.caab.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;

/**
 * Service class to handle Case Outcomes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseOutcomeService {

  private final CaabApiClient caabApiClient;

  /**
   * Get a single Case Outcome based on the supplied case reference number and provider id.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @return Optional CaseOutcomeDetail if one exists for the supplied search criteria.
   */
  public Optional<CaseOutcomeDetail> getCaseOutcome(
      final String caseReferenceNumber,
      final Integer providerId) {
    return caabApiClient.getCaseOutcomes(caseReferenceNumber, providerId)
        .mapNotNull(caseOutcomeDetails -> caseOutcomeDetails.getContent().stream()
            .findFirst())
        .block();
  }


}
