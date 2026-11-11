package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapperImpl;
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
  @Spy private OpponentMapper opponentMapper = new OpponentMapperImpl();
  @Spy @InjectMocks private CaseOutcomeService service;

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void loadsBothEditFormsFromTheSelectedAward(String path) {
    final var opponent = editableIndividual();
    stubEditableIndividual(path, opponent);

    final var flow = service.getIndividualLiableParty("300000001", 123, path, 7, 11);

    assertThat(flow.getOpponentId()).isEqualTo(11);
    assertThat(flow.isEditing()).isTrue();
    assertThat(flow.getContactDetails().getTitle()).isEqualTo("MS");
    assertThat(flow.getContactDetails().getFirstName()).isEqualTo("Existing");
    assertThat(flow.getContactDetails().getDateOfBirth()).isEqualTo("01/02/2000");
    assertThat(flow.getContactDetails().getAddressLine1()).isEqualTo("High Street");
    assertThat(flow.getOtherDetails().getEmployerName()).isEqualTo("Existing employer");
    assertThat(flow.getOtherDetails().getAssessedIncome()).isEqualTo("123.45");
    assertThat(flow.getOtherDetails().getAssessmentDate()).isEqualTo("01/02/2020");
    assertThat(flow.getContactDetails().getLegalAided()).isNull();
    assertThat(flow.getOtherDetails().getCourtOrderedMeansAssessment()).isNull();
    assertThat(flow.isContactDetailsComplete()).isFalse();
  }

  @Test
  void updatingContactsRetainsThePersistedOtherDetails() {
    final var existing = editableIndividual();
    stubEditableIndividual("land", existing);
    final var form = new IndividualOpponentFormData();
    form.setTitle("MR");
    form.setFirstName("Changed");
    form.setSurname("Name");
    form.setOtherInformation("Must not overwrite persisted other details");
    when(caabApiClient.updateAwardLiableParty(
            org.mockito.ArgumentMatchers.eq(42), org.mockito.ArgumentMatchers.eq("LAND"),
            org.mockito.ArgumentMatchers.eq(7), org.mockito.ArgumentMatchers.eq(11),
            org.mockito.ArgumentMatchers.eq("user1"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.empty());

    service.updateIndividualLiablePartyContactDetails(
        "300000001", 123, "land", 7, 11, "user1", form);

    final var captor = org.mockito.ArgumentCaptor.forClass(OpponentDetail.class);
    verify(caabApiClient)
        .updateAwardLiableParty(
            org.mockito.ArgumentMatchers.eq(42), org.mockito.ArgumentMatchers.eq("LAND"),
            org.mockito.ArgumentMatchers.eq(7), org.mockito.ArgumentMatchers.eq(11),
            org.mockito.ArgumentMatchers.eq("user1"), captor.capture());
    assertThat(captor.getValue().getFirstName()).isEqualTo("Changed");
    assertThat(captor.getValue().getEmployerName()).isEqualTo("Existing employer");
    assertThat(captor.getValue().getAssessedIncome()).isEqualByComparingTo("123.45");
    assertThat(captor.getValue().getOtherInformation()).isEqualTo("Existing information");
    assertThat(existing.getFirstName()).isEqualTo("Existing");
  }

  @Test
  void updatingOtherDetailsRetainsLatestPersistedContactsAndAllowsClearing() {
    final var existing = editableIndividual();
    stubEditableIndividual("land", existing);
    final var form = new IndividualOpponentOtherDetailsFormData();
    form.setOtherInformation("Changed information");
    when(caabApiClient.updateAwardLiableParty(42, "LAND", 7, 11, "user1", existing))
        .thenReturn(Mono.empty());

    service.updateIndividualLiablePartyOtherDetails("300000001", 123, "land", 7, 11, "user1", form);

    verify(caabApiClient).updateAwardLiableParty(42, "LAND", 7, 11, "user1", existing);
    assertThat(existing.getFirstName()).isEqualTo("Existing");
    assertThat(existing.getEmployerName()).isNull();
    assertThat(existing.getAssessedIncome()).isNull();
    assertThat(existing.getAssessmentDate()).isNull();
    assertThat(existing.getOtherInformation()).isEqualTo("Changed information");
  }

  @ParameterizedTest
  @ValueSource(strings = {"imported", "shared", "organisation", "unrelated"})
  void editLoadRejectsReadOnlyWrongTypeOrUnrelatedOpponents(String kind) {
    final var existing = editableIndividual();
    switch (kind) {
      case "imported" -> existing.setEbsId("12345");
      case "shared" -> existing.setSharedInd(true);
      case "organisation" -> existing.setType("Organisation");
      case "unrelated" -> existing.setId(99);
      default -> throw new IllegalArgumentException(kind);
    }
    stubEditableIndividual("land", existing);
    assertThatThrownBy(() -> service.getIndividualLiableParty("300000001", 123, "land", 7, 11))
        .isInstanceOf(CaabApplicationException.class);
  }

  private OpponentDetail editableIndividual() {
    return new OpponentDetail()
        .id(11)
        .type("Individual")
        .ebsId("AWARDLP1")
        .title("MS")
        .firstName("Existing")
        .surname("Name")
        .dateOfBirth(uk.gov.laa.ccms.caab.util.DateUtils.convertToDate("01/02/2000"))
        .address(new uk.gov.laa.ccms.caab.model.AddressDetail().addressLine1("High Street"))
        .employerName("Existing employer")
        .assessedIncome(new BigDecimal("123.45"))
        .assessmentDate(uk.gov.laa.ccms.caab.util.DateUtils.convertToDate("01/02/2020"))
        .otherInformation("Existing information");
  }

  private void stubEditableIndividual(String path, OpponentDetail opponent) {
    final var outcome = outcome();
    stubOutcome(outcome);
    when(caabApiClient.getAwardLiableParties(42, award(outcome, path).getAwardType(), 7))
        .thenReturn(Mono.just(List.of(opponent)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void mapsBothPagesAndUsesAtomicAwardEndpoint(String path) {
    final var outcome = outcome();
    stubOutcome(outcome);
    final var contact = new IndividualOpponentFormData();
    contact.setTitle("MS");
    contact.setFirstName("Jane");
    contact.setSurname("Smith");
    contact.setDateOfBirth("01/02/2000");
    contact.setRelationshipToCase("OPPONENT");
    contact.setRelationshipToClient("OTHER");
    contact.setAddressLine1("High Street");
    contact.setCountry("GBR");
    contact.setPostcode("SW1A 1AA");
    contact.setLegalAided(true);
    contact.setCertificateNumber("CERT/123");
    final var other = new IndividualOpponentOtherDetailsFormData();
    other.setEmploymentStatus("EMPLOYED");
    other.setEmployerName("Acme");
    other.setEmployerAddress("Employer address");
    other.setCourtOrderedMeansAssessment(false);
    other.setAssessedIncome("123.45");
    other.setAssessedIncomeFrequency("MONTHLY");
    other.setAssessedAssets("2000");
    other.setAssessmentDate("01/02/2030");
    other.setOtherInformation("Other information");
    final var captor = org.mockito.ArgumentCaptor.forClass(OpponentDetail.class);
    when(caabApiClient.createAwardLiableParty(
            org.mockito.ArgumentMatchers.eq(42),
            org.mockito.ArgumentMatchers.eq(award(outcome, path).getAwardType()),
            org.mockito.ArgumentMatchers.eq(7),
            org.mockito.ArgumentMatchers.eq("user1"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.empty());

    service.createIndividualLiableParty("300000001", 123, path, 7, "user1", contact, other);

    verify(caabApiClient)
        .createAwardLiableParty(
            org.mockito.ArgumentMatchers.eq(42),
            org.mockito.ArgumentMatchers.eq(award(outcome, path).getAwardType()),
            org.mockito.ArgumentMatchers.eq(7),
            org.mockito.ArgumentMatchers.eq("user1"),
            captor.capture());
    final var payload = captor.getValue();
    assertThat(payload.getFirstName()).isEqualTo("Jane");
    assertThat(payload.getSurname()).isEqualTo("Smith");
    assertThat(payload.getType()).isEqualTo("Individual");
    assertThat(payload.getAddress().getAddressLine1()).isEqualTo("High Street");
    assertThat(payload.getLegalAided()).isTrue();
    assertThat(payload.getCertificateNumber()).isEqualTo("CERT/123");
    assertThat(payload.getEmploymentStatus()).isEqualTo("EMPLOYED");
    assertThat(payload.getEmployerName()).isEqualTo("Acme");
    assertThat(payload.getEmployerAddress()).isEqualTo("Employer address");
    assertThat(payload.getCourtOrderedMeansAssessment()).isFalse();
    assertThat(payload.getAssessedIncome()).isEqualByComparingTo("123.45");
    assertThat(payload.getAssessedIncomeFrequency()).isEqualTo("MONTHLY");
    assertThat(payload.getAssessedAssets()).isEqualByComparingTo("2000");
    assertThat(new java.text.SimpleDateFormat("dd/MM/yyyy").format(payload.getAssessmentDate()))
        .isEqualTo("01/02/2030");
    assertThat(payload.getOtherInformation()).isEqualTo("Other information");
    verifyNoInteractions(lookupService, liablePartiesDisplayMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void savingToAnUnknownAwardDoesNotCallAnyWriteApi(String path) {
    stubOutcome(outcome());
    assertThatThrownBy(
            () ->
                service.createIndividualLiableParty(
                    "300000001",
                    123,
                    path,
                    999,
                    "user1",
                    new IndividualOpponentFormData(),
                    new IndividualOpponentOtherDetailsFormData()))
        .isInstanceOf(CaabApplicationException.class);
    verifyNoInteractions(caabApiClient, opponentMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void validatesPartyAwardWithoutLoadingPartiesOrLookups(String path) {
    stubOutcome(outcome());

    service.validatePartyAward("300000001", 123, path, 7);

    verifyNoInteractions(caabApiClient, lookupService, liablePartiesDisplayMapper, opponentMapper);
  }

  @Test
  void missingPartyAwardFailsBeforeAnyFurtherApiCalls() {
    stubOutcome(outcome());

    assertThatThrownBy(() -> service.validatePartyAward("300000001", 123, "land", 999))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("award with id: 999");
    verifyNoInteractions(caabApiClient, lookupService, liablePartiesDisplayMapper, opponentMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"COST", "financial", "other-asset", "invalid"})
  void rejectsUnsupportedPartyAwardPaths(String path) {
    stubOutcome(outcome());

    assertThatThrownBy(() -> service.validatePartyAward("300000001", 123, path, 7))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessage("Unsupported award path: " + path);
    verifyNoInteractions(caabApiClient, lookupService, liablePartiesDisplayMapper, opponentMapper);
  }

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

  @Nested
  class GetAwardLiablePartiesTests {

    @Test
    void returnsTheRawOpponentsLinkedToTheAward() {
      final CaseOutcomeDetail outcome = linkedOutcome();
      stubOutcome(outcome);
      final List<OpponentDetail> opponents = List.of(new OpponentDetail().id(11).ebsId("EBS-1"));
      when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.just(opponents));

      assertThat(service.getAwardLiableParties("300000001", 123, "land", 7)).isEqualTo(opponents);
    }

    @Test
    void returnsAnEmptyListWhenNoPartiesAreLinkedWithoutCallingTheClient() {
      stubOutcome(outcome());

      assertThat(service.getAwardLiableParties("300000001", 123, "land", 7)).isEmpty();

      verifyNoInteractions(caabApiClient);
    }

    @Test
    void returnsAnEmptyListWhenTheClientRespondsWithNoBody() {
      stubOutcome(linkedOutcome());
      when(caabApiClient.getAwardLiableParties(42, "LAND", 7)).thenReturn(Mono.empty());

      assertThat(service.getAwardLiableParties("300000001", 123, "land", 7)).isEmpty();
    }
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
      service.addLiableParties(
          "300000001", 123, "land", 7, List.of(), List.of(), "login", id -> {}, opponent -> {});
      verifyNoInteractions(caabApiClient);

      service.addLiableParties(
          "300000001", 123, "land", 7, null, List.of(), "login", id -> {}, opponent -> {});
      verifyNoInteractions(caabApiClient);

      service.addLiableParties(
          "300000001", 123, "land", 7, null, null, "login", id -> {}, opponent -> {});
      verifyNoInteractions(caabApiClient);
    }

    @Test
    void createsAndLinksEachSuppliedNewOpponentViaTheCreateEndpoint() {
      stubOutcome(outcome());
      final OpponentDetail newOpponent = new OpponentDetail().ebsId("61700985").type("INDIVIDUAL");
      when(caabApiClient.createAwardLiableParty(42, "LAND", 7, "login", newOpponent))
          .thenReturn(Mono.empty());

      service.addLiableParties(
          "300000001",
          123,
          "land",
          7,
          List.of(),
          List.of(newOpponent),
          "login",
          id -> {},
          opponent -> {});

      verify(caabApiClient).createAwardLiableParty(42, "LAND", 7, "login", newOpponent);
      verifyNoMoreInteractions(caabApiClient);
    }

    @Test
    void linksExistingOpponentsAndCreatesNewOpponentsInTheSameCall() {
      stubOutcome(outcome());
      final OpponentDetail newOpponent = new OpponentDetail().ebsId("61700985").type("INDIVIDUAL");
      when(caabApiClient.addAwardLiableParty(42, "LAND", 7, 11, "login")).thenReturn(Mono.empty());
      when(caabApiClient.createAwardLiableParty(42, "LAND", 7, "login", newOpponent))
          .thenReturn(Mono.empty());

      final List<Integer> linked = new java.util.ArrayList<>();
      final List<OpponentDetail> created = new java.util.ArrayList<>();

      service.addLiableParties(
          "300000001",
          123,
          "land",
          7,
          List.of(11),
          List.of(newOpponent),
          "login",
          linked::add,
          created::add);

      assertThat(linked).containsExactly(11);
      assertThat(created).containsExactly(newOpponent);
      verify(caabApiClient).addAwardLiableParty(42, "LAND", 7, 11, "login");
      verify(caabApiClient).createAwardLiableParty(42, "LAND", 7, "login", newOpponent);
      verifyNoMoreInteractions(caabApiClient);
    }

    @Test
    void newOpponentCreationFailurePropagates() {
      stubOutcome(outcome());
      final OpponentDetail newOpponent = new OpponentDetail().ebsId("61700985").type("INDIVIDUAL");
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.createAwardLiableParty(42, "LAND", 7, "login", newOpponent))
          .thenReturn(Mono.error(failure));

      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001",
                      123,
                      "land",
                      7,
                      List.of(),
                      List.of(newOpponent),
                      "login",
                      id -> {},
                      opponent -> {}))
          .isSameAs(failure);
    }

    @Test
    void missingCaseOutcomeSurfacesAnError() {
      doReturn(Optional.empty()).when(service).getCaseOutcome("300000001", 123);
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001",
                      123,
                      "land",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
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
                      "300000001",
                      123,
                      "unsupported",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
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
      service.addLiableParties(
          "300000001",
          123,
          "cost-award",
          7,
          List.of(11, 11, 22),
          List.of(),
          "login",
          id -> {},
          opponent -> {});

      verify(caabApiClient).addAwardLiableParty(42, "COST", 7, 11, "login");
      verify(caabApiClient).addAwardLiableParty(42, "COST", 7, 22, "login");
      verifyNoMoreInteractions(caabApiClient);
    }

    @Test
    void costAwardLinkFailurePropagates() {
      stubOutcome(outcome());
      final CaabApiClientException failure = new CaabApiClientException("502 Bad Gateway");
      when(caabApiClient.addAwardLiableParty(42, "COST", 7, 11, "login"))
          .thenReturn(Mono.error(failure));
      assertThatThrownBy(
              () ->
                  service.addLiableParties(
                      "300000001",
                      123,
                      "cost-award",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
          .isSameAs(failure);
    }

    @Test
    void financialAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("DAMAGE"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties(
          "300000001",
          123,
          "financial-award",
          7,
          List.of(11, 22),
          List.of(),
          "login",
          id -> {},
          opponent -> {});

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
                      "300000001",
                      123,
                      "financial-award",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
          .isSameAs(failure);
    }

    @Test
    void landAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("LAND"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties(
          "300000001",
          123,
          "land",
          7,
          List.of(11, 22),
          List.of(),
          "login",
          id -> {},
          opponent -> {});

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
              () ->
                  service.addLiableParties(
                      "300000001",
                      123,
                      "land",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
          .isSameAs(failure);
    }

    @Test
    void otherAssetAwardLinksEachDistinctOpponentViaTheLiablePartiesEndpoint() {
      stubOutcome(outcome());
      when(caabApiClient.addAwardLiableParty(eq(42), eq("ASSET"), eq(7), any(), eq("login")))
          .thenReturn(Mono.empty());

      service.addLiableParties(
          "300000001",
          123,
          "asset",
          7,
          List.of(11, 22),
          List.of(),
          "login",
          id -> {},
          opponent -> {});

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
              () ->
                  service.addLiableParties(
                      "300000001",
                      123,
                      "asset",
                      7,
                      List.of(11),
                      List.of(),
                      "login",
                      id -> {},
                      opponent -> {}))
          .isSameAs(failure);
    }
  }
}
