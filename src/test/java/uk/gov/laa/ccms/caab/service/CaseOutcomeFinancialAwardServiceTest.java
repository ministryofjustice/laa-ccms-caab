package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetails;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;

@ExtendWith(MockitoExtension.class)
class CaseOutcomeFinancialAwardServiceTest {

  @Mock private CaabApiClient caabApiClient;

  @InjectMocks private CaseOutcomeService service;

  @Test
  void createsFinancialAwardAgainstExistingCaseOutcome() {
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final CaseOutcomeDetail caseOutcome = new CaseOutcomeDetail().id(42);
    when(caabApiClient.getCaseOutcomes("300000001", 123))
        .thenReturn(Mono.just(new CaseOutcomeDetails().content(List.of(caseOutcome))));
    when(caabApiClient.createFinancialAward(42, "user1", request)).thenReturn(Mono.just("7"));

    service.createFinancialAward("300000001", 123, request, "user1");

    verify(caabApiClient).createFinancialAward(42, "user1", request);
  }

  @Test
  void doesNotCreateFinancialAwardWhenCaseOutcomeDoesNotExist() {
    final FinancialAwardRequest request = new FinancialAwardRequest();
    when(caabApiClient.getCaseOutcomes("300000001", 123))
        .thenReturn(Mono.just(new CaseOutcomeDetails().content(List.of())));

    assertThrows(
        IllegalStateException.class,
        () -> service.createFinancialAward("300000001", 123, request, "user1"));

    verify(caabApiClient, never()).createFinancialAward(42, "user1", request);
  }

  @Test
  void updatesFinancialAwardAgainstExistingCaseOutcome() {
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(42)
            .financialAwards(List.of(new FinancialAwardDetail().id(7).updateAllowed(true)));
    when(caabApiClient.getCaseOutcomes("300000001", 123))
        .thenReturn(Mono.just(new CaseOutcomeDetails().content(List.of(caseOutcome))));
    when(caabApiClient.updateFinancialAward(42, 7, "user1", request)).thenReturn(Mono.empty());

    service.updateFinancialAward("300000001", 123, 7, request, "user1");

    verify(caabApiClient).updateFinancialAward(42, 7, "user1", request);
  }

  @Test
  void doesNotUpdateFinancialAwardWhenUpdateIsNotAllowed() {
    final FinancialAwardRequest request = new FinancialAwardRequest();
    final CaseOutcomeDetail caseOutcome =
        new CaseOutcomeDetail()
            .id(42)
            .financialAwards(List.of(new FinancialAwardDetail().id(7).updateAllowed(false)));
    when(caabApiClient.getCaseOutcomes("300000001", 123))
        .thenReturn(Mono.just(new CaseOutcomeDetails().content(List.of(caseOutcome))));

    assertThrows(
        IllegalStateException.class,
        () -> service.updateFinancialAward("300000001", 123, 7, request, "user1"));

    verify(caabApiClient, never()).updateFinancialAward(42, 7, "user1", request);
  }

  @Test
  void getsFinancialAwardDirectly() {
    final FinancialAwardDetail award = new FinancialAwardDetail().id(7);
    final CaseOutcomeDetail caseOutcome = new CaseOutcomeDetail().id(42);
    when(caabApiClient.getCaseOutcomes("300000001", 123))
        .thenReturn(Mono.just(new CaseOutcomeDetails().content(List.of(caseOutcome))));
    when(caabApiClient.getFinancialAward(42, 7)).thenReturn(Mono.just(award));

    assertSame(award, service.getFinancialAward("300000001", 123, 7).orElseThrow());
    verify(caabApiClient).getFinancialAward(42, 7);
  }
}
