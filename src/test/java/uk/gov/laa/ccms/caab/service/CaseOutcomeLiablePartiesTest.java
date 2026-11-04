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
    void costAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("COST"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      // Opponent 11 is repeated in the request, alongside a new opponent 22, to prove
      // de-duplication of the ids sent to the API.
      service.addLiableParties("300000001", 123, "cost-award", 7, List.of(11, 11, 22), "login");

      verify(caabApiClient).addAwardLiableParty(42, "COST", 7, 11, "login");
      verify(caabApiClient).addAwardLiableParty(42, "COST", 7, 22, "login");
      org.mockito.Mockito.verifyNoMoreInteractions(caabApiClient);
    }

    @Test
    void costAwardLinkFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.addAwardLiableParty(42, "COST", 7, 11, "login"))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties("300000001", 123, "cost-award", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void financialAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("DAMAGE"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "financial-award", 7, List.of(11, 22), "login");

      verify(caabApiClient).addAwardLiableParty(42, "DAMAGE", 7, 11, "login");
      verify(caabApiClient).addAwardLiableParty(42, "DAMAGE", 7, 22, "login");
    }

    @Test
    void financialAwardLinkFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.addAwardLiableParty(42, "DAMAGE", 7, 11, "login"))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001", 123, "financial-award", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void landAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("LAND"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "land", 7, List.of(11, 22), "login");

      verify(caabApiClient).addAwardLiableParty(42, "LAND", 7, 11, "login");
      verify(caabApiClient).addAwardLiableParty(42, "LAND", 7, 22, "login");
    }

    @Test
    void landAwardLinkFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.addAwardLiableParty(42, "LAND", 7, 11, "login"))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "land", 7, List.of(11), "login"))
          .isSameAs(failure);
    }

    @Test
    void otherAssetAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("ASSET"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties("300000001", 123, "asset", 7, List.of(11, 22), "login");

      verify(caabApiClient).addAwardLiableParty(42, "ASSET", 7, 11, "login");
      verify(caabApiClient).addAwardLiableParty(42, "ASSET", 7, 22, "login");
    }

    @Test
    void otherAssetAwardLinkFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.addAwardLiableParty(42, "ASSET", 7, 11, "login"))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () -> service.addLiableParties("300000001", 123, "asset", 7, List.of(11), "login"))
          .isSameAs(failure);
    }
  }
}
