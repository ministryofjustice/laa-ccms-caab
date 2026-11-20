package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.controller.application.ApplicationTestUtils;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDischargeRequest;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LiablePartyDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class OutcomeSubmissionServiceTest {
  @Mock private CaseOutcomeService outcomes;
  @Mock private CaabApiClient client;
  @InjectMocks private OutcomeSubmissionService service;
  private ApplicationDetail currentCase;
  private CaseOutcomeDetail draft;
  private UserDetail user;

  @BeforeEach
  void setup() {
    user = ApplicationTestUtils.buildUser();
    user.setFunctions(List.of("POD", "SUBOUT"));
    currentCase =
        new ApplicationDetail().caseReferenceNumber("case").availableFunctions(List.of("POD"));
    draft = new CaseOutcomeDetail().id(42).caseReferenceNumber("case").providerId("1");
  }

  @Test
  void initialSubmissionLoadsAndPreparesOnlyCurrentProviderCase() {
    when(outcomes.requireCaseOutcome("case", user.getProvider().getId().intValue()))
        .thenReturn(draft);
    final var scope =
        new CaseOutcomeDetail()
            .caseReferenceNumber("case")
            .providerId(user.getProvider().getId().toString());
    final var cleaned = new CaseOutcomeDetail().id(42);
    when(client.prepareCaseOutcomeSubmission(42, user.getLoginId(), scope))
        .thenReturn(Mono.just(cleaned));
    assertThat(service.prepareSubmission(currentCase, user)).isSameAs(cleaned);
    verify(outcomes).requireCaseOutcome("case", user.getProvider().getId().intValue());
    verifyNoMoreInteractions(outcomes);
    verify(client).prepareCaseOutcomeSubmission(42, user.getLoginId(), scope);
    verifyNoMoreInteractions(client);
  }

  @Test
  void preparationFailureDoesNotReturnTheUncleanedDraft() {
    when(outcomes.requireCaseOutcome("case", user.getProvider().getId().intValue()))
        .thenReturn(draft);
    when(client.prepareCaseOutcomeSubmission(eq(42), eq(user.getLoginId()), any()))
        .thenReturn(Mono.empty());
    assertThatThrownBy(() -> service.prepareSubmission(currentCase, user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No prepared outcome returned");
  }

  @Test
  void requiredOutcomeFailureNeverAttemptsCleanup() {
    final var failure = new IllegalStateException("No persisted outcome with an id");
    when(outcomes.requireCaseOutcome("case", user.getProvider().getId().intValue()))
        .thenThrow(failure);
    assertThatThrownBy(() -> service.prepareSubmission(currentCase, user)).isSameAs(failure);
    verifyNoInteractions(client);
  }

  @Test
  void requiresUserAndCaseSubmissionPermission() {
    service.requireSubmissionPermission(currentCase, user);
    currentCase.setAvailableFunctions(List.of("PO"));
    service.requireSubmissionPermission(currentCase, user);
    user.setFunctions(List.of("POD"));
    assertThatThrownBy(() -> service.requireSubmissionPermission(currentCase, user))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    user.setFunctions(List.of("SUBOUT"));
    currentCase.setAvailableFunctions(List.of("VO"));
    assertThatThrownBy(() -> service.requireSubmissionPermission(currentCase, user))
        .isInstanceOf(ResponseStatusException.class);
    user.setFunctions(null);
    currentCase.setAvailableFunctions(null);
    assertThatThrownBy(() -> service.requireSubmissionPermission(currentCase, user))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(client);
  }

  @Test
  void dischargeRequiresCasePermissionUserPermissionAndEveryOutcome() {
    currentCase.setProceedings(List.of(new ProceedingDetail().proceedingCaseId("p")));
    assertThat(service.canDischarge(currentCase, user, draft)).isFalse();
    draft.setProceedingOutcomes(List.of(completeOutcome()));
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
    user.setFunctions(List.of("SUBOUT"));
    assertThat(service.canDischarge(currentCase, user, draft)).isFalse();
    user.setFunctions(List.of("POD"));
    currentCase.setAvailableFunctions(List.of("PO"));
    assertThat(service.canDischarge(currentCase, user, draft)).isFalse();
  }

  @Test
  void clearedLocalOutcomeAllowsDischargeWithoutRestoringEbsOutcome() {
    currentCase.setProceedings(
        List.of(new ProceedingDetail().proceedingCaseId("p").outcome(completeOutcome())));
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
    draft.setProceedingOutcomes(List.of(new ProceedingOutcomeDetail().proceedingCaseId("p")));
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void clearedLocalOutcomeCountsButEveryProceedingStillRequiresAnOutcome() {
    currentCase.setProceedings(
        List.of(
            new ProceedingDetail().proceedingCaseId("p"),
            new ProceedingDetail().proceedingCaseId("other")));
    draft.setProceedingOutcomes(List.of(new ProceedingOutcomeDetail().proceedingCaseId("p")));
    assertThat(service.canDischarge(currentCase, user, draft)).isFalse();
    draft.setProceedingOutcomes(
        List.of(
            new ProceedingOutcomeDetail().proceedingCaseId("p"),
            new ProceedingOutcomeDetail().proceedingCaseId("other")));
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
  }

  @Test
  void noProceedingsCountsAsCompleteAsInLegacy() {
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
  }

  @Test
  void legacyLocalOutcomePresenceCountsButPartialEbsOutcomeDoesNot() {
    currentCase.setProceedings(
        List.of(
            new ProceedingDetail()
                .proceedingCaseId("p")
                .outcome(new ProceedingOutcomeDetail().dateOfFinalWork(new Date(1000)))));
    assertThat(service.canDischarge(currentCase, user, draft)).isFalse();
    draft.setProceedingOutcomes(
        List.of(
            new ProceedingOutcomeDetail().proceedingCaseId("p").dateOfFinalWork(new Date(1000))));
    assertThat(service.canDischarge(currentCase, user, draft)).isTrue();
  }

  @Test
  void rejectsAwardsWithoutPartiesAndUnansweredInterimAward() {
    draft.setCostAwards(List.of(new CostAwardDetail()));
    draft.setFinancialAwards(List.of(new FinancialAwardDetail()));
    assertThat(service.awardErrors(draft))
        .containsExactly(
            "case.outcomeSubmission.costParties",
            "case.outcomeSubmission.financialParties",
            "case.outcomeSubmission.interimAward");
  }

  @Test
  void dischargeAndTechnicalMetadataDoNotCountAsOutcomeChanges() {
    currentCase.setCaseOutcome(
        new CaseOutcomeDetail().id(1).providerId("other").dischargeReason("CD"));
    draft.setDischargeCaseInd(true);
    draft.setClientContinueInd(false);
    draft.setOtherDetails("Discharge only");
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    verifyNoInteractions(client);
  }

  @Test
  void comparesMoneyByValueAndNotScale() {
    currentCase.setCaseOutcome(new CaseOutcomeDetail().legalCosts(new BigDecimal("12.0")));
    draft.legalCosts(new BigDecimal("12.00"));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    draft.legalCosts(new BigDecimal("12.01"));
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void comparesPreCertificateCostsByValueAndNotScale() {
    currentCase.setCaseOutcome(new CaseOutcomeDetail().preCertificateCosts(new BigDecimal("12.0")));
    draft.preCertificateCosts(new BigDecimal("12.00"));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    draft.preCertificateCosts(new BigDecimal("12.01"));
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void omittedAndExplicitlyNullCostsAreEquivalentToZero() {
    currentCase.setCaseOutcome(new CaseOutcomeDetail().preCertificateCosts(null).legalCosts(null));
    draft.setPreCertificateCosts(JsonNullable.undefined());
    draft.setLegalCosts(JsonNullable.undefined());
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    draft.setPreCertificateCosts(null);
    draft.setLegalCosts(null);
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
  }

  @Test
  void comparesCalendarDatesAsLegacyDoes() {
    final Date morning = new java.util.GregorianCalendar(2026, 0, 15, 9, 0).getTime();
    final Date evening = new java.util.GregorianCalendar(2026, 0, 15, 17, 0).getTime();
    currentCase.setProceedings(
        List.of(
            new ProceedingDetail()
                .proceedingCaseId("p")
                .outcome(
                    new ProceedingOutcomeDetail().proceedingCaseId("p").dateOfFinalWork(morning))));
    draft.setProceedingOutcomes(
        List.of(new ProceedingOutcomeDetail().proceedingCaseId("p").dateOfFinalWork(evening)));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
  }

  @Test
  void nullTopLevelCostsAreEquivalentToZeroInEitherDirection() {
    currentCase.setCaseOutcome(new CaseOutcomeDetail().legalCosts(null).preCertificateCosts(null));
    draft.legalCosts(new BigDecimal("0.00"));
    draft.preCertificateCosts(BigDecimal.ZERO);
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    currentCase.setCaseOutcome(draft);
    assertThat(
            service.hasChanges(
                currentCase, new CaseOutcomeDetail().legalCosts(null).preCertificateCosts(null)))
        .isFalse();
    draft.legalCosts(new BigDecimal("0.01"));
    assertThat(
            service.hasChanges(
                currentCase, new CaseOutcomeDetail().legalCosts(null).preCertificateCosts(null)))
        .isTrue();
    draft.setLegalCosts(null);
    draft.preCertificateCosts(new BigDecimal("0.01"));
    assertThat(
            service.hasChanges(
                currentCase, new CaseOutcomeDetail().legalCosts(null).preCertificateCosts(null)))
        .isTrue();
  }

  @Test
  void proceedingIdsLabelsAndOrderingDoNotCountButResultChangesDo() {
    currentCase.setProceedings(
        List.of(
            new ProceedingDetail()
                .proceedingCaseId("p")
                .outcome(
                    completeOutcome()
                        .id(1)
                        .result(
                            new StringDisplayValue().id("SUCCESS").displayValue("Old label")))));
    draft.setProceedingOutcomes(
        List.of(
            completeOutcome()
                .id(99)
                .result(new StringDisplayValue().id("SUCCESS").displayValue("New label"))));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    draft.getProceedingOutcomes().getFirst().getResult().setId("OTHER");
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
  void detectsEachAwardTypeWithoutCountingTechnicalIds(String type) {
    final BaseAwardDetail original = award(type);
    final BaseAwardDetail local = award(type);
    original.setId(1);
    local.setId(999);
    final var baseline = new CaseOutcomeDetail();
    setAwards(baseline, original);
    setAwards(draft, local);
    currentCase.setCaseOutcome(baseline);
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    local.setDescription("Changed");
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void detectsLiablePartyEditsUsingEbsIdentityRatherThanLocalIds() {
    final var original =
        new FinancialAwardDetail()
            .awardType("DAMAGE")
            .ebsId("award")
            .liableParties(List.of(new LiablePartyDetail().opponentId(100)));
    final var local =
        new FinancialAwardDetail()
            .id(4)
            .awardType("DAMAGE")
            .ebsId("award")
            .liableParties(List.of(new LiablePartyDetail().opponentId(200)));
    currentCase.setCaseOutcome(new CaseOutcomeDetail().financialAwards(List.of(original)));
    draft.setFinancialAwards(List.of(local));
    currentCase.setOpponents(List.of(new OpponentDetail().ebsId("100").surname("Original")));
    when(client.getAwardLiableParties(42, "DAMAGE", 4))
        .thenReturn(
            Mono.just(List.of(new OpponentDetail().id(200).ebsId("100").surname("Original"))));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    when(client.getAwardLiableParties(42, "DAMAGE", 4))
        .thenReturn(
            Mono.just(List.of(new OpponentDetail().id(200).ebsId("100").surname("Edited"))));
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void dischargePreservesFalseAndSendsNoChildCollections() {
    final var form = new CaseDischargeFormData();
    form.setDischargeCaseInd(true);
    form.setDischargeReason("CD");
    form.setClientContinueInd(false);
    form.setOtherDetails("Details");
    when(client.updateCaseOutcomeDischarge(eq(42), eq("user"), any())).thenReturn(Mono.empty());
    service.saveDischarge(draft, form, "user");
    final var captor = ArgumentCaptor.forClass(CaseOutcomeDischargeRequest.class);
    verify(client).updateCaseOutcomeDischarge(eq(42), eq("user"), captor.capture());
    assertThat(captor.getValue().getClientContinueInd().get()).isFalse();
    assertThat(captor.getValue().getDischargeCaseInd().get()).isTrue();
    assertThat(captor.getValue().getProviderId()).isEqualTo("1");
    assertThat(captor.getValue().getCaseReferenceNumber()).isEqualTo("case");
  }

  @Test
  void recoveryAndTimeRecoveryAreIncludedInBusinessComparison() {
    final var original =
        new FinancialAwardDetail()
            .awardType("DAMAGE")
            .ebsId("award")
            .recovery(new RecoveryDetail().solicitorRecoveryAmount(new BigDecimal("10.00")));
    final var local =
        new FinancialAwardDetail()
            .awardType("DAMAGE")
            .ebsId("award")
            .recovery(new RecoveryDetail().solicitorRecoveryAmount(new BigDecimal("10.0")));
    currentCase.setCaseOutcome(new CaseOutcomeDetail().financialAwards(List.of(original)));
    draft.setFinancialAwards(List.of(local));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    local.getRecovery().setSolicitorRecoveryAmount(new BigDecimal("11"));
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
    currentCase.setCaseOutcome(
        new CaseOutcomeDetail()
            .landAwards(
                List.of(
                    new LandAwardDetail()
                        .awardType("LAND")
                        .ebsId("land")
                        .timeRecovery(new TimeRecoveryDetail().triggeringEvent("Event")))));
    draft.setFinancialAwards(List.of());
    draft.setLandAwards(
        List.of(
            new LandAwardDetail()
                .awardType("LAND")
                .ebsId("land")
                .timeRecovery(new TimeRecoveryDetail().triggeringEvent("Event"))));
    assertThat(service.hasChanges(currentCase, draft)).isFalse();
    draft.getLandAwards().getFirst().getTimeRecovery().setTriggeringEvent("New event");
    assertThat(service.hasChanges(currentCase, draft)).isTrue();
  }

  @Test
  void clearingDischargeSetsAllFourFieldsToNullWithoutReplacingOutcome() {
    draft.setDischargeCaseInd(false);
    when(client.updateCaseOutcomeDischarge(eq(42), eq("user"), any())).thenReturn(Mono.empty());
    service.clearDischarge(draft, "user");
    final var captor = ArgumentCaptor.forClass(CaseOutcomeDischargeRequest.class);
    verify(client).updateCaseOutcomeDischarge(eq(42), eq("user"), captor.capture());
    assertThat(captor.getValue().getDischargeCaseInd()).isEqualTo(JsonNullable.of(null));
    assertThat(captor.getValue().getClientContinueInd()).isEqualTo(JsonNullable.of(null));
    assertThat(captor.getValue().getDischargeReason()).isEqualTo(JsonNullable.of(null));
    assertThat(captor.getValue().getOtherDetails()).isEqualTo(JsonNullable.of(null));
    verifyNoMoreInteractions(client);
  }

  private ProceedingOutcomeDetail completeOutcome() {
    return new ProceedingOutcomeDetail()
        .proceedingCaseId("p")
        .dateOfFinalWork(new Date(1000))
        .stageEnd(new StringDisplayValue().id("END"))
        .result(new StringDisplayValue().id("SUCCESS"))
        .resolutionMethod("COURT")
        .alternativeResolution("N")
        .widerBenefits("N");
  }

  private BaseAwardDetail award(String type) {
    return switch (type) {
      case "COST" -> new CostAwardDetail().awardType(type).ebsId("award");
      case "DAMAGE" -> new FinancialAwardDetail().awardType(type).ebsId("award");
      case "LAND" -> new LandAwardDetail().awardType(type).ebsId("award");
      default -> new OtherAssetAwardDetail().awardType(type).ebsId("award");
    };
  }

  private void setAwards(CaseOutcomeDetail outcome, BaseAwardDetail award) {
    if (award instanceof CostAwardDetail cost) {
      outcome.setCostAwards(List.of(cost));
    } else if (award instanceof FinancialAwardDetail financial) {
      outcome.setFinancialAwards(List.of(financial));
    } else if (award instanceof LandAwardDetail land) {
      outcome.setLandAwards(List.of(land));
    } else if (award instanceof OtherAssetAwardDetail asset) {
      outcome.setOtherAssetAwards(List.of(asset));
    }
  }
}
