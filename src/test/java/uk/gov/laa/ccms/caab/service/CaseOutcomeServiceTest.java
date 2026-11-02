package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
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

@ExtendWith(MockitoExtension.class)
class CaseOutcomeServiceTest {

  @Mock private CaabApiClient caabApiClient;
  @Mock private CostAwardMapper costAwardMapper;

  @Spy @InjectMocks private CaseOutcomeService caseOutcomeService;

  @ParameterizedTest
  @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
  void deleteAwardDeletesOnlySelectedAwardWithoutReplacingOutcome(String type) {
    doReturn(Optional.of(deletionOutcome()))
        .when(caseOutcomeService)
        .getCaseOutcome("caseRef", 123);
    when(caabApiClient.deleteAward(42, type, 7, "user1")).thenReturn(Mono.empty());

    caseOutcomeService.deleteAward("caseRef", 123, type, 7, "user1");

    verify(caabApiClient).deleteAward(42, type, 7, "user1");
    verifyNoMoreInteractions(caabApiClient);
  }

  @ParameterizedTest
  @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET", "INVALID"})
  void missingAwardOrUnsupportedTypeDoesNotDelete(String type) {
    doReturn(Optional.of(new CaseOutcomeDetail().id(42)))
        .when(caseOutcomeService)
        .getCaseOutcome("caseRef", 123);

    final Class<? extends RuntimeException> expectedException =
        "INVALID".equals(type) ? IllegalArgumentException.class : IllegalStateException.class;
    assertThrows(
        expectedException, () -> caseOutcomeService.deleteAward("caseRef", 123, type, 7, "user1"));

    verifyNoMoreInteractions(caabApiClient);
  }

  @Test
  void wrongAwardIdDoesNotDelete() {
    doReturn(Optional.of(deletionOutcome()))
        .when(caseOutcomeService)
        .getCaseOutcome("caseRef", 123);

    assertThrows(
        IllegalStateException.class,
        () -> caseOutcomeService.deleteAward("caseRef", 123, "COST", 99, "user1"));
    verifyNoMoreInteractions(caabApiClient);
  }

  @Test
  void missingProviderCaseOutcomeDoesNotDeleteOrBootstrap() {
    doReturn(Optional.empty()).when(caseOutcomeService).getCaseOutcome("caseRef", 123);

    assertThrows(
        IllegalStateException.class,
        () -> caseOutcomeService.deleteAward("caseRef", 123, "COST", 7, "user1"));
    verifyNoMoreInteractions(caabApiClient);
  }

  @Test
  void deleteAwardPropagatesApiFailure() {
    doReturn(Optional.of(deletionOutcome()))
        .when(caseOutcomeService)
        .getCaseOutcome("caseRef", 123);
    when(caabApiClient.deleteAward(42, "COST", 7, "user1"))
        .thenReturn(Mono.error(new IllegalStateException("Deletion failed")));

    assertThrows(
        IllegalStateException.class,
        () -> caseOutcomeService.deleteAward("caseRef", 123, "COST", 7, "user1"));
  }

  private CaseOutcomeDetail deletionOutcome() {
    return new CaseOutcomeDetail()
        .id(42)
        .costAwards(List.of(new CostAwardDetail().id(7)))
        .financialAwards(List.of(new FinancialAwardDetail().id(7)))
        .landAwards(List.of(new LandAwardDetail().id(7)))
        .otherAssetAwards(List.of(new OtherAssetAwardDetail().id(7)));
  }

  @Test
  void getFinancialAward_existingAward_returnsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer financialAwardId = 7;

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    final FinancialAwardDetail financialAward = new FinancialAwardDetail().id(financialAwardId);
    when(caabApiClient.getFinancialAward(caseOutcomeId, financialAwardId))
        .thenReturn(Mono.just(financialAward));

    final Optional<FinancialAwardDetail> result =
        caseOutcomeService.getFinancialAward(caseReferenceNumber, providerId, financialAwardId);

