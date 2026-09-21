package uk.gov.laa.ccms.caab.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
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
   * Loads the case outcome for Outcome and Awards, creating a bootstrap record when one does not
   * already exist.
   */
  public CaseOutcomeDetail getOrCreateCaseOutcome(
      final String caseReferenceNumber,
      final Integer providerId,
      final String loginId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    return getCaseOutcome(caseReferenceNumber, providerId)
        .orElseGet(
            () ->
                createAndReloadCaseOutcome(
                    caseReferenceNumber, providerId, loginId, bootstrapCaseOutcome));
  }

  /** Returns a financial award only when it belongs to the supplied case outcome. */
  public Optional<FinancialAwardDetail> getFinancialAward(
      final String caseReferenceNumber, final Integer providerId, final Integer financialAwardId) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getCaseOutcome(caseReferenceNumber, providerId)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No case outcome exists for case reference number: "
                                + caseReferenceNumber)),
            caseReferenceNumber);
    return Optional.ofNullable(
        caabApiClient.getFinancialAward(caseOutcomeId, financialAwardId).block());
  }

  /** Creates a financial award without replacing the owning case outcome aggregate. */
  public void createFinancialAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final FinancialAwardRequest financialAward,
      final String loginId) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getOrCreateCaseOutcome(caseReferenceNumber, providerId, loginId, null),
            caseReferenceNumber);
    caabApiClient.createFinancialAward(caseOutcomeId, loginId, financialAward).block();
  }

  /** Updates a financial award without replacing the owning case outcome aggregate. */
  public void updateFinancialAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final Integer financialAwardId,
      final FinancialAwardRequest financialAward,
      final String loginId) {
    final CaseOutcomeDetail caseOutcome =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: "
                            + caseReferenceNumber));
    final Integer caseOutcomeId = requireCaseOutcomeId(caseOutcome, caseReferenceNumber);
    Optional.ofNullable(caseOutcome.getFinancialAwards()).orElse(Collections.emptyList()).stream()
        .filter(award -> financialAwardId.equals(award.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Financial award %s does not belong to case reference number: %s"
                        .formatted(financialAwardId, caseReferenceNumber)));
    caabApiClient
        .updateFinancialAward(caseOutcomeId, financialAwardId, loginId, financialAward)
        .block();
  }

  /** Returns a cost award only when it belongs to the supplied case outcome. */
  public Optional<CostAwardDetail> getCostAward(
      final String caseReferenceNumber, final Integer providerId, final Integer costAwardId) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getCaseOutcome(caseReferenceNumber, providerId)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No case outcome exists for case reference number: "
                                + caseReferenceNumber)),
            caseReferenceNumber);
    return Optional.ofNullable(caabApiClient.getCostAward(caseOutcomeId, costAwardId).block());
  }

  /** Creates a cost award without replacing the owning case outcome aggregate. */
  public void createCostAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final CostAwardDetail costAward,
      final String loginId) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getOrCreateCaseOutcome(caseReferenceNumber, providerId, loginId, null),
            caseReferenceNumber);
    caabApiClient.createCostAward(caseOutcomeId, loginId, costAward).block();
  }

  /** Updates a cost award without replacing the owning case outcome aggregate. */
  public void updateCostAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final Integer costAwardId,
      final CostAwardDetail costAward,
      final String loginId) {
    final CaseOutcomeDetail caseOutcome =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: "
                            + caseReferenceNumber));
    final Integer caseOutcomeId = requireCaseOutcomeId(caseOutcome, caseReferenceNumber);
    Optional.ofNullable(caseOutcome.getCostAwards()).orElse(Collections.emptyList()).stream()
        .filter(award -> costAwardId.equals(award.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Cost award %s does not belong to case reference number: %s"
                        .formatted(costAwardId, caseReferenceNumber)));
    caabApiClient.updateCostAward(caseOutcomeId, costAwardId, loginId, costAward).block();
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
      caseOutcome = initialiseCaseOutcomeForCreate(caseReferenceNumber, providerId, null);
      caseOutcome.addProceedingOutcomesItem(proceedingOutcome);
    }

    caabApiClient.createCaseOutcome(loginId, caseOutcome).block();
  }

  /**
   * Clears a single proceeding outcome from the case outcome record. Because the CAAB API has no
   * PATCH endpoint for case outcomes, the existing record is deleted and recreated with a
   * proceeding-level clear marker. This allows the display layer to suppress EBS fallback for the
   * specifically cleared proceeding while still hiding the clear action (marker has no clearable
   * outcome data).
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param proceedingCaseId - the proceeding case id whose outcome should be cleared.
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
    caseOutcome.addProceedingOutcomesItem(buildClearedProceedingOutcomeMarker(proceedingCaseId));

    caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId).block();
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
    if (source.getCostAwards() != null) {
      copy.setCostAwards(new ArrayList<>(source.getCostAwards()));
    }
    if (source.getFinancialAwards() != null) {
      copy.setFinancialAwards(new ArrayList<>(source.getFinancialAwards()));
    }
    if (source.getLandAwards() != null) {
      copy.setLandAwards(new ArrayList<>(source.getLandAwards()));
    }
    if (source.getOtherAssetAwards() != null) {
      copy.setOtherAssetAwards(new ArrayList<>(source.getOtherAssetAwards()));
    }
    return copy;
  }

  private CaseOutcomeDetail createAndReloadCaseOutcome(
      final String caseReferenceNumber,
      final Integer providerId,
      final String loginId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    final CaseOutcomeDetail caseOutcomeToCreate =
        initialiseCaseOutcomeForCreate(caseReferenceNumber, providerId, bootstrapCaseOutcome);
    try {
      caabApiClient.createCaseOutcome(loginId, caseOutcomeToCreate).block();
    } catch (CaabApiClientException ex) {
      if (!ex.hasHttpStatus(HttpStatus.CONFLICT)) {
        throw ex;
      }
      log.info(
          "Case outcome already exists for case reference number: {}. Reloading after conflict.",
          caseReferenceNumber,
          ex);
    }
    return getCaseOutcome(caseReferenceNumber, providerId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Created case outcome could not be reloaded for case reference number: "
                        + caseReferenceNumber));
  }

  private CaseOutcomeDetail initialiseCaseOutcomeForCreate(
      final String caseReferenceNumber,
      final Integer providerId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    final CaseOutcomeDetail caseOutcome =
        bootstrapCaseOutcome == null
            ? new CaseOutcomeDetail()
            : copyCaseOutcomeForCreate(bootstrapCaseOutcome);
    caseOutcome.setId(null);
    caseOutcome.setCaseReferenceNumber(caseReferenceNumber);
    caseOutcome.setProviderId(String.valueOf(providerId));
    return caseOutcome;
  }

  private Integer requireCaseOutcomeId(
      final CaseOutcomeDetail caseOutcome, final String caseReferenceNumber) {
    if (caseOutcome.getId() == null) {
      throw new IllegalStateException(
          "Case outcome record exists but has no id for case reference number: "
              + caseReferenceNumber);
    }
    return caseOutcome.getId();
  }

  private ProceedingOutcomeDetail buildClearedProceedingOutcomeMarker(
      final String proceedingCaseId) {
    return new ProceedingOutcomeDetail().proceedingCaseId(proceedingCaseId);
  }
}
