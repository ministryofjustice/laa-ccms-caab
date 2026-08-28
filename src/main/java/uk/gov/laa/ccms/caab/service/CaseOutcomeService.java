package uk.gov.laa.ccms.caab.service;

import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
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
    if (existing.isPresent()) {
      final CaseOutcomeDetail existingCaseOutcome = existing.get();
      final Integer existingCaseOutcomeId = existingCaseOutcome.getId();
      if (existingCaseOutcomeId == null) {
        throw new IllegalStateException(
            "Case outcome record exists but has no id for case reference number: "
                + caseReferenceNumber);
      }

      final CaseOutcomeDetail rollbackCaseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);
      caseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);
      if (caseOutcome.getProceedingOutcomes() == null) {
        caseOutcome.setProceedingOutcomes(new ArrayList<>());
      }

      // Replace any existing outcome for this proceeding
      caseOutcome
          .getProceedingOutcomes()
          .removeIf(
              o ->
                  proceedingOutcome.getProceedingCaseId() != null
                      && proceedingOutcome.getProceedingCaseId().equals(o.getProceedingCaseId()));
      caseOutcome.addProceedingOutcomesItem(proceedingOutcome);

      caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId).block();
      recreateCaseOutcomeWithRollback(
          caseReferenceNumber, loginId, caseOutcome, rollbackCaseOutcome);
      return;
    } else {
      caseOutcome =
          new CaseOutcomeDetail()
              .caseReferenceNumber(caseReferenceNumber)
              .providerId(String.valueOf(providerId));
      caseOutcome.addProceedingOutcomesItem(proceedingOutcome);
    }

    caabApiClient.createCaseOutcome(loginId, caseOutcome).block();
  }

  /**
   * Clears a single proceeding outcome from the case outcome record. Because the CAAB API has no
   * PATCH endpoint for case outcomes, the existing record is deleted and recreated without the
   * cleared proceeding outcome.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param proceedingCaseId - the proceeding case id whose outcome should be removed.
   * @param loginId - the login ID of the user performing the update.
   */
  public void clearProceedingOutcome(
      final String caseReferenceNumber,
      final Integer providerId,
      final String proceedingCaseId,
      final String loginId) {
    if (proceedingCaseId == null) {
      return;
    }

    final Optional<CaseOutcomeDetail> existing = getCaseOutcome(caseReferenceNumber, providerId);
    if (existing.isEmpty()) {
      return;
    }

    final CaseOutcomeDetail existingCaseOutcome = existing.get();
    final Integer existingCaseOutcomeId = existingCaseOutcome.getId();
    if (existingCaseOutcomeId == null) {
      throw new IllegalStateException(
          "Case outcome record exists but has no id for case reference number: "
              + caseReferenceNumber);
    }

    final CaseOutcomeDetail rollbackCaseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);
    final CaseOutcomeDetail caseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);

    if (caseOutcome.getProceedingOutcomes() == null
        || caseOutcome.getProceedingOutcomes().isEmpty()) {
      return;
    }

    final boolean removed =
        caseOutcome
            .getProceedingOutcomes()
            .removeIf(outcome -> proceedingCaseId.equals(outcome.getProceedingCaseId()));
    if (!removed) {
      return;
    }

    caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId).block();

    // Always recreate — even with an empty proceedings list — so that the display layer can
    // distinguish "case outcomes have been managed (possibly all cleared)" from "never touched",
    // preventing a cleared outcome from reappearing via the EBS fallback.
    recreateCaseOutcomeWithRollback(caseReferenceNumber, loginId, caseOutcome, rollbackCaseOutcome);
  }

  private void recreateCaseOutcomeWithRollback(
      final String caseReferenceNumber,
      final String loginId,
      final CaseOutcomeDetail caseOutcomeToCreate,
      final CaseOutcomeDetail rollbackCaseOutcome) {
    try {
      caabApiClient.createCaseOutcome(loginId, caseOutcomeToCreate).block();
    } catch (CaabApiClientException ex) {
      log.warn(
          "Failed to create updated case outcome for case reference number: {}. "
              + "Attempting to restore previous case outcome data.",
          caseReferenceNumber,
          ex);
      try {
        caabApiClient.createCaseOutcome(loginId, rollbackCaseOutcome).block();
      } catch (CaabApiClientException restoreEx) {
        log.error(
            "Failed to restore previous case outcome for case reference number: {}",
            caseReferenceNumber,
            restoreEx);
        ex.addSuppressed(restoreEx);
      }
      throw ex;
    }
  }

  private CaseOutcomeDetail copyCaseOutcomeForCreate(final CaseOutcomeDetail source) {
    final CaseOutcomeDetail copy = new CaseOutcomeDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
    if (source.getProceedingOutcomes() != null) {
      copy.setProceedingOutcomes(new ArrayList<>(source.getProceedingOutcomes()));
    }
    return copy;
  }
}
