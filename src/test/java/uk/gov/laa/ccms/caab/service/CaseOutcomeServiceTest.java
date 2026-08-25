package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;

@ExtendWith(MockitoExtension.class)
class CaseOutcomeServiceTest {

  @Mock private CaabApiClient caabApiClient;

  @Spy @InjectMocks private CaseOutcomeService caseOutcomeService;

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
}
