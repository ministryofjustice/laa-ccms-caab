package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.mapper.NullableMapper.unwrap;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDischargeRequest;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Initial outcome submission checks and scalar saves; never submits to EBS. */
@Service
@RequiredArgsConstructor
public class OutcomeSubmissionService {
  private final CaseOutcomeService caseOutcomeService;
  private final CaabApiClient client;

  /** Enforces the same submission permission for initial Submit and discharge requests. */
  public void requireSubmissionPermission(
      final ApplicationDetail currentCase, final UserDetail user) {
    if (user.getFunctions() == null
        || !user.getFunctions().contains(UserRole.SUBMIT_CASE_OUTCOME.getCode())
        || currentCase.getAvailableFunctions() == null
        || !(currentCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_WITH_DISCHARGE)
            || currentCase
                .getAvailableFunctions()
                .contains(FunctionConstants.OUTCOME_NO_DISCHARGE))) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Outcome submission is not permitted");
    }
  }

  /** Applies legacy's preparatory cleanup only for the initial Outcome and Awards Submit. */
  public CaseOutcomeDetail prepareSubmission(
      final ApplicationDetail currentCase, final UserDetail user) {
    final CaseOutcomeDetail draft =
        caseOutcomeService.requireCaseOutcome(
            currentCase.getCaseReferenceNumber(), user.getProvider().getId().intValue());
    return client
        .prepareCaseOutcomeSubmission(
            draft.getId(),
            user.getLoginId(),
            new CaseOutcomeDetail()
                .caseReferenceNumber(currentCase.getCaseReferenceNumber())
                .providerId(user.getProvider().getId().toString()))
        .blockOptional()
        .orElseThrow(() -> new IllegalStateException("No prepared outcome returned"));
  }

  /** Legacy POD permission and proceeding completeness gate, recalculated on every request. */
  public boolean canDischarge(
      final ApplicationDetail currentCase, final UserDetail user, final CaseOutcomeDetail draft) {
    if (currentCase.getAvailableFunctions() == null
        || !currentCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_WITH_DISCHARGE)
        || user.getFunctions() == null
        || !user.getFunctions().contains(UserRole.REQUEST_CASE_DISCHARGE.getCode())) {
      return false;
    }
    return list(currentCase.getProceedings()).stream()
        .allMatch(
            proceeding -> {
              final ProceedingOutcomeDetail local =
                  list(draft.getProceedingOutcomes()).stream()
                      .filter(
                          outcome ->
                              Objects.equals(
                                  proceeding.getProceedingCaseId(), outcome.getProceedingCaseId()))
                      .findFirst()
                      .orElse(null);
              // Legacy counts any persisted local outcome, including a cleared one, as complete.
              return local != null || complete(proceeding.getOutcome());
            });
  }

  private boolean complete(final ProceedingOutcomeDetail outcome) {
    return outcome != null
        && outcome.getDateOfFinalWork() != null
        && outcome.getDateOfFinalWork().getTime() > 0
        && outcome.getStageEnd() != null
        && StringUtils.hasText(outcome.getStageEnd().getId())
        && outcome.getResult() != null
        && StringUtils.hasText(outcome.getResult().getId())
        && StringUtils.hasText(outcome.getResolutionMethod())
        && StringUtils.hasText(outcome.getAlternativeResolution())
        && StringUtils.hasText(outcome.getWiderBenefits());
  }

  /** Validates the cross-award requirements checked by legacy's initial Submit. */
  public List<String> awardErrors(final CaseOutcomeDetail draft) {
    final List<String> errors = new ArrayList<>();
    if (list(draft.getCostAwards()).stream().anyMatch(a -> list(a.getLiableParties()).isEmpty())) {
      errors.add("case.outcomeSubmission.costParties");
    }
    if (list(draft.getFinancialAwards()).stream()
        .anyMatch(a -> list(a.getLiableParties()).isEmpty())) {
      errors.add("case.outcomeSubmission.financialParties");
    }
    if (list(draft.getFinancialAwards()).stream()
        .anyMatch(a -> !StringUtils.hasText(a.getInterimAward()))) {
      errors.add("case.outcomeSubmission.interimAward");
    }
    return errors;
  }

  public void saveDischarge(
      final CaseOutcomeDetail draft, final CaseDischargeFormData form, final String loginId) {
    final CaseOutcomeDischargeRequest details =
        new CaseOutcomeDischargeRequest()
            .caseReferenceNumber(draft.getCaseReferenceNumber())
            .providerId(draft.getProviderId())
            .dischargeCaseInd(form.getDischargeCaseInd())
            .dischargeReason(text(form.getDischargeReason()))
            .clientContinueInd(form.getClientContinueInd())
            .otherDetails(text(form.getOtherDetails()));
    client.updateCaseOutcomeDischarge(draft.getId(), loginId, details).block();
  }

  public void clearDischarge(final CaseOutcomeDetail draft, final String loginId) {
    if (draft.getDischargeCaseInd() != null
        || draft.getClientContinueInd() != null
        || StringUtils.hasText(draft.getDischargeReason())
        || StringUtils.hasText(draft.getOtherDetails())) {
      saveDischarge(draft, new CaseDischargeFormData(), loginId);
    }
  }

  /**
   * Compares business data, not generated ids, audit information, UI flags or lookup labels.
   * Discharge fields and office/file references are deliberately excluded, as in legacy's non-POD
   * submission guard.
   */
  public boolean hasChanges(final ApplicationDetail currentCase, final CaseOutcomeDetail draft) {
    final CaseOutcomeDetail baseline =
        currentCase.getCaseOutcome() == null
            ? new CaseOutcomeDetail()
            : currentCase.getCaseOutcome();
    if (!values(
            Objects.requireNonNullElse(unwrap(draft.getPreCertificateCosts()), BigDecimal.ZERO),
            Objects.requireNonNullElse(unwrap(draft.getLegalCosts()), BigDecimal.ZERO))
        .equals(
            values(
                Objects.requireNonNullElse(
                    unwrap(baseline.getPreCertificateCosts()), BigDecimal.ZERO),
                Objects.requireNonNullElse(unwrap(baseline.getLegalCosts()), BigDecimal.ZERO)))) {
      return true;
    }
    final List<ProceedingOutcomeDetail> resolved = new ArrayList<>();
    final List<ProceedingOutcomeDetail> originals = new ArrayList<>();
    for (final var proceeding : list(currentCase.getProceedings())) {
      final var original = proceeding.getOutcome();
      final var local =
          list(draft.getProceedingOutcomes()).stream()
              .filter(
                  outcome ->
                      Objects.equals(
                          proceeding.getProceedingCaseId(), outcome.getProceedingCaseId()))
              .findFirst()
              .orElse(original);
      if (local != null) {
        resolved.add(local);
      }
      if (original != null) {
        originals.add(original);
      }
    }
    return !same(resolved, originals, this::proceedingValues)
        || !same(draft.getCostAwards(), baseline.getCostAwards(), this::awardValues)
        || !same(draft.getFinancialAwards(), baseline.getFinancialAwards(), this::awardValues)
        || !same(draft.getLandAwards(), baseline.getLandAwards(), this::awardValues)
        || !same(draft.getOtherAssetAwards(), baseline.getOtherAssetAwards(), this::awardValues)
        || matchedAwardsChanged(currentCase, draft, baseline);
  }

  private List<Object> proceedingValues(final ProceedingOutcomeDetail outcome) {
    return values(
        outcome.getProceedingCaseId(),
        outcome.getDateOfFinalWork(),
        outcome.getStageEnd(),
        outcome.getResolutionMethod(),
        outcome.getResult(),
        outcome.getResultInfo(),
        outcome.getAlternativeResolution(),
        outcome.getAdrInfo(),
        outcome.getWiderBenefits(),
        ProceedingOutcomeFormData.normaliseCourtValue(outcome.getCourtCode()),
        ProceedingOutcomeFormData.normaliseCourtValue(outcome.getCourtName()),
        outcome.getOutcomeCourtCaseNo());
  }

  private List<Object> awardValues(final BaseAwardDetail award) {
    final List<Object> result =
        new ArrayList<>(
            values(
                award.getAwardType(),
                award.getDateOfOrder(),
                award.getAwardedBy(),
                award.getDescription(),
                award.getAwardCode()));
    if (award instanceof CostAwardDetail cost) {
      result.add(
          values(
              cost.getCourtAssessmentStatus(),
              cost.getPreCertificateLscCost(),
              cost.getPreCertificateOtherCost(),
              cost.getCertificateCostLsc(),
              cost.getCertificateCostMarket(),
              cost.getInterestAwardedRate(),
              cost.getOrderServedDate(),
              cost.getInterestStartDate(),
              cost.getOtherDetails(),
              cost.getAddressLine1(),
              cost.getAddressLine2(),
              cost.getAddressLine3(),
              recoveryValues(cost.getRecovery())));
    } else if (award instanceof FinancialAwardDetail financial) {
      result.add(
          values(
              financial.getAwardAmount(),
              financial.getInterimAward(),
              financial.getAwardJustifications(),
              financial.getOrderServedDate(),
              financial.getStatutoryChargeExemptReason(),
              financial.getOtherDetails(),
              financial.getAddressLine1(),
              financial.getAddressLine2(),
              financial.getAddressLine3(),
              recoveryValues(financial.getRecovery())));
    } else if (award instanceof LandAwardDetail land) {
      result.add(
          values(
              land.getTitleNumber(),
              land.getValuationAmount(),
              land.getValuationCriteria(),
              land.getValuationDate(),
              land.getDisputedPercentage(),
              land.getAwardedPercentage(),
              land.getMortgageAmountDue(),
              land.getEquity(),
              land.getRecovery(),
              land.getNoRecoveryDetails(),
              land.getStatutoryChargeExemptReason(),
              land.getLandChargeRegistration(),
              land.getRegistrationReference(),
              land.getRecoveryOfAwardTimeRelated(),
              land.getAddressLine1(),
              land.getAddressLine2(),
              land.getAddressLine3(),
              timeValues(land.getTimeRecovery())));
    } else if (award instanceof OtherAssetAwardDetail asset) {
      result.add(
          values(
              asset.getValuationAmount(),
              asset.getValuationCriteria(),
              asset.getValuationDate(),
              asset.getDisputedPercentage(),
              asset.getAwardedPercentage(),
              asset.getRecoveredAmount(),
              asset.getDisputedAmount(),
              asset.getAwardedAmount(),
              asset.getRecoveredPercentage(),
              asset.getRecovery(),
              asset.getNoRecoveryDetails(),
              asset.getStatutoryChargeExemptReason(),
              asset.getRecoveryOfAwardTimeRelated(),
              timeValues(asset.getTimeRecovery())));
    }
    return result;
  }

  private Object recoveryValues(final RecoveryDetail recovery) {
    return recovery == null
        ? ""
        : values(
            recovery.getSolicitorRecoveryAmount(),
            recovery.getSolicitorRecoveryDate(),
            recovery.getSolicitorAmountPaidToLsc(),
            recovery.getCourtRecoveryAmount(),
            recovery.getCourtRecoveryDate(),
            recovery.getCourtAmountPaidToLsc(),
            recovery.getClientRecoveryAmount(),
            recovery.getClientRecoveryDate(),
            recovery.getClientAmountPaidToLsc(),
            recovery.getLeaveOfCourtRequiredInd(),
            recovery.getOfferedAmount(),
            recovery.getOfferDetails());
  }

  private Object timeValues(final TimeRecoveryDetail recovery) {
    return recovery == null
        ? ""
        : values(
            recovery.getAwardAmount(),
            recovery.getTriggeringEvent(),
            recovery.getEffectiveDate(),
            recovery.getTimeRelatedRecoveryDetails());
  }

  private boolean matchedAwardsChanged(
      final ApplicationDetail currentCase,
      final CaseOutcomeDetail draft,
      final CaseOutcomeDetail baseline) {
    final List<BaseAwardDetail> originalAwards = awards(baseline);
    for (final var award : awards(draft)) {
      final var original =
          originalAwards.stream()
              .filter(
                  a ->
                      Objects.equals(a.getEbsId(), award.getEbsId())
                          && Objects.equals(a.getAwardType(), award.getAwardType()))
              .findFirst()
              .orElse(null);
      if (original == null
          || !awardValues(award).equals(awardValues(original))
          || list(award.getLiableParties()).size() != list(original.getLiableParties()).size()) {
        return true;
      }
      if (!list(award.getLiableParties()).isEmpty()) {
        final List<OpponentDetail> localParties =
            client
                .getAwardLiableParties(draft.getId(), award.getAwardType(), award.getId())
                .blockOptional()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No liable party response returned for outcome submission"));
        final List<OpponentDetail> originalParties = new ArrayList<>();
        for (final var party : original.getLiableParties()) {
          final var opponent =
              list(currentCase.getOpponents()).stream()
                  .filter(
                      o ->
                          party.getOpponentId() != null
                              && Objects.equals(o.getEbsId(), party.getOpponentId().toString()))
                  .findFirst();
          if (opponent.isEmpty()) {
            return true;
          }
          originalParties.add(opponent.get());
        }
        if (!same(localParties, originalParties, this::opponentValues)) {
          return true;
        }
      }
    }
    return false;
  }

  private List<Object> opponentValues(final OpponentDetail opponent) {
    final var address = opponent.getAddress();
    return values(
        opponent.getEbsId(),
        opponent.getType(),
        opponent.getTitle(),
        opponent.getFirstName(),
        opponent.getMiddleNames(),
        opponent.getSurname(),
        opponent.getDateOfBirth(),
        opponent.getNationalInsuranceNumber(),
        address == null
            ? ""
            : values(
                address.getHouseNameOrNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getCounty(),
                address.getCountry(),
                address.getPostcode()),
        opponent.getRelationshipToCase(),
        opponent.getRelationshipToClient(),
        opponent.getTelephoneHome(),
        opponent.getTelephoneWork(),
        opponent.getTelephoneMobile(),
        opponent.getFaxNumber(),
        opponent.getEmailAddress(),
        opponent.getOtherInformation(),
        opponent.getEmploymentStatus(),
        opponent.getEmployerName(),
        opponent.getEmployerAddress(),
        unwrap(opponent.getLegalAided()),
        opponent.getCertificateNumber(),
        unwrap(opponent.getCourtOrderedMeansAssessment()),
        opponent.getAssessedIncome(),
        opponent.getAssessedIncomeFrequency(),
        opponent.getAssessedAssets(),
        opponent.getAssessmentDate(),
        opponent.getOrganisationName(),
        opponent.getOrganisationType(),
        opponent.getCurrentlyTrading(),
        opponent.getContactNameRole());
  }

  private List<BaseAwardDetail> awards(final CaseOutcomeDetail outcome) {
    return Stream.of(
            list(outcome.getCostAwards()),
            list(outcome.getFinancialAwards()),
            list(outcome.getLandAwards()),
            list(outcome.getOtherAssetAwards()))
        .flatMap(List::stream)
        .map(BaseAwardDetail.class::cast)
        .toList();
  }

  private <T> boolean same(
      final List<T> left, final List<T> right, final Function<T, List<Object>> projection) {
    final List<List<Object>> unmatched =
        new ArrayList<>(list(right).stream().map(projection).toList());
    for (final T item : list(left)) {
      if (!unmatched.remove(projection.apply(item))) {
        return false;
      }
    }
    return unmatched.isEmpty();
  }

  private List<Object> values(final Object... fields) {
    return Arrays.stream(fields)
        .map(
            value -> {
              if (value instanceof BigDecimal decimal) {
                return (Object) decimal.stripTrailingZeros();
              }
              if (value instanceof StringDisplayValue lookup) {
                return Objects.requireNonNullElse(lookup.getId(), "");
              }
              if (value instanceof Date date) {
                return Instant.ofEpochMilli(date.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
              }
              return value == null ? "" : value;
            })
        .toList();
  }

  private static <T> List<T> list(final List<T> value) {
    return value == null ? Collections.emptyList() : value;
  }

  private String text(final String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
