package uk.gov.laa.ccms.caab.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;

/** Service class to handle Case Outcomes. */
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
      final String caseReferenceNumber, final Integer providerId) {
    return caabApiClient
        .getCaseOutcomes(caseReferenceNumber, providerId)
        .mapNotNull(caseOutcomeDetails -> caseOutcomeDetails.getContent().stream().findFirst())
        .block();
  }

  /**
   * Creates or updates a single proceeding outcome within the case outcome record. Because the CAAB
   * API has no PATCH endpoint for case outcomes, the existing record is recreated with the updated
   * proceeding outcome.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param proceedingOutcome - the updated proceeding outcome to store.
   * @param loginId - the login ID of the user performing the update.
   */
  public void updateProceedingOutcome(
      final String caseReferenceNumber,
      final Integer providerId,
      final ProceedingOutcomeDetail proceedingOutcome,
      final String loginId) {

    Optional<CaseOutcomeDetail> existing = getCaseOutcome(caseReferenceNumber, providerId);

    final CaseOutcomeDetail caseOutcome;
    Integer existingCaseOutcomeId = null;
    if (existing.isPresent()) {
      caseOutcome = existing.get();
      existingCaseOutcomeId = caseOutcome.getId();
      if (existingCaseOutcomeId == null) {
        throw new IllegalStateException(
            "Case outcome record exists but has no id for case reference number: "
                + caseReferenceNumber);
      }
      // Replace any existing outcome for this proceeding
      caseOutcome
          .getProceedingOutcomes()
          .removeIf(
              o ->
                  proceedingOutcome.getProceedingCaseId() != null
                      && proceedingOutcome.getProceedingCaseId().equals(o.getProceedingCaseId()));
      caseOutcome.addProceedingOutcomesItem(proceedingOutcome);
      caseOutcome.setId(null);
    } else {
      caseOutcome =
          new CaseOutcomeDetail()
              .caseReferenceNumber(caseReferenceNumber)
              .providerId(String.valueOf(providerId));
      caseOutcome.addProceedingOutcomesItem(proceedingOutcome);
    }

    caabApiClient.createCaseOutcome(loginId, caseOutcome).block();
    if (existingCaseOutcomeId != null) {
      caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId).block();
    }
  }
}
