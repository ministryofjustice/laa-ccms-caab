package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;

@ExtendWith(MockitoExtension.class)
class CaseOutcomeServiceTest {

  @Mock private CaabApiClient caabApiClient;
  @Mock private CostAwardMapper costAwardMapper;

  @Spy @InjectMocks private CaseOutcomeService caseOutcomeService;

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

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createFinancialAward(caseOutcomeId, loginId, request))
        .thenReturn(Mono.just("7"));

    caseOutcomeService.createFinancialAward(caseReferenceNumber, providerId, request, loginId);

    verify(caabApiClient).createFinancialAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createFinancialAward_whenNoCaseOutcome_createsBootstrapCaseOutcomeFirst() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final String loginId = "user1";
    final CaseOutcomeDetail createdCaseOutcome = new CaseOutcomeDetail().id(caseOutcomeId);

    doReturn(Optional.empty(), Optional.of(createdCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just(String.valueOf(caseOutcomeId)));
    when(caabApiClient.createFinancialAward(caseOutcomeId, loginId, request))
        .thenReturn(Mono.just("7"));

    caseOutcomeService.createFinancialAward(caseReferenceNumber, providerId, request, loginId);

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    verify(caabApiClient).createFinancialAward(caseOutcomeId, loginId, request);
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(caseReferenceNumber, caseOutcomeCaptor.getValue().getCaseReferenceNumber());
    assertEquals(String.valueOf(providerId), caseOutcomeCaptor.getValue().getProviderId());
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

    doReturn(Optional.of(new CaseOutcomeDetail().id(caseOutcomeId)))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCostAward(caseOutcomeId, loginId, request)).thenReturn(Mono.just("7"));

    caseOutcomeService.createCostAward(caseReferenceNumber, providerId, request, loginId);

    verify(caabApiClient).createCostAward(caseOutcomeId, loginId, request);
  }

  @Test
  void createCostAward_whenNoCaseOutcome_createsBootstrapCaseOutcomeFirst() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final Integer caseOutcomeId = 42;
    final CostAwardDetail request = new CostAwardDetail();
    final String loginId = "user1";
    final CaseOutcomeDetail createdCaseOutcome = new CaseOutcomeDetail().id(caseOutcomeId);

    doReturn(Optional.empty(), Optional.of(createdCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);
    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just(String.valueOf(caseOutcomeId)));
    when(caabApiClient.createCostAward(caseOutcomeId, loginId, request)).thenReturn(Mono.just("7"));

    caseOutcomeService.createCostAward(caseReferenceNumber, providerId, request, loginId);

    final ArgumentCaptor<CaseOutcomeDetail> caseOutcomeCaptor =
        ArgumentCaptor.forClass(CaseOutcomeDetail.class);
    verify(caabApiClient).createCaseOutcome(eq(loginId), caseOutcomeCaptor.capture());
    verify(caabApiClient).createCostAward(caseOutcomeId, loginId, request);
    assertNull(caseOutcomeCaptor.getValue().getId());
    assertEquals(caseReferenceNumber, caseOutcomeCaptor.getValue().getCaseReferenceNumber());
    assertEquals(String.valueOf(providerId), caseOutcomeCaptor.getValue().getProviderId());
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
    request.setCourtAssessmentStatus("ASSESSED");
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
  void updateProceedingOutcome_existingOutcome_deletesOldThenCreatesNewRecord() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final Integer existingCaseOutcomeId = 42;

    final ProceedingOutcomeDetail existingProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("old");
    final ProceedingOutcomeDetail untouchedProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc2").resultInfo("keep");
    final ProceedingOutcomeDetail replacementProceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("new");

    final CaseOutcomeDetail existingCaseOutcome = new CaseOutcomeDetail();
    existingCaseOutcome.setId(existingCaseOutcomeId);
    existingCaseOutcome.setProceedingOutcomes(
        new ArrayList<>(List.of(existingProceedingOutcome, untouchedProceedingOutcome)));

    doReturn(Optional.of(existingCaseOutcome))
        .when(caseOutcomeService)
        .getCaseOutcome(caseReferenceNumber, providerId);

    when(caabApiClient.createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class)))
        .thenReturn(Mono.just("new-id"));
    when(caabApiClient.deleteCaseOutcome(existingCaseOutcomeId, loginId)).thenReturn(Mono.empty());

    caseOutcomeService.updateProceedingOutcome(
        caseReferenceNumber, providerId, replacementProceedingOutcome, loginId);

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

    final InOrder inOrder = inOrder(caabApiClient);
    inOrder.verify(caabApiClient).deleteCaseOutcome(existingCaseOutcomeId, loginId);
    inOrder.verify(caabApiClient).createCaseOutcome(eq(loginId), any(CaseOutcomeDetail.class));
  }

  @Test
  void getOrCreateCaseOutcome_whenNoPersistedOutcome_bootstrapsFromSuppliedOutcome() {
    final String caseReferenceNumber = "300000001";
    final Integer providerId = 123;
    final String loginId = "user1";
    final CostAwardDetail costAward = new CostAwardDetail().id(7);
    final ProceedingOutcomeDetail proceedingOutcome =
        new ProceedingOutcomeDetail().proceedingCaseId("pc1").resultInfo("saved");
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
    assertEquals(List.of(costAward), caseOutcomeCaptor.getValue().getCostAwards());
    assertEquals(List.of(proceedingOutcome), caseOutcomeCaptor.getValue().getProceedingOutcomes());
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
                caseReferenceNumber, providerId, replacementProceedingOutcome, loginId));

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
        caseReferenceNumber, providerId, proceedingOutcome, loginId);

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
