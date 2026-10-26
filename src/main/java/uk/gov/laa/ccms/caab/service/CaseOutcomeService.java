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
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryRequest;

/** Service class to handle Case Outcomes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseOutcomeService {

  private final CaabApiClient caabApiClient;
  private final CostAwardMapper costAwardMapper;

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
      final String loginId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getOrCreateCaseOutcome(
                caseReferenceNumber,
                providerId,
                loginId,
                sanitiseAwardBootstrapCaseOutcome(bootstrapCaseOutcome)),
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
      final String loginId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            getOrCreateCaseOutcome(
                caseReferenceNumber,
                providerId,
                loginId,
                sanitiseAwardBootstrapCaseOutcome(bootstrapCaseOutcome)),
            caseReferenceNumber);
    caabApiClient.createCostAward(caseOutcomeId, loginId, costAward).block();
  }

  /** Updates a cost award without replacing the owning case outcome aggregate. */
  public void updateCostAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final Integer costAwardId,
      final CostAwardFormData costAwardFormData,
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
    final CostAwardDetail existingCostAward =
        Optional.ofNullable(caabApiClient.getCostAward(caseOutcomeId, costAwardId).block())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cost award %s could not be loaded for case reference number: %s"
                            .formatted(costAwardId, caseReferenceNumber)));
    costAwardMapper.updateCostAward(costAwardFormData, existingCostAward);
    caabApiClient.updateCostAward(caseOutcomeId, costAwardId, loginId, existingCostAward).block();
  }

  /** Returns an other asset award only when it belongs to the supplied case outcome. */
  public Optional<OtherAssetAwardDetail> getOtherAssetAward(
      final String caseReferenceNumber, final Integer providerId, final Integer otherAssetAwardId) {
    final Integer caseOutcomeId =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: " + caseReferenceNumber))
            .getId();
    return Optional.ofNullable(
        caabApiClient.getOtherAssetAward(caseOutcomeId, otherAssetAwardId).block());
  }

  /** Creates an other asset award without replacing the owning case outcome aggregate. */
  public Integer createOtherAssetAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final OtherAssetAwardRequest otherAssetAward,
      final String loginId) {
    final Integer caseOutcomeId =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: " + caseReferenceNumber))
            .getId();
    final String id =
        caabApiClient.createOtherAssetAward(caseOutcomeId, loginId, otherAssetAward).block();
    if (id == null) {
      throw new IllegalStateException("No id returned for the newly created other asset award");
    }
    return Integer.valueOf(id);
  }

  /** Updates an other asset award without replacing the owning case outcome aggregate. */
  public void updateOtherAssetAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final Integer otherAssetAwardId,
      final OtherAssetAwardRequest otherAssetAward,
      final String loginId) {
    final CaseOutcomeDetail caseOutcome =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: "
                            + caseReferenceNumber));
    Optional.ofNullable(caseOutcome.getOtherAssetAwards()).orElse(Collections.emptyList()).stream()
        .filter(award -> otherAssetAwardId.equals(award.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Other asset award %s does not belong to case reference number: %s"
                        .formatted(otherAssetAwardId, caseReferenceNumber)));
    caabApiClient
        .updateOtherAssetAward(caseOutcome.getId(), otherAssetAwardId, loginId, otherAssetAward)
        .block();
  }

  /** Returns a land award only when it belongs to the supplied case outcome. */
  public Optional<LandAwardDetail> getLandAward(
      final String caseReferenceNumber, final Integer providerId, final Integer landAwardId) {
    final Integer caseOutcomeId =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: " + caseReferenceNumber))
            .getId();
    return Optional.ofNullable(caabApiClient.getLandAward(caseOutcomeId, landAwardId).block());
  }

  /** Creates a land award without replacing the owning case outcome aggregate. */
  public Integer createLandAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final LandAwardRequest landAward,
      final String loginId) {
    final Integer caseOutcomeId =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: " + caseReferenceNumber))
            .getId();
    final String id = caabApiClient.createLandAward(caseOutcomeId, loginId, landAward).block();
    if (id == null) {
      throw new IllegalStateException("No id returned for the newly created land award");
    }
    return Integer.valueOf(id);
  }

  /** Updates a land award without replacing the owning case outcome aggregate. */
  public void updateLandAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final Integer landAwardId,
      final LandAwardRequest landAward,
      final String loginId) {
    final CaseOutcomeDetail caseOutcome =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: "
                            + caseReferenceNumber));
    Optional.ofNullable(caseOutcome.getLandAwards()).orElse(Collections.emptyList()).stream()
        .filter(award -> landAwardId.equals(award.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Land award %s does not belong to case reference number: %s"
                        .formatted(landAwardId, caseReferenceNumber)));
    caabApiClient.updateLandAward(caseOutcome.getId(), landAwardId, loginId, landAward).block();
  }

  /**
   * Saves a time-related recovery against a land or other asset award in the current case outcome.
   */
  public void upsertTimeRecovery(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardType,
      final Integer awardId,
      final TimeRecoveryRequest recovery,
      final String loginId) {
    final Integer caseOutcomeId =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No case outcome exists for case reference number: " + caseReferenceNumber))
            .getId();
    caabApiClient.upsertTimeRecovery(caseOutcomeId, awardType, awardId, loginId, recovery).block();
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
   * @param bootstrapCaseOutcome - the EBS case outcome used to seed a new local record, if any.
   */
  public void updateProceedingOutcome(
      final String caseReferenceNumber,
      final Integer providerId,
      final ProceedingOutcomeDetail proceedingOutcome,
      final String loginId,
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {

    Optional<CaseOutcomeDetail> existing = getCaseOutcome(caseReferenceNumber, providerId);

    if (existing.isEmpty()) {
      final CaseOutcomeDetail caseOutcome =
          initialiseCaseOutcomeForCreate(caseReferenceNumber, providerId, bootstrapCaseOutcome);
      replaceProceedingOutcome(caseOutcome, proceedingOutcome);
      caabApiClient.createCaseOutcome(loginId, caseOutcome).block();
      return;
    }

    final CaseOutcomeDetail existingCaseOutcome = existing.get();
    final Integer existingCaseOutcomeId =
        requireCaseOutcomeId(existingCaseOutcome, caseReferenceNumber);

    final CaseOutcomeDetail rollbackCaseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);
    final CaseOutcomeDetail caseOutcome = copyCaseOutcomeForCreate(existingCaseOutcome);
    replaceProceedingOutcome(caseOutcome, proceedingOutcome);

    caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId).block();
    recreateCaseOutcomeWithRollback(caseReferenceNumber, loginId, caseOutcome, rollbackCaseOutcome);
  }

  private void replaceProceedingOutcome(
      final CaseOutcomeDetail caseOutcome, final ProceedingOutcomeDetail proceedingOutcome) {
    if (caseOutcome.getProceedingOutcomes() == null) {
      caseOutcome.setProceedingOutcomes(new ArrayList<>());
    }
    caseOutcome
        .getProceedingOutcomes()
        .removeIf(
            o ->
                proceedingOutcome.getProceedingCaseId() != null
                    && proceedingOutcome.getProceedingCaseId().equals(o.getProceedingCaseId()));
    caseOutcome.addProceedingOutcomesItem(proceedingOutcome);
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
      copy.setProceedingOutcomes(
          source.getProceedingOutcomes().stream()
              .map(this::copyProceedingOutcome)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    if (source.getCostAwards() != null) {
      copy.setCostAwards(
          source.getCostAwards().stream()
              .map(this::copyCostAward)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    if (source.getFinancialAwards() != null) {
      copy.setFinancialAwards(
          source.getFinancialAwards().stream()
              .map(this::copyFinancialAward)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    if (source.getLandAwards() != null) {
      copy.setLandAwards(
          source.getLandAwards().stream()
              .map(this::copyLandAward)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    if (source.getOtherAssetAwards() != null) {
      copy.setOtherAssetAwards(
          source.getOtherAssetAwards().stream()
              .map(this::copyOtherAssetAward)
              .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    return copy;
  }

  private ProceedingOutcomeDetail copyProceedingOutcome(final ProceedingOutcomeDetail source) {
    final ProceedingOutcomeDetail copy = new ProceedingOutcomeDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
    return copy;
  }

  private CostAwardDetail copyCostAward(final CostAwardDetail source) {
    final CostAwardDetail copy = new CostAwardDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
    return copy;
  }

  private FinancialAwardDetail copyFinancialAward(final FinancialAwardDetail source) {
    final FinancialAwardDetail copy = new FinancialAwardDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
    return copy;
  }

  private LandAwardDetail copyLandAward(final LandAwardDetail source) {
    final LandAwardDetail copy = new LandAwardDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
    return copy;
  }

  private OtherAssetAwardDetail copyOtherAssetAward(final OtherAssetAwardDetail source) {
    final OtherAssetAwardDetail copy = new OtherAssetAwardDetail();
    BeanUtils.copyProperties(source, copy);
    copy.setId(null);
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

  private CaseOutcomeDetail sanitiseAwardBootstrapCaseOutcome(
      @Nullable final CaseOutcomeDetail bootstrapCaseOutcome) {
    if (bootstrapCaseOutcome == null) {
      return null;
    }
    final CaseOutcomeDetail sanitised = copyCaseOutcomeForCreate(bootstrapCaseOutcome);
    sanitised.setProceedingOutcomes(null);
    return sanitised;
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