    assertEquals(financialAward, result.orElseThrow());
    verify(caabApiClient).getFinancialAward(caseOutcomeId, financialAwardId);
  }

  @Test
  void createFinancialAward_existingCaseOutcome_createsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final String loginId = "user1";
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final CaseOutcomeDetail bootstrapCaseOutcome = new CaseOutcomeDetail().id(88);

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createFinancialAward(caseOutcomeId, loginId, request))
        .thenReturn(Mono.just("7"));

    assertEquals(
        7,
        caseOutcomeService.createFinancialAward(
            caseReferenceNumber, providerId, request, loginId, bootstrapCaseOutcome));

    verify(caabApiClient).createFinancialAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createFinancialAward_whenNoIdReturned_throwsRatherThanOpeningInvalidPartySummary() {
    final FinancialAwardRequest request = new FinancialAwardRequest();
    doReturn(Optional.of(new CaseOutcomeDetail().id(42)))
        .when(caseOutcomeService)
        .getCaseOutcome("300000001", 123);
    when(caabApiClient.createFinancialAward(42, "user1", request)).thenReturn(Mono.empty());

    final IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                caseOutcomeService.createFinancialAward("300000001", 123, request, "user1", null));
    assertEquals("No id returned for the newly created financial award", exception.getMessage());
  }

  @Test
  void createFinancialAward_whenNoCaseOutcome_createsBootstrapCaseOutcomeFirst() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final String loginId = "user1";
    final CaseOutcomeDetail createdCaseOutcome = new CaseOutcomeDetail().id(caseOutcomeId);
    final FinancialAwardDetail bootstrapAward =
        new FinancialAwardDetail().id(9).awardType("DAMAGE");
    final ProceedingOutcomeDetail bootstrapProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1");
    final CaseOutcomeDetail bootstrapCaseOutcome =
        new CaseOutcomeDetail()
            .id(88)
            .financialAwards(List.of(bootstrapAward))
            .proceedingOutcomes(List.of(bootstrapProceedingOutcome));

    doReturn(Optional.empty(), Optional.of(createdCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just(String.valueOf(caseOutcomeId)));
    when(caabApiClient.createFinancialAward(caseOutcomeId, loginId, request))
        .thenReturn(Mono.just("7"));

    assertEquals(
        7,
        caseOutcomeService.createFinancialAward(
            caseReferenceNumber, providerId, request, loginId, bootstrapCaseOutcome));

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    verify(caabApiClient).createFinancialAward(caseOutcomeId, loginId, request);
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(caseReferenceNumber, caseOutcomeCaptor.getValue().getCaseReferenceNumber());
    assertEquals(String.valueOf(providerId), caseOutcomeCaptor.getValue().getProviderId());
    assertEquals(1, caseOutcomeCaptor.getValue().getFinancialAwards().size());
    assertEquals("DAMAGE", caseOutcomeCaptor.getValue().getFinancialAwards().get(0).getAwardType());
    assertNull(caseOutcomeCaptor.getValue().getProceedingOutcomes());
  }

  @Test
  void getOrCreateCaseOutcome_whenBootstrapHasProceedingOutcome_clearsPersistedIds() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final ProceedingOutcomeDetail bootstrapOutcome =
        new ProceedingOutcomeDetail()
            .id(99)
            .proceedingCaseId("pc1")
            .result(
                new uk.gov.laa.ccms.caab.model.StringDisplayValue().id("R1").displayValue("EBS"));
    final CaseOutcomeDetail bootstrapCaseOutcome =
        new CaseOutcomeDetail().id(42).proceedingOutcomes(List.of(bootstrapOutcome));

    doReturn(Optional.empty(), Optional.of(new CaseOutcomeDetail().id(77)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("77"));

    caseOutcomeService.getOrCreateCaseOutcome(
        caseReferenceNumber, providerId, loginId, bootstrapCaseOutcome);

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(1, caseOutcomeCaptor.getValue().getProceedingOutcomes().size());
    assertNull(caseOutcomeCaptor.getValue().getProceedingOutcomes().get(0).getId());
    assertEquals(
        "pc1", caseOutcomeCaptor.getValue().getProceedingOutcomes().get(0).getProceedingCaseId());
    assertEquals(99, bootstrapOutcome.getId());
  }

  @Test
  void getCostAward_existingAward_returnsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer costAwardId = 7;

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    final CostAwardDetail costAward = new CostAwardDetail().id(costAwardId);
    when(caabApiClient.getCostAward(caseOutcomeId, costAwardId)).thenReturn(Mono.just(costAward));

    final Optional<CostAwardDetail> result =
        caseOutcomeService.getCostAward(caseReferenceNumber, providerId, costAwardId);

    assertEquals(costAward, result.orElseThrow());
    verify(caabApiClient).getCostAward(caseOutcomeId, costAwardId);
  }

  @Test
  void createCostAward_existingCaseOutcome_createsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final String loginId = "user1";
    final CostAwardDetail request = new CostAwardDetail();
    final CaseOutcomeDetail bootstrapCaseOutcome = new CaseOutcomeDetail().id(88);

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCostAward(caseOutcomeId, loginId, request)).thenReturn(Mono.just("7"));

    assertEquals(
        7,
        caseOutcomeService.createCostAward(
            caseReferenceNumber, providerId, request, loginId, bootstrapCaseOutcome));

    verify(caabApiClient).createCostAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createCostAward_whenNoIdReturned_throws() {
    final CostAwardDetail request = new CostAwardDetail();
    doReturn(Optional.of(new CaseOutcomeDetail().id(42)))
        .when(caseOutcomeService)
        .getCaseOutcome("300000001", 123);
    when(caabApiClient.createCostAward(42, "user1", request)).thenReturn(Mono.empty());

    final IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> caseOutcomeService.createCostAward("300000001", 123, request, "user1", null));
    assertEquals("No id returned for the newly created cost award", exception.getMessage());
  }

  @Test
  void createCostAward_whenNoCaseOutcome_createsBootstrapCaseOutcomeFirst() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final CostAwardDetail request = new CostAwardDetail();
    final String loginId = "user1";
    final CaseOutcomeDetail createdCaseOutcome = new CaseOutcomeDetail().id(caseOutcomeId);
    final CostAwardDetail bootstrapAward = new CostAwardDetail().id(9).awardType("COST");
    final ProceedingOutcomeDetail bootstrapProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1");
    final CaseOutcomeDetail bootstrapCaseOutcome =
        new CaseOutcomeDetail()
            .id(88)
            .costAwards(List.of(bootstrapAward))
            .proceedingOutcomes(List.of(bootstrapProceedingOutcome));

    doReturn(Optional.empty(), Optional.of(createdCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just(String.valueOf(caseOutcomeId)));
    when(caabApiClient.createCostAward(caseOutcomeId, loginId, request)).thenReturn(Mono.just("7"));

    assertEquals(
        7,
        caseOutcomeService.createCostAward(
            caseReferenceNumber, providerId, request, loginId, bootstrapCaseOutcome));

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    verify(caabApiClient).createCostAward(caseOutcomeId, loginId, request);
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(caseReferenceNumber, caseOutcomeCaptor.getValue().getCaseReferenceNumber());
    assertEquals(String.valueOf(providerId), caseOutcomeCaptor.getValue().getProviderId());
    assertEquals(1, caseOutcomeCaptor.getValue().getCostAwards().size());
    assertEquals("COST", caseOutcomeCaptor.getValue().getCostAwards().get(0).getAwardType());
    assertNull(caseOutcomeCaptor.getValue().getProceedingOutcomes());
  }

  @Test
  void updateFinancialAward_existingUpdateableAward_updatesAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer financialAwardId = 7;
    final String loginId = "user1";
    final FinancialAwardRequest request = new FinancialAwardRequest();

    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(caseOutcomeId)
            .financialAwards(
                List.of(new FinancialAwardDetail().id(financialAwardId).updateAllowed(true)));

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.updateFinancialAward(caseOutcomeId, financialAwardId, loginId, request))
        .thenReturn(Mono.empty());

    caseOutcomeService.updateFinancialAward(
        caseReferenceNumber, providerId, financialAwardId, request, loginId);

    verify(caabApiClient).updateFinancialAward(caseOutcomeId, financialAwardId, loginId, request);
  }

  @Test
  void updateCostAward_existingUpdateableAward_updatesAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer costAwardId = 7;
    final String loginId = "user1";
    final CostAwardFormData request = new CostAwardFormData();
    request.setId(99);
    request.setAwardType("TAMPERED");
    request.setAwardCode("WRONG");
    request.setDescription("Changed");
    request.setCourtAssessmentStatus("A");
    final CostAwardDetail existingCostAward =
        new CostAwardDetail()
            .id(costAwardId)
            .awardType("COST")
            .awardCode("COST_AGR")
            .description("Cost")
            .ebsId("ebs-1");

    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(caseOutcomeId)
            .costAwards(List.of(new CostAwardDetail().id(costAwardId).updateAllowed(true)));

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.getCostAward(caseOutcomeId, costAwardId))
        .thenReturn(Mono.just(existingCostAward));
    when(caabApiClient.updateCostAward(caseOutcomeId, costAwardId, loginId, existingCostAward))
        .thenReturn(Mono.empty());

    caseOutcomeService.updateCostAward(
        caseReferenceNumber, providerId, costAwardId, request, loginId);

    verify(costAwardMapper).updateCostAward(request, existingCostAward);
    verify(caabApiClient).getCostAward(caseOutcomeId, costAwardId);
    verify(caabApiClient).updateCostAward(caseOutcomeId, costAwardId, loginId, existingCostAward);
  }

  @Test
  void getOtherAssetAward_existingAward_returnsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer otherAssetAwardId = 7;

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    final OtherAssetAwardDetail otherAssetAward = new OtherAssetAwardDetail().id(otherAssetAwardId);
    when(caabApiClient.getOtherAssetAward(caseOutcomeId, otherAssetAwardId))
        .thenReturn(Mono.just(otherAssetAward));

    final Optional<OtherAssetAwardDetail> result =
        caseOutcomeService.getOtherAssetAward(caseReferenceNumber, providerId, otherAssetAwardId);

    assertEquals(otherAssetAward, result.orElseThrow());
    verify(caabApiClient).getOtherAssetAward(caseOutcomeId, otherAssetAwardId);
  }

  @Test
  void createOtherAssetAward_existingCaseOutcome_createsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final String loginId = "user1";
    final OtherAssetAwardRequest request = new OtherAssetAwardRequest();

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createOtherAssetAward(caseOutcomeId, loginId, request))
        .thenReturn(Mono.just("7"));

    assertEquals(
        7,
        caseOutcomeService.createOtherAssetAward(
            caseReferenceNumber, providerId, request, loginId));

    verify(caabApiClient).createOtherAssetAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createOtherAssetAward_whenNoCaseOutcome_throwsWithoutCreatingAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final OtherAssetAwardRequest request = new OtherAssetAwardRequest();

    doReturn(Optional.empty())
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    assertThrows(
        IllegalStateException.class,
        () ->
            caseOutcomeService.createOtherAssetAward(
                caseReferenceNumber, providerId, request, "user1"));

    verify(caabApiClient, never()).createOtherAssetAward(any(), any(), any());
  }

  @Test
  void createOtherAssetAward_withoutReturnedIdFailsExplicitly() {
    doReturn(Optional.of(new CaseOutcomeDetail().id(42)))
        .when(caseOutcomeService)
        .getCaseOutcome("300000001", 123);
    when(caabApiClient.createOtherAssetAward(eq(42), eq("user1"), any())).thenReturn(Mono.empty());

    assertThrows(
        IllegalStateException.class,
        () ->
            caseOutcomeService.createOtherAssetAward(
                "300000001", 123, new OtherAssetAwardRequest(), "user1"));
  }

  @Test
  void updateOtherAssetAward_existingAward_updatesAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer otherAssetAwardId = 7;
    final String loginId = "user1";
    final OtherAssetAwardRequest request = new OtherAssetAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(caseOutcomeId)
            .otherAssetAwards(List.of(new OtherAssetAwardDetail().id(otherAssetAwardId)));

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.updateOtherAssetAward(caseOutcomeId, otherAssetAwardId, loginId, request))
        .thenReturn(Mono.empty());

    caseOutcomeService.updateOtherAssetAward(
        caseReferenceNumber, providerId, otherAssetAwardId, request, loginId);

    verify(caabApiClient).updateOtherAssetAward(caseOutcomeId, otherAssetAwardId, loginId, request);
  }

  @Test
  void updateOtherAssetAward_whenAwardDoesNotBelongToOutcome_throwsWithoutUpdating() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer otherAssetAwardId = 7;
    final OtherAssetAwardRequest request = new OtherAssetAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail().id(caseOutcomeId).otherAssetAwards(List.of());

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    final IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                caseOutcomeService.updateOtherAssetAward(
                    caseReferenceNumber, providerId, otherAssetAwardId, request, "user1"));

    assertEquals(
        "Other asset award 7 does not belong to case reference number: 300000001",
        exception.getMessage());
    verify(caabApiClient, never()).updateOtherAssetAward(any(), any(), any(), any());
  }

  @Test
  void getLandAward_existingCaseOutcome_returnsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer landAwardId = 7;
    final LandAwardDetail landAward = new LandAwardDetail().id(landAwardId);

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.getLandAward(caseOutcomeId, landAwardId)).thenReturn(Mono.just(landAward));

    final Optional<LandAwardDetail> result =
        caseOutcomeService.getLandAward(caseReferenceNumber, providerId, landAwardId);

    assertEquals(landAward, result.orElseThrow());
    verify(caabApiClient).getLandAward(caseOutcomeId, landAwardId);
  }

  @Test
  void getLandAward_whenNoCaseOutcome_throwsWithoutGettingAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;

    doReturn(Optional.empty())
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    assertThrows(
        IllegalStateException.class,
        () -> caseOutcomeService.getLandAward(caseReferenceNumber, providerId, 7));

    verify(caabApiClient, never()).getLandAward(any(), any());
  }

  @Test
  void createLandAward_existingCaseOutcome_createsAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final String loginId = "user1";
    final LandAwardRequest request = new LandAwardRequest();

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createLandAward(caseOutcomeId, loginId, request)).thenReturn(Mono.just("7"));

    assertEquals(
        7, caseOutcomeService.createLandAward(caseReferenceNumber, providerId, request, loginId));

    verify(caabApiClient).createLandAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createLandAward_whenNoCaseOutcome_throwsWithoutCreatingAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final LandAwardRequest request = new LandAwardRequest();

    doReturn(Optional.empty())
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    assertThrows(
        IllegalStateException.class,
        () ->
            caseOutcomeService.createLandAward(caseReferenceNumber, providerId, request, "user1"));

    verify(caabApiClient, never()).createLandAward(any(), any(), any());
  }

  @Test
  void updateLandAward_existingOwnedAward_updatesAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final Integer landAwardId = 7;
    final String loginId = "user1";
    final LandAwardRequest request = new LandAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(caseOutcomeId)
            .landAwards(List.of(new LandAwardDetail().id(landAwardId)));

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.updateLandAward(caseOutcomeId, landAwardId, loginId, request))
        .thenReturn(Mono.empty());

    caseOutcomeService.updateLandAward(
        caseReferenceNumber, providerId, landAwardId, request, loginId);

    verify(caabApiClient).updateLandAward(caseOutcomeId, landAwardId, loginId, request);
  }

  @Test
  void updateLandAward_unownedAward_throwsWithoutUpdatingAward() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer landAwardId = 7;
    final LandAwardRequest request = new LandAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail().id(42).landAwards(List.of(new LandAwardDetail().id(8)));

    doReturn(Optional.of(caseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    assertThrows(
        IllegalStateException.class,
        () ->
            caseOutcomeService.updateLandAward(
                caseReferenceNumber, providerId, landAwardId, request, "user1"));

    verify(caabApiClient, never()).updateLandAward(any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"LAND", "ASSET"})
  void upsertTimeRecovery_usesCaseOutcomeIdAndScopedAwardPath(String awardType) {
    final TimeRecoveryRequest request = new TimeRecoveryRequest("Sale", "When sold");
    doReturn(Optional.of(new CaseOutcomeDetail().id(42)))
        .when(caseOutcomeService)
        .getCaseOutcome("300000001", 123);
    when(caabApiClient.upsertTimeRecovery(42, awardType, 7, "user1", request))
        .thenReturn(Mono.empty());

    caseOutcomeService.upsertTimeRecovery("300000001", 123, awardType, 7, request, "user1");

    verify(caabApiClient).upsertTimeRecovery(42, awardType, 7, "user1", request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"LAND", "ASSET"})
  void upsertTimeRecovery_withoutCaseOutcomeDoesNotCallApi(String awardType) {
    doReturn(Optional.empty()).when(caseOutcomeService).getCaseOutcome("300000001", 123);

    assertThrows(
        IllegalStateException.class,
        () ->
            caseOutcomeService.upsertTimeRecovery(
                "300000001",
                123,
                awardType,
                7,
                new TimeRecoveryRequest("Sale", "When sold"),
                "user1"));
    verify(caabApiClient, never()).upsertTimeRecovery(any(), any(), any(), any(), any());
  }

  @Test
  void updateProceedingOutcome_existingOutcome_deletesOldThenCreatesNewRecord() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final Integer existingCaseOutcomeId = 42;

    final ProceedingOutcomeDetail existingProceedingOutcome =
        new ProceedingOutcomeDetail().id(10).proceedingCaseId("pc1").resultInfo("old");
    final ProceedingOutcomeDetail untouchedProceedingOutcome =
        new ProceedingOutcomeDetail().id(11).proceedingCaseId("pc2").resultInfo("keep");
    final ProceedingOutcomeDetail replacementProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("new");
    final CostAwardDetail existingCostAward = new CostAwardDetail().id(55).awardType("COST");

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(existingCaseOutcomeId);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(List.of(existingProceedingOutcome, untouchedProceedingOutcome)));
    existingCaseOutcome.setCostAwards(new ArrayList<>(List.of(existingCostAward)));

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));
    when(caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId)).thenReturn(Mono.empty());

    caseOutcomeService.updateProceedingOutcome(
        caseReferenceNumber, providerId, replacementProceedingOutcome, loginId, null);

    final ArgumentCaptor<CaseOutcomeDetail> createdCaseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), createdCaseOutcomeCaptor.capture());
    verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);
    verify(caabApiClient, never()).deleteCaseOutcomes(any(), any(), any());

    final CaseOutcomeDetail createdCaseOutcome = createdCaseOutcomeCaptor.getValue();
    assertEquals(2, createdCaseOutcome.getProceedingOutcomes().size());
    assertEquals(
        List.of("pc2", "pc1"),
        createdCaseOutcome.getProceedingOutcomes().stream()
            .map(ProceedingOutcomeDetail::getProceedingCaseId)
            .toList());
    assertEquals("new", createdCaseOutcome.getProceedingOutcomes().get(1).getResultInfo());
    assertNull(createdCaseOutcome.getId());
    assertNull(createdCaseOutcome.getProceedingOutcomes().get(0).getId());
    assertNull(createdCaseOutcome.getProceedingOutcomes().get(1).getId());
    assertNull(createdCaseOutcome.getCostAwards().get(0).getId());
    assertEquals(11, untouchedProceedingOutcome.getId());
    assertEquals(55, existingCostAward.getId());

    final InOrder inOrder = inOrder(caabApiClient);
    inOrder.verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);
    inOrder.verify(caabApiClient).createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class));
  }

  @Test
  void getOrCreateCaseOutcome_whenNoPersistedOutcome_bootstrapsFromSuppliedOutcome() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final CostAwardDetail costAward = new CostAwardDetail().id(7).awardType("COST");
    final ProceedingOutcomeDetail proceedingOutcome =
        new ProceedingOutcomeDetail().id(12).proceedingCaseId("pc1").resultInfo("saved");
    final CaseOutcomeDetail bootstrapCaseOutcome =
        new CaseOutcomeDetail()
            .id(99)
            .costAwards(List.of(costAward))
            .proceedingOutcomes(List.of(proceedingOutcome));
    final CaseOutcomeDetail createdCaseOutcome = new CaseOutcomeDetail().id(42);

    doReturn(Optional.empty(), Optional.of(createdCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("42"));

    final CaseOutcomeDetail result =
        caseOutcomeService.getOrCreateCaseOutcome(
            caseReferenceNumber, providerId, loginId, bootstrapCaseOutcome);

    assertEquals(createdCaseOutcome, result);

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(caseReferenceNumber, caseOutcomeCaptor.getValue().getCaseReferenceNumber());
    assertEquals(String.valueOf(providerId), caseOutcomeCaptor.getValue().getProviderId());
    assertEquals(1, caseOutcomeCaptor.getValue().getCostAwards().size());
    assertEquals("COST", caseOutcomeCaptor.getValue().getCostAwards().get(0).getAwardType());
    assertNull(caseOutcomeCaptor.getValue().getCostAwards().get(0).getId());
    assertEquals(1, caseOutcomeCaptor.getValue().getProceedingOutcomes().size());
    assertEquals(
        "saved", caseOutcomeCaptor.getValue().getProceedingOutcomes().get(0).getResultInfo());
    assertNull(caseOutcomeCaptor.getValue().getProceedingOutcomes().get(0).getId());
    assertEquals(7, costAward.getId());
    assertEquals(12, proceedingOutcome.getId());
  }

  @Test
  void getOrCreateCaseOutcome_whenCreateConflicts_reloadsExistingOutcome() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final CaseOutcomeDetail reloadedCaseOutcome = new CaseOutcomeDetail().id(42);

    doReturn(Optional.empty(), Optional.of(reloadedCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.error(new CaabApiClientException("Conflict", HttpStatus.CONFLICT)));

    final CaseOutcomeDetail result =
        caseOutcomeService.getOrCreateCaseOutcome(caseReferenceNumber, providerId, loginId, null);

    assertEquals(reloadedCaseOutcome, result);
    verify(caabApiClient).createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class));
  }

  @Test
  void updateProceedingOutcome_whenCreateFails_attemptsRestoreAndThrows() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(42);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(
            List.of(new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("old"))));

    final ProceedingOutcomeDetail replacementProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("new");

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    when(caabApiClient.deleteCaseOutcome(42, loginId)).thenReturn(Mono.empty());
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(
            Mono.error(new CaabApiClientException("Transient API failure")),
            Mono.just("restored-id"));

    assertThrows(
        CaabApiClientException.class,
        () ->
            caseOutcomeService.updateProceedingOutcome(
                caseReferenceNumber, providerId, replacementProceedingOutcome, loginId, null));

    verify(caabApiClient).deleteCaseOutcome(42, loginId);
    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient, times(2)).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    verify(caabApiClient, never()).deleteCaseOutcomes(any(), any(), any());

    final List<CaseOutcomeDetail> createAttempts = caseOutcomeCaptor.getAllValues();
    assertEquals("pc1", createAttempts.get(0).getProceedingOutcomes().get(0).getProceedingCaseId());
    assertEquals("pc1", createAttempts.get(1).getProceedingOutcomes().get(0).getProceedingCaseId());
    assertEquals("new", createAttempts.get(0).getProceedingOutcomes().get(0).getResultInfo());
    assertEquals("old", createAttempts.get(1).getProceedingOutcomes().get(0).getResultInfo());
  }

  @Test
  void updateProceedingOutcome_whenNoExistingOutcome_bootstrapsFromEbsOutcome() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final ProceedingOutcomeDetail ebsReplacedOutcome =
        new ProceedingOutcomeDetail().id(10).proceedingCaseId("pc1");
    final ProceedingOutcomeDetail ebsOtherOutcome =
        new ProceedingOutcomeDetail().id(11).proceedingCaseId("pc2");
    final CostAwardDetail ebsCostAward = new CostAwardDetail().awardType("COST");
    final CaseOutcomeDetail ebsCaseOutcome =
        new CaseOutcomeDetail()
            .proceedingOutcomes(new ArrayList<>(List.of(ebsReplacedOutcome, ebsOtherOutcome)))
            .costAwards(new ArrayList<>(List.of(ebsCostAward)));
    final ProceedingOutcomeDetail proceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1");

    doReturn(Optional.empty())
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));

    caseOutcomeService.updateProceedingOutcome(
        caseReferenceNumber, providerId, proceedingOutcome, loginId, ebsCaseOutcome);

    final ArgumentCaptor<CaseOutcomeDetail> captor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), captor.capture());
    final CaseOutcomeDetail created = captor.getValue();
    assertEquals(caseReferenceNumber, created.getCaseReferenceNumber());
    assertEquals(List.of(ebsCostAward), created.getCostAwards());
    assertEquals(2, created.getProceedingOutcomes().size());
    assertEquals("pc2", created.getProceedingOutcomes().get(0).getProceedingCaseId());
    assertNull(created.getProceedingOutcomes().get(0).getId());
    assertSame(proceedingOutcome, created.getProceedingOutcomes().get(1));
    assertEquals(11, ebsOtherOutcome.getId());
    assertEquals(2, ebsCaseOutcome.getProceedingOutcomes().size());
  }

  @Test
  void updateProceedingOutcome_whenNoExistingOutcome_createsWithoutDelete() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final ProceedingOutcomeDetail proceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1");

    doReturn(Optional.empty())
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));

    caseOutcomeService.updateProceedingOutcome(
        caseReferenceNumber, providerId, proceedingOutcome, loginId, null);

    verify(caabApiClient).createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class));
    verify(caabApiClient, never()).deleteCaseOutcome(any(), any());
    verify(caabApiClient, never()).deleteCaseOutcomes(any(), any(), any());
    verifyNoMoreInteractions(caabApiClient);
  }

  @Test
  void clearProceedingOutcome_existingOutcome_deletesOldThenCreatesNewRecord() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final Integer existingCaseOutcomeId = 42;

    final ProceedingOutcomeDetail proceedingToClear =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("old");
    final ProceedingOutcomeDetail untouchedProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc2").resultInfo("keep");

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(existingCaseOutcomeId);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(List.of(proceedingToClear, untouchedProceedingOutcome)));

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId)).thenReturn(Mono.empty());
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));

    caseOutcomeService.clearProceedingOutcome(caseReferenceNumber, providerId, "pc1", loginId);

    verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);
    final ArgumentCaptor<CaseOutcomeDetail> createdCaseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), createdCaseOutcomeCaptor.capture());
    verify(caabApiClient, never()).deleteCaseOutcomes(any(), any(), any());

    final CaseOutcomeDetail createdCaseOutcome = createdCaseOutcomeCaptor.getValue();
    assertEquals(2, createdCaseOutcome.getProceedingOutcomes().size());
    assertEquals("pc2", createdCaseOutcome.getProceedingOutcomes().get(0).getProceedingCaseId());
    assertEquals("pc1", createdCaseOutcome.getProceedingOutcomes().get(1).getProceedingCaseId());
    assertNull(createdCaseOutcome.getProceedingOutcomes().get(1).getResultInfo());
    assertNull(createdCaseOutcome.getId());

    final InOrder inOrder = inOrder(caabApiClient);
    inOrder.verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);
    inOrder.verify(caabApiClient).createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class));
  }

  @Test
  void clearProceedingOutcome_lastRemainingOutcome_deletesAndRecreatesWithClearMarker() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final Integer existingCaseOutcomeId = 42;

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(existingCaseOutcomeId);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(List.of(new ProceedingOutcomeDetail().proceedingCaseId("pc1"))));

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId)).thenReturn(Mono.empty());
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));

    caseOutcomeService.clearProceedingOutcome(caseReferenceNumber, providerId, "pc1", loginId);

    final InOrder inOrder = inOrder(caabApiClient);
    inOrder.verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);

    final ArgumentCaptor<CaseOutcomeDetail> captor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    inOrder.verify(caabApiClient).createCaseOutcome(eq(loginId), captor.capture());

    assertNotNull(captor.getValue().getProceedingOutcomes());
    assertEquals(1, captor.getValue().getProceedingOutcomes().size());
    assertEquals("pc1", captor.getValue().getProceedingOutcomes().get(0).getProceedingCaseId());
    assertNull(captor.getValue().getProceedingOutcomes().get(0).getResultInfo());
    assertNull(captor.getValue().getId());
    verify(caabApiClient, never()).deleteCaseOutcomes(any(), any(), any());
  }

  @Test
  void clearProceedingOutcome_whenCreateFails_attemptsRestoreAndThrows() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(42);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(
            List.of(
                new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("remove"),
                new ProceedingOutcomeDetail().proceedingCaseId("pc2").resultInfo("keep"))));

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.deleteCaseOutcome(42, loginId)).thenReturn(Mono.empty());
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(
            Mono.error(new CaabApiClientException("Transient API failure")),
            Mono.just("restored-id"));

    assertThrows(
        CaabApiClientException.class,
        () ->
            caseOutcomeService.clearProceedingOutcome(
                caseReferenceNumber, providerId, "pc1", loginId));

    verify(caabApiClient).deleteCaseOutcome(42, loginId);
    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient, times(2)).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());

    final List<CaseOutcomeDetail> createAttempts = caseOutcomeCaptor.getAllValues();
    assertEquals(
        List.of("pc2", "pc1"),
        createAttempts.get(0).getProceedingOutcomes().stream()
            .map(ProceedingOutcomeDetail::getProceedingCaseId)
            .toList());
    assertNull(createAttempts.get(0).getProceedingOutcomes().get(1).getResultInfo());
    assertEquals(
        List.of("pc1", "pc2"),
        createAttempts.get(1).getProceedingOutcomes().stream()
            .map(ProceedingOutcomeDetail::getProceedingCaseId)
            .toList());
  }

  @Test
  void clearProceedingOutcome_whenNoExistingOutcome_noop() {
    doReturn(Optional.empty()).when(caseOutcomeService).getCaseOutcome("300000001", 123);

    caseOutcomeService.clearProceedingOutcome("300000001", 123, "pc1", "user1");

    verifyNoMoreInteractions(caabApiClient);
  }
}
