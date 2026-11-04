package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.LiablePartiesMappingContext;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LiablePartyDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;

@ExtendWith(MockitoExtension.class)
class CaseOutcomeLiablePartiesTest {
  @Mock private CaabApiClient caabApiClient;
  @Mock private LookupService lookupService;
  @Mock private LiablePartiesDisplayMapper liablePartiesDisplayMapper;
  @Spy @InjectMocks private CaseOutcomeService service;

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void selectsAwardAndLoadsOnlyItsParties(String path) {
    final CaseOutcomeDetail outcome = outcome();
    final BaseAwardDetail award = award(outcome, path);
    award.addLiablePartiesItem(new LiablePartyDetail().id(101).opponentId(11));
    final List<OpponentDetail> opponents = List.of(new OpponentDetail().id(11));
    stubOutcome(outcome);
    final var context = LiablePartiesMappingContext.builder().build();
    final var display = new LiablePartiesDisplay("LAND", "Property", null, false, false, List.of());
    when(lookupService.getLiablePartiesMappingContext()).thenReturn(Mono.just(context));
    when(caabApiClient.getAwardLiableParties(42, award.getAwardType(), 7))
        .thenReturn(Mono.just(opponents));
    when(liablePartiesDisplayMapper.toLiablePartiesDisplay(award, opponents, context))
        .thenReturn(display);

    assertThat(service.getLiablePartiesDisplay("300000001", 123, path, 7)).isSameAs(display);

    verify(liablePartiesDisplayMapper).toLiablePartiesDisplay(award, opponents, context);
    verify(caabApiClient).getAwardLiableParties(42, award.getAwardType(), 7);
  }

  @Test
  void emptyPartyListDoesNotFetchLookups() {
    final CaseOutcomeDetail outcome = outcome();
    stubOutcome(outcome);

    service.getLiablePartiesDisplay("300000001", 123, "land", 7);

    verify(liablePartiesDisplayMapper)
        .toLiablePartiesDisplay(outcome.getLandAwards().getFirst(), List.of(), null);
    verifyNoInteractions(lookupService, caabApiClient);
  }

