package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryRequest;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;

/** Service class to handle Case Outcomes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseOutcomeService {

  private final CaabApiClient caabApiClient;
  private final LookupService lookupService;
  private final LiablePartiesDisplayMapper liablePartiesDisplayMapper;
  private final CostAwardMapper costAwardMapper;
  private final OpponentMapper opponentMapper;

  /** Loads both pages of an editable individual linked to the requested award. */
  public IndividualLiablePartyFlow getIndividualLiableParty(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final Integer opponentId) {
    final var opponent =
        requireIndividualLiableParty(caseReference, providerId, awardPath, awardId, opponentId)
            .opponent();
    final var flow =
        new IndividualLiablePartyFlow(caseReference, providerId, awardPath, awardId, opponentId);
    flow.setContactDetails(
        opponentMapper.toIndividualOpponentFormData(opponent, null, null, null, null, true));
    flow.setOtherDetails(opponentMapper.toIndividualOpponentOtherDetailsFormData(opponent));
    return flow;
  }

  /** Saves contact changes on Next without persisting draft changes from Other Details. */
  public void updateIndividualLiablePartyContactDetails(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final Integer opponentId,
      final String loginId,
      final IndividualOpponentFormData contact) {
    final var party =
        requireIndividualLiableParty(caseReference, providerId, awardPath, awardId, opponentId);
    final var updated = opponentMapper.toIndividualOpponent(contact);
    opponentMapper.updateIndividualOtherDetails(
        opponentMapper.toIndividualOpponentOtherDetailsFormData(party.opponent()), updated);
    updateIndividualLiableParty(party, loginId, updated);
  }

  /** Saves Other Details while retaining the latest persisted contact details. */
  public void updateIndividualLiablePartyOtherDetails(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final Integer opponentId,
      final String loginId,
      final IndividualOpponentOtherDetailsFormData other) {
    final var party =
        requireIndividualLiableParty(caseReference, providerId, awardPath, awardId, opponentId);
    opponentMapper.updateIndividualOtherDetails(other, party.opponent());
    updateIndividualLiableParty(party, loginId, party.opponent());
  }

  /**
   * Validated ownership context for updating an award-linked individual.
   *
   * @param outcome the owning case outcome
   * @param award the award to which the opponent is linked
   * @param opponent the editable individual
   */
  private record AwardParty(
      CaseOutcomeDetail outcome, BaseAwardDetail award, OpponentDetail opponent) {}

  /**
   * Loads an award-linked individual and checks that it is award-created and not shared.
   *
   * @param caseReference the case reference number
   * @param providerId the provider owning the case
   * @param awardPath the award type's URL path segment
   * @param awardId the award containing the party
   * @param opponentId the individual to load
   * @return the owning case outcome, award and editable opponent
   * @throws CaabApplicationException if the award or party is unavailable or not editable
   * @throws IllegalStateException if the case has no outcome
   */
  private AwardParty requireIndividualLiableParty(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final Integer opponentId) {
    final var outcome = requireCaseOutcome(caseReference, providerId);
    final var award = requireAward(outcome, awardTypeForPath(awardPath), awardId);
    final var opponents =
        caabApiClient
            .getAwardLiableParties(
                requireCaseOutcomeId(outcome, caseReference), award.getAwardType(), awardId)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve award parties"));
    final var opponent =
        opponents.stream()
            .filter(candidate -> opponentId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(
                () -> new CaabApplicationException("Opponent is not linked to the selected award"));
    if (!"Individual".equals(opponent.getType())
        || !StringUtils.startsWithIgnoreCase(opponent.getEbsId(), "AWARD")
        || Boolean.TRUE.equals(opponent.getSharedInd())) {
      throw new CaabApplicationException("This individual party cannot be edited");
    }
    return new AwardParty(outcome, award, opponent);
  }

  /**
   * Persists an opponent's details using its validated case-outcome and award scope.
   *
   * @param party the validated ownership and opponent identity
   * @param loginId the user performing the update
   * @param opponent the updated opponent payload
   */
  private void updateIndividualLiableParty(
      final AwardParty party, final String loginId, final OpponentDetail opponent) {
    caabApiClient
        .updateAwardLiableParty(
            party.outcome().getId(),
            party.award().getAwardType(),
            party.award().getId(),
            party.opponent().getId(),
            loginId,
            opponent)
        .block();
  }

  /** Checks the case/provider and award scope without loading unrelated opponent forms. */
  public void validatePartyAward(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId) {
    requireAward(
        requireCaseOutcome(caseReference, providerId), awardTypeForPath(awardPath), awardId);
  }

  /** Saves all individual details through the atomic outcome/award endpoint. */
  public void createIndividualLiableParty(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final String loginId,
      final IndividualOpponentFormData contact,
      final IndividualOpponentOtherDetailsFormData other) {
    final CaseOutcomeDetail outcome = requireCaseOutcome(caseReference, providerId);
    final BaseAwardDetail award = requireAward(outcome, awardTypeForPath(awardPath), awardId);
    final var opponent = opponentMapper.toIndividualOpponent(contact);
    opponentMapper.updateIndividualOtherDetails(other, opponent);
    caabApiClient
        .createAwardLiableParty(
            requireCaseOutcomeId(outcome, caseReference),
            award.getAwardType(),
            awardId,
            loginId,
            opponent)
        .block();
  }

  /** Deletes only the selected award, without replacing the case outcome aggregate. */
  public void deleteAward(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardType,
      final Integer awardId,
      final String loginId) {
    final CaseOutcomeDetail caseOutcome = requireCaseOutcome(caseReferenceNumber, providerId);
    requireAward(caseOutcome, awardType, awardId);
    caabApiClient
        .deleteAward(
            requireCaseOutcomeId(caseOutcome, caseReferenceNumber), awardType, awardId, loginId)
        .block();
  }

  private CaseOutcomeDetail requireCaseOutcome(
      final String caseReferenceNumber, final Integer providerId) {
    return getCaseOutcome(caseReferenceNumber, providerId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No case outcome exists for case reference number: " + caseReferenceNumber));
  }

  private BaseAwardDetail requireAward(
      final CaseOutcomeDetail caseOutcome, final String awardType, final Integer awardId) {
    final List<? extends BaseAwardDetail> awards =
        switch (awardType) {
          case AWARD_TYPE_COST -> caseOutcome.getCostAwards();
          case AWARD_TYPE_FINANCIAL -> caseOutcome.getFinancialAwards();
          case AWARD_TYPE_LAND -> caseOutcome.getLandAwards();
          case AWARD_TYPE_OTHER_ASSET -> caseOutcome.getOtherAssetAwards();
          default -> throw new IllegalArgumentException("Unsupported award type: " + awardType);
        };
    return Optional.ofNullable(awards).orElse(Collections.emptyList()).stream()
        .filter(award -> awardId.equals(award.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new CaabApplicationException(
                    "Could not find award with id: %s for case outcome: %s"
                        .formatted(awardId, caseOutcome.getId())));
  }

  /**
   * Resolves an award URL path segment to its API award type.
   *
   * @param awardPath the award type's URL path segment
   * @return the corresponding API award type
   * @throws CaabApplicationException if the path does not identify a supported award type
   */
  private String awardTypeForPath(final String awardPath) {
    return switch (awardPath) {
      case "cost-award" -> AWARD_TYPE_COST;
      case "financial-award" -> AWARD_TYPE_FINANCIAL;
      case "land" -> AWARD_TYPE_LAND;
      case "asset" -> AWARD_TYPE_OTHER_ASSET;
      default -> throw new CaabApplicationException("Unsupported award path: " + awardPath);
    };
  }

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

  /** Loads one award and its parties to build the liable or interested party summary. */
  public LiablePartiesDisplay getLiablePartiesDisplay(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardPath,
      final Integer awardId) {
    final CaseOutcomeDetail outcome =
        getCaseOutcome(caseReferenceNumber, providerId)
            .orElseThrow(() -> new CaabApplicationException("Could not find case outcome"));
    final BaseAwardDetail award = requireAwardByPath(outcome, awardPath, awardId);
    if (award.getLiableParties() == null || award.getLiableParties().isEmpty()) {
      return liablePartiesDisplayMapper.toLiablePartiesDisplay(award, List.of(), null);
    }
    return Mono.zip(
            caabApiClient.getAwardLiableParties(outcome.getId(), award.getAwardType(), awardId),
            lookupService.getLiablePartiesMappingContext())
        .map(
            tuple ->
                liablePartiesDisplayMapper.toLiablePartiesDisplay(
                    award, tuple.getT1(), tuple.getT2()))
        .blockOptional()
        .orElseThrow(
            () -> new CaabApplicationException("Failed to retrieve award parties or lookup data"));
  }

  /**
   * Loads the raw opponents already linked as liable/interested parties to one award, including
   * each opponent's {@code ebsId}. Unlike {@link #getLiablePartiesDisplay}, this is not shaped for
   * display and is intended for identity matching (for example, deciding whether an EBS-sourced
   * case opponent has already been added to a specific award) rather than rendering.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param awardPath - the award path segment identifying the award type.
   * @param awardId - the id of the award whose linked opponents should be loaded.
   * @return the opponents currently linked to the award, or an empty list if none are linked.
   */
  public List<OpponentDetail> getAwardLiableParties(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardPath,
      final Integer awardId) {
    final CaseOutcomeDetail outcome = requireCaseOutcome(caseReferenceNumber, providerId);
    final BaseAwardDetail award = requireAwardByPath(outcome, awardPath, awardId);
    if (award.getLiableParties() == null || award.getLiableParties().isEmpty()) {
      return List.of();
    }
    return caabApiClient
        .getAwardLiableParties(outcome.getId(), award.getAwardType(), awardId)
        .blockOptional()
        .orElse(List.of());
  }

  /** Finds the award, of any type, identified by the given award path and id. */
  private BaseAwardDetail requireAwardByPath(
      final CaseOutcomeDetail outcome, final String awardPath, final Integer awardId) {
    final List<? extends BaseAwardDetail> awards =
        switch (awardPath) {
          case "cost-award" -> outcome.getCostAwards();
          case "financial-award" -> outcome.getFinancialAwards();
          case "land" -> outcome.getLandAwards();
          case "asset" -> outcome.getOtherAssetAwards();
          default -> throw new CaabApplicationException("Unsupported award path: " + awardPath);
        };
    return (awards == null ? List.<BaseAwardDetail>of() : awards)
        .stream()
            .filter(candidate -> awardId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(
                () -> new CaabApplicationException("Could not find award with id: " + awardId));
  }

  /**
   * Links the supplied opponents to an award as liable/interested parties. Equivalent to {@link
   * #addLiableParties(String, Integer, String, Integer, List, List, String, Consumer, Consumer)}
   * with no-op callbacks, for callers that do not need to react to each individual link as it
   * succeeds (for example, as each one succeeds, in case a later one in the same batch fails).
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param awardPath - the award path segment identifying the award type.
   * @param awardId - the id of the award to link the parties to.
   * @param existingOpponentIds - the ids of already-known opponents to link to the award.
   * @param newOpponents - the full details of opponents with no existing CAAB id, to create and
   *     link to the award.
   * @param loginId - the login ID of the user performing the update.
   */
  public void addLiableParties(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final List<Integer> existingOpponentIds,
      final List<OpponentDetail> newOpponents,
      final String loginId) {
    addLiableParties(
        caseReferenceNumber,
        providerId,
        awardPath,
        awardId,
        existingOpponentIds,
        newOpponents,
        loginId,
        opponentId -> {},
        created -> {});
  }

  /**
   * Links the supplied opponents to an award as liable/interested parties, persisting each
   * association through the CAAB API's liable-parties endpoint rather than merging it into the
   * award aggregate (the award update endpoints do not persist liable party links). Opponent ids
   * already linked to the award are left untouched by the API as a no-op.
   *
   * <p>Opponents that already have a real CAAB opponent id (for example, those already linked to
   * another award on this case) are linked directly via {@code existingOpponentIds}. Opponents that
   * have never been linked before (for example, an EBS-sourced case opponent with no CAAB id yet)
   * are supplied in full via {@code newOpponents} and are created and linked in one atomic step,
   * since there is no existing id to link against.
   *
   * <p>{@code newOpponents} are created one at a time rather than in a single batch call, so if one
   * creation fails partway through, any opponents already created and linked before it remain
   * persisted. {@code onNewOpponentCreated} is invoked immediately after each individual creation
   * succeeds (not after the whole batch completes), so a caller tracking created opponents (for
   * example, to recognise them as already-added on a later page load) still records each success
   * even if a later creation in the same batch then fails and the exception propagates. Similarly,
   * {@code onExistingOpponentLinked} is invoked immediately after each existing opponent id is
   * successfully linked, so a caller can recognise it as already-added without waiting for (or
   * depending on) a subsequent read of the award's liable parties, which may not yet reflect a link
   * that was only just written.
   *
   * @param caseReferenceNumber - the case reference number.
   * @param providerId - the provider id.
   * @param awardPath - the award path segment identifying the award type.
   * @param awardId - the id of the award to link the parties to.
   * @param existingOpponentIds - the ids of already-known opponents to link to the award.
   * @param newOpponents - the full details of opponents with no existing CAAB id, to create and
   *     link to the award.
   * @param loginId - the login ID of the user performing the update.
   * @param onExistingOpponentLinked - invoked with each existing opponent's id immediately after it
   *     has been successfully linked.
   * @param onNewOpponentCreated - invoked with each new opponent's original details immediately
   *     after it has been successfully created and linked.
   */
  public void addLiableParties(
      final String caseReferenceNumber,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final List<Integer> existingOpponentIds,
      final List<OpponentDetail> newOpponents,
      final String loginId,
      final Consumer<Integer> onExistingOpponentLinked,
      final Consumer<OpponentDetail> onNewOpponentCreated) {
    final boolean hasExistingOpponents =
        existingOpponentIds != null && !existingOpponentIds.isEmpty();
    final boolean hasNewOpponents = newOpponents != null && !newOpponents.isEmpty();
    if (!hasExistingOpponents && !hasNewOpponents) {
      return;
    }
    final Integer caseOutcomeId =
        requireCaseOutcomeId(
            requireCaseOutcome(caseReferenceNumber, providerId), caseReferenceNumber);
    final String awardType = resolveAwardType(awardPath);
    if (hasExistingOpponents) {
      final Set<Integer> distinctOpponentIds = new LinkedHashSet<>();
      for (final Integer opponentId : existingOpponentIds) {
        if (opponentId != null) {
          distinctOpponentIds.add(opponentId);
        }
      }
      for (final Integer opponentId : distinctOpponentIds) {
        caabApiClient
            .addAwardLiableParty(caseOutcomeId, awardType, awardId, opponentId, loginId)
            .block();
        onExistingOpponentLinked.accept(opponentId);
      }
    }
    if (hasNewOpponents) {
      for (final OpponentDetail opponent : newOpponents) {
        if (opponent != null) {
          caabApiClient
              .createAwardLiableParty(caseOutcomeId, awardType, awardId, opponent, loginId)
              .block();
          onNewOpponentCreated.accept(opponent);
        }
      }
    }
  }

  /** Maps the award path segment used in URLs to the award type expected by the CAAB API. */
  private String resolveAwardType(final String awardPath) {
    return switch (awardPath) {
      case "cost-award" -> AWARD_TYPE_COST;
      case "financial-award" -> AWARD_TYPE_FINANCIAL;
      case "land" -> AWARD_TYPE_LAND;
      case "asset" -> AWARD_TYPE_OTHER_ASSET;
      default -> throw new CaabApplicationException("Unsupported award path: " + awardPath);
    };
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
  public Integer createFinancialAward(
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
    final String id =
        caabApiClient.createFinancialAward(caseOutcomeId, loginId, financialAward).block();
    if (id == null) {
      throw new IllegalStateException("No id returned for the newly created financial award");
    }
    return Integer.valueOf(id);
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
  public Integer createCostAward(
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
    final String id = caabApiClient.createCostAward(caseOutcomeId, loginId, costAward).block();
    if (id == null) {
      throw new IllegalStateException("No id returned for the newly created cost award");
    }
    return Integer.valueOf(id);
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