  @Test
  void rejectsUnknownAwardBeforeLoadingLookups() {
    stubOutcome(outcome());
    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 99))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("award with id: 99");
    verifyNoInteractions(lookupService, liablePartiesDisplayMapper, caabApiClient);
  }

  @Test
  void missingOutcomeSurfacesAnError() {
    doReturn(Optional.empty()).when(service).getCaseOutcome("300000001", 123);
    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("case outcome");
    verifyNoInteractions(lookupService, liablePartiesDisplayMapper, caabApiClient);
  }

  @Test
  void nullAwardCollectionSurfacesMissingAward() {
    stubOutcome(new CaseOutcomeDetail().id(42).landAwards(null));
    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("award with id: 7");
  }

  @Test
  void missingLookupContextSurfacesAnError() {
    final CaseOutcomeDetail outcome = outcome();
    outcome
        .getLandAwards()
        .getFirst()
        .addLiablePartiesItem(new LiablePartyDetail().id(101).opponentId(11));
    stubOutcome(outcome);
    when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.just(List.of()));
    when(lookupService.getLiablePartiesMappingContext()).thenReturn(Mono.empty());
    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("award parties or lookup data");
    verifyNoInteractions(liablePartiesDisplayMapper);
  }

  @Test
  void missingPartyResponseSurfacesAnError() {
    final CaseOutcomeDetail outcome = linkedOutcome();
    stubOutcome(outcome);
    when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.empty());
    when(lookupService.getLiablePartiesMappingContext())
        .thenReturn(Mono.just(LiablePartiesMappingContext.builder().build()));

    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("award parties or lookup data");
    verifyNoInteractions(liablePartiesDisplayMapper);
  }

  @Test
  void partyRetrievalFailureFromTheClientIsPropagatedNotSwallowed() {
    // The CaabApiClient already converts a 404 response into an empty list, so any
    // CaabApiClientException that reaches the service represents a genuine failure (for
    // example a 502 or a connection error) and must not be masked as an empty party list.
    final CaseOutcomeDetail outcome = linkedOutcome();
    stubOutcome(outcome);
    final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
    when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.error(failure));
    when(lookupService.getLiablePartiesMappingContext())
        .thenReturn(Mono.just(LiablePartiesMappingContext.builder().build()));

    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isSameAs(failure);
    verifyNoInteractions(liablePartiesDisplayMapper);
  }

  @Test
  void partyRetrievalFailureIsPropagated() {
    stubOutcome(linkedOutcome());
    final IllegalStateException failure = new IllegalStateException("Party API unavailable");
    when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.error(failure));
    when(lookupService.getLiablePartiesMappingContext())
        .thenReturn(Mono.just(LiablePartiesMappingContext.builder().build()));

    assertThatThrownBy(() -> service.getLiablePartiesDisplay("300000001", 123, "land", 7))
        .isSameAs(failure);
    verifyNoInteractions(liablePartiesDisplayMapper);
  }

  private CaseOutcomeDetail linkedOutcome() {
    final CaseOutcomeDetail outcome = outcome();
    outcome
        .getLandAwards()
        .getFirst()
        .addLiablePartiesItem(new LiablePartyDetail().id(101).opponentId(11));
    return outcome;
  }

  private void stubOutcome(CaseOutcomeDetail outcome) {
    doReturn(Optional.of(outcome)).when(service).getCaseOutcome("300000001", 123);
  }

  private BaseAwardDetail award(CaseOutcomeDetail outcome, String path) {
    return switch (path) {
      case "cost-award" -> outcome.getCostAwards().getFirst();
      case "financial-award" -> outcome.getFinancialAwards().getFirst();
      case "land" -> outcome.getLandAwards().getFirst();
      default -> outcome.getOtherAssetAwards().getFirst();
    };
  }

  private CaseOutcomeDetail outcome() {
    return new CaseOutcomeDetail()
        .id(42)
        .costAwards(List.of(new CostAwardDetail().id(7).awardType("COST")))
        .financialAwards(List.of(new FinancialAwardDetail().id(7).awardType("DAMAGE")))
        .landAwards(List.of(new LandAwardDetail().id(7).awardType("LAND")))
        .otherAssetAwards(List.of(new OtherAssetAwardDetail().id(7).awardType("ASSET")));
  }

  @Nested
  class AddLiablePartiesTests {

    @Test
    void doesNothingWhenNoOpponentIdsSupplied() {
      service.addLiableParties("300000001", 123, "land", 7, List.of(), "login");
      verifyNoInteractions(caabApiClient);

      service.addLiableParties("300000001", 123, "land", 7, null, "login");
      verifyNoInteractions(caabApiClient);
    }

    @Test
    void missingCaseOutcomeSurfacesAnError() {
      doReturn(Optional.empty()).when(service).getCaseOutcome("300000001", 123);
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "land", 7, List.of(11), "login"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No case outcome exists");
      verifyNoInteractions(caabApiClient);
    }

    @Test
    void rejectsUnsupportedAwardPath() {
      stubOutcome(outcome());
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001", 123, "unsupported", 7, List.of(11), "login"))
          .isInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Unsupported award path");
      verifyNoInteractions(caabApiClient);
    }

    @Test
    void costAwardMergesDedupesAndPersistsNewOpponentsPreservingExistingLinks() {
      stubOutcome(outcome());
      final CostAwardDetail award =
          new CostAwardDetail()
              .id(7)
              .awardType("COST")
              .liableParties(
                  List.of(new LiablePartyDetail().id(101).awardType("COST").opponentId(11)));
      when(caabApiClient.getCostAward(42, 7)).thenReturn(Mono.just(award));
      when(caabApiClient.updateCostAward(eq(42), eq(7), eq("login"), any(CostAwardDetail.class)))
          .thenReturn(Mono.empty());

      // Opponent 11 is already linked (should be left untouched) and 11 is repeated in the
      // request, alongside a new opponent 22, to prove both existing-link preservation and
      // de-duplication of the newly added ids.
      service.addLiableParties("300000001", 123, "cost-award", 7, List.of(11, 11, 22), "login");

      final ArgumentCaptor<CostAwardDetail> captor = ArgumentCaptor.forClass(CostAwardDetail.class);
      verify(caabApiClient).updateCostAward(eq(42), eq(7), eq("login"), captor.capture());
      final List<LiablePartyDetail> savedLinks = captor.getValue().getLiableParties();
      assertThat(savedLinks).hasSize(2);
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(11))
          .singleElement()
          .satisfies(
              link -> {
                // The existing link's id and metadata must be preserved, not replaced.
                assertThat(link.getId()).isEqualTo(101);
                assertThat(link.getAwardType()).isEqualTo("COST");
              });
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(22))
          .singleElement()
          .satisfies(
              link -> {
                assertThat(link.getId()).isNull();
                assertThat(link.getAwardType()).isEqualTo("COST");
              });
    }

    @Test
    void costAwardRetrievalFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.getCostAward(42, 7)).thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties("300000001", 123, "cost-award", 7, List.of(11), "login"))
          .isSameAs(failure);
      verify(caabApiClient, org.mockito.Mockito.never())
          .updateCostAward(any(), any(), any(), any(CostAwardDetail.class));
    }

    @Test
    void costAwardUpdateFailurePropagates() {
      stubOutcome(outcome());
      when(caabApiClient.getCostAward(42, 7))
          .thenReturn(Mono.just(new CostAwardDetail().id(7).awardType("COST")));
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.updateCostAward(eq(42), eq(7), eq("login"), any(CostAwardDetail.class)))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties("300000001", 123, "cost-award", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void financialAwardMergesDedupesAndPersistsNewOpponentsPreservingExistingLinks() {
      stubOutcome(outcome());
      final FinancialAwardDetail award =
          new FinancialAwardDetail()
              .id(7)
              .awardType("DAMAGE")
              .liableParties(
                  List.of(new LiablePartyDetail().id(201).awardType("DAMAGE").opponentId(11)));
      when(caabApiClient.getFinancialAward(42, 7)).thenReturn(Mono.just(award));
      when(caabApiClient.updateFinancialAward(
              eq(42), eq(7), eq("login"), any(FinancialAwardDetail.class)))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "financial-award", 7, List.of(11, 22), "login");

      final ArgumentCaptor<FinancialAwardDetail> captor =
          ArgumentCaptor.forClass(FinancialAwardDetail.class);
      verify(caabApiClient).updateFinancialAward(eq(42), eq(7), eq("login"), captor.capture());
      final List<LiablePartyDetail> savedLinks = captor.getValue().getLiableParties();
      assertThat(savedLinks).hasSize(2);
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(11))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isEqualTo(201));
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(22))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isNull());
    }

    @Test
    void financialAwardRetrievalFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.getFinancialAward(42, 7)).thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001", 123, "financial-award", 7, List.of(11), "login"))
          .isSameAs(failure);
      verify(caabApiClient, org.mockito.Mockito.never())
          .updateFinancialAward(any(), any(), any(), any(FinancialAwardDetail.class));
    }

    @Test
    void financialAwardUpdateFailurePropagates() {
      stubOutcome(outcome());
      when(caabApiClient.getFinancialAward(42, 7))
          .thenReturn(Mono.just(new FinancialAwardDetail().id(7).awardType("DAMAGE")));
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.updateFinancialAward(
              eq(42), eq(7), eq("login"), any(FinancialAwardDetail.class)))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001", 123, "financial-award", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void landAwardMergesDedupesAndPersistsNewOpponentsPreservingExistingLinks() {
      stubOutcome(outcome());
      final LandAwardDetail award =
          new LandAwardDetail()
              .id(7)
              .awardType("LAND")
              .liableParties(
                  List.of(new LiablePartyDetail().id(301).awardType("LAND").opponentId(11)));
      when(caabApiClient.getLandAward(42, 7)).thenReturn(Mono.just(award));
      when(caabApiClient.updateLandAward(eq(42), eq(7), eq("login"), any(LandAwardDetail.class)))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "land", 7, List.of(11, 22), "login");

      final ArgumentCaptor<LandAwardDetail> captor = ArgumentCaptor.forClass(LandAwardDetail.class);
      verify(caabApiClient).updateLandAward(eq(42), eq(7), eq("login"), captor.capture());
      final List<LiablePartyDetail> savedLinks = captor.getValue().getLiableParties();
      assertThat(savedLinks).hasSize(2);
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(11))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isEqualTo(301));
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(22))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isNull());
    }

    @Test
    void landAwardRetrievalFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.getLandAward(42, 7)).thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "land", 7, List.of(11), "login"))
          .isSameAs(failure);
      verify(caabApiClient, org.mockito.Mockito.never())
          .updateLandAward(any(), any(), any(), any(LandAwardDetail.class));
    }

    @Test
    void landAwardUpdateFailurePropagates() {
      stubOutcome(outcome());
      when(caabApiClient.getLandAward(42, 7))
          .thenReturn(Mono.just(new LandAwardDetail().id(7).awardType("LAND")));
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.updateLandAward(eq(42), eq(7), eq("login"), any(LandAwardDetail.class)))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "land", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void otherAssetAwardMergesDedupesAndPersistsNewOpponentsPreservingExistingLinks() {
      stubOutcome(outcome());
      final OtherAssetAwardDetail award =
          new OtherAssetAwardDetail()
              .id(7)
              .awardType("ASSET")
              .liableParties(
                  List.of(new LiablePartyDetail().id(401).awardType("ASSET").opponentId(11)));
      when(caabApiClient.getOtherAssetAward(42, 7)).thenReturn(Mono.just(award));
      when(caabApiClient.updateOtherAssetAward(
              eq(42), eq(7), eq("login"), any(OtherAssetAwardDetail.class)))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "asset", 7, List.of(11, 22), "login");

      final ArgumentCaptor<OtherAssetAwardDetail> captor =
          ArgumentCaptor.forClass(OtherAssetAwardDetail.class);
      verify(caabApiClient).updateOtherAssetAward(eq(42), eq(7), eq("login"), captor.capture());
      final List<LiablePartyDetail> savedLinks = captor.getValue().getLiableParties();
      assertThat(savedLinks).hasSize(2);
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(11))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isEqualTo(401));
      assertThat(savedLinks)
          .filteredOn(link -> link.getOpponentId().equals(22))
          .singleElement()
          .satisfies(link -> assertThat(link.getId()).isNull());
    }

    @Test
    void otherAssetAwardRetrievalFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.getOtherAssetAward(42, 7)).thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "asset", 7, List.of(11), "login"))
          .isSameAs(failure);
      verify(caabApiClient, org.mockito.Mockito.never())
          .updateOtherAssetAward(any(), any(), any(), any(OtherAssetAwardDetail.class));
    }

    @Test
    void otherAssetAwardUpdateFailurePropagates() {
      stubOutcome(outcome());
      when(caabApiClient.getOtherAssetAward(42, 7))
          .thenReturn(Mono.just(new OtherAssetAwardDetail().id(7).awardType("ASSET")));
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.updateOtherAssetAward(
              eq(42), eq(7), eq("login"), any(OtherAssetAwardDetail.class)))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "asset", 7, List.of(11), "login"))
          .isSameAs(failure);
    }
  }
}
